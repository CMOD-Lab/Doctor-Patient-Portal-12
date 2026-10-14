package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis Session Configuration for Amazon ElastiCache.
 *
 * Enables distributed HTTP session management via Spring Session backed by
 * Amazon ElastiCache for Redis (cr-java-0065).  All HttpSession.setAttribute /
 * getAttribute calls in the application are transparently stored in Redis,
 * eliminating server-affinity and enabling horizontal scaling.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint (default: localhost)
 *   REDIS_PORT  – ElastiCache port             (default: 6379)
 *
 * The SpringSessionRepositoryFilter registered by @EnableRedisHttpSession
 * intercepts every request and wraps the HttpSession with a Redis-backed
 * implementation, so no changes to existing servlet session API calls are
 * needed.
 */
@Configuration
@EnableRedisHttpSession
public class RedisSessionConfig {

    /**
     * Lettuce connection factory pointing at Amazon ElastiCache.
     * Connection parameters are read from environment variables so that no
     * credentials or host names are hard-coded in source (12-factor principle).
     */
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST")
                : "localhost";
        int redisPort = System.getenv("REDIS_PORT") != null
                ? Integer.parseInt(System.getenv("REDIS_PORT"))
                : 6379;

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

        // If ElastiCache AUTH token is configured, apply it
        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }
}
