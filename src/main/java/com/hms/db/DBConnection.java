package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnection - HikariCP connection pool with AWS RDS Proxy support.
 *
 * Connection parameters are resolved from environment variables so that
 * the application can run in any AWS environment (ECS, Elastic Beanstalk,
 * Lambda, etc.) without code changes.  When an RDS Proxy endpoint is
 * configured the pool automatically benefits from connection multiplexing
 * and IAM-based authentication offered by the proxy.
 *
 * Environment variables consumed:
 *   DB_URL      - full JDBC URL, e.g.
 *                 jdbc:mysql://<rds-proxy-endpoint>:3306/hospital
 *   DB_USERNAME - database user name
 *   DB_PASSWORD - database password
 *
 * Defaults (for local / non-cloud development only):
 *   DB_URL      -> jdbc:mysql://localhost:3306/hospital
 *   DB_USERNAME -> root
 *   DB_PASSWORD -> (empty string)
 *
 * cr-java-0097 – Missing Connection Timeouts
 *   All timeout values are explicitly configured at both the HikariCP pool
 *   level and the MySQL JDBC driver level to prevent indefinite hangs and
 *   resource exhaustion in cloud environments with variable network latency.
 *
 *   HikariCP-level timeouts:
 *     connectionTimeout  – max time (ms) to wait for a connection from the pool (30 s)
 *     validationTimeout  – max time (ms) to validate a connection before use  (5 s)
 *     idleTimeout        – max time (ms) a connection may sit idle in the pool (10 min)
 *     keepaliveTime      – interval (ms) at which idle connections are tested  (5 min)
 *     maxLifetime        – max lifetime (ms) of any connection in the pool     (30 min)
 *
 *   JDBC / socket-level timeouts (embedded in the JDBC URL):
 *     connectTimeout     – TCP connect timeout in milliseconds (10 s)
 *     socketTimeout      – TCP socket read/write timeout in milliseconds (30 s)
 */
public class DBConnection {

    // ---------------------------------------------------------------------------
    // Timeout constants (milliseconds) – cr-java-0097
    // ---------------------------------------------------------------------------

    /** Maximum time to wait for a connection from the HikariCP pool (30 seconds). */
    private static final long POOL_CONNECTION_TIMEOUT_MS  = 30_000L;

    /** Maximum time to validate a borrowed connection before returning it to the caller (5 seconds). */
    private static final long POOL_VALIDATION_TIMEOUT_MS  =  5_000L;

    /** Maximum time a connection may remain idle in the pool before being evicted (10 minutes). */
    private static final long POOL_IDLE_TIMEOUT_MS        = 600_000L;

    /** Interval at which idle connections are sent a keep-alive ping (5 minutes). */
    private static final long POOL_KEEPALIVE_TIME_MS      = 300_000L;

    /** Maximum lifetime of any connection in the pool (30 minutes). */
    private static final long POOL_MAX_LIFETIME_MS        = 1_800_000L;

    /** TCP connect timeout passed to the MySQL JDBC driver (10 seconds, in milliseconds). */
    private static final int  JDBC_CONNECT_TIMEOUT_MS     = 10_000;

    /** TCP socket read/write timeout passed to the MySQL JDBC driver (30 seconds, in milliseconds). */
    private static final int  JDBC_SOCKET_TIMEOUT_MS      = 30_000;

    // ---------------------------------------------------------------------------
    // Pool configuration – initialised once at class-load time (thread-safe).
    // ---------------------------------------------------------------------------
    private static final HikariDataSource dataSource;

    static {
        String dbUrl      = getEnv("DB_URL",      "jdbc:mysql://localhost:3306/hospital");
        String dbUsername = getEnv("DB_USERNAME",  "root");
        String dbPassword = getEnv("DB_PASSWORD",  "");

        // Append JDBC-level socket timeouts to the URL so that the MySQL driver
        // enforces TCP-level timeouts independently of the HikariCP pool timeouts.
        // This prevents a single slow/hung network call from blocking a pool thread
        // indefinitely – a critical safeguard in cloud environments (cr-java-0097).
        String dbUrlWithTimeouts = appendJdbcTimeouts(dbUrl,
                JDBC_CONNECT_TIMEOUT_MS, JDBC_SOCKET_TIMEOUT_MS);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrlWithTimeouts);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // -----------------------------------------------------------------------
        // Pool sizing – sensible defaults that work well behind an RDS Proxy.
        // -----------------------------------------------------------------------
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        // -----------------------------------------------------------------------
        // Explicit timeout configuration – cr-java-0097
        // Prevents indefinite hangs and resource exhaustion in cloud environments.
        // -----------------------------------------------------------------------

        // Time to wait for a connection from the pool before throwing an exception.
        config.setConnectionTimeout(POOL_CONNECTION_TIMEOUT_MS);

        // Time allowed to validate a connection (via connectionTestQuery) before
        // it is considered invalid and discarded.
        config.setValidationTimeout(POOL_VALIDATION_TIMEOUT_MS);

        // Time a connection may sit idle before being removed from the pool.
        config.setIdleTimeout(POOL_IDLE_TIMEOUT_MS);

        // Interval at which idle connections are tested with a keep-alive query
        // to detect stale connections early (especially important with RDS Proxy).
        config.setKeepaliveTime(POOL_KEEPALIVE_TIME_MS);

        // Hard upper bound on connection lifetime; forces periodic recycling to
        // avoid issues with database-side connection limits and RDS Proxy pinning.
        config.setMaxLifetime(POOL_MAX_LIFETIME_MS);

        // Keep-alive query for MySQL / Aurora.
        config.setConnectionTestQuery("SELECT 1");

        // Pool name – useful in CloudWatch / application logs.
        config.setPoolName("HmsHikariPool");

        dataSource = new HikariDataSource(config);
    }

    // ---------------------------------------------------------------------------
    // Public API – callers must close() the returned Connection when done.
    // ---------------------------------------------------------------------------

    /**
     * Returns a Connection from the HikariCP pool.
     * The caller is responsible for closing the connection (try-with-resources
     * is strongly recommended) so that it is returned to the pool promptly.
     *
     * @return a live {@link Connection} from the pool, or {@code null} if the
     *         pool cannot provide one (error is printed to stderr).
     */
    public static Connection getConn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Appends MySQL JDBC socket-level timeout parameters to the given JDBC URL
     * if they are not already present.  This ensures that TCP connect and read
     * timeouts are enforced at the driver level, independently of HikariCP.
     *
     * @param jdbcUrl           the base JDBC URL (may already contain query params)
     * @param connectTimeoutMs  TCP connect timeout in milliseconds
     * @param socketTimeoutMs   TCP socket read/write timeout in milliseconds
     * @return the JDBC URL with timeout parameters appended
     */
    static String appendJdbcTimeouts(String jdbcUrl, int connectTimeoutMs, int socketTimeoutMs) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return jdbcUrl;
        }
        StringBuilder url = new StringBuilder(jdbcUrl);
        // Determine the correct separator: '?' if no query string yet, '&' otherwise.
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        if (!jdbcUrl.contains("connectTimeout=")) {
            url.append(separator).append("connectTimeout=").append(connectTimeoutMs);
            separator = "&";
        }
        if (!jdbcUrl.contains("socketTimeout=")) {
            url.append(separator).append("socketTimeout=").append(socketTimeoutMs);
        }
        return url.toString();
    }
}
