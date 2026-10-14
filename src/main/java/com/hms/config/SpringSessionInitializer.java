package com.hms.config;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Registers the Spring Session {@code springSessionRepositoryFilter} as the
 * first {@link javax.servlet.Filter} in the servlet container's filter chain.
 *
 * <p>This initializer is discovered automatically by the servlet container
 * (Servlet 3.0+) via the {@link javax.servlet.ServletContainerInitializer} SPI,
 * so no additional {@code web.xml} filter declaration is required when the
 * application is deployed to a Servlet 3.0-compliant container (e.g. Tomcat 7+).
 *
 * <p>The filter transparently wraps every incoming {@link javax.servlet.http.HttpServletRequest}
 * so that calls to {@code request.getSession()} return a Spring-managed
 * {@link org.springframework.session.Session} backed by Amazon ElastiCache for
 * Redis, as configured in {@link RedisSessionConfig}.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * This class is the entry point for Spring Session's distributed session
 * management.  By extending {@code AbstractHttpSessionApplicationInitializer}
 * and passing {@link RedisSessionConfig}, the
 * {@code springSessionRepositoryFilter} is automatically registered and
 * intercepts all HTTP requests, replacing the in-process JVM session store
 * with Amazon ElastiCache for Redis.  This eliminates server affinity and
 * enables stateless horizontal scaling across multiple application instances.
 * </p>
 */
public class SpringSessionInitializer extends AbstractHttpSessionApplicationInitializer {

    /**
     * Passes {@link RedisSessionConfig} to the parent constructor so that
     * Spring Session can locate the {@code LettuceConnectionFactory} bean and
     * the {@code @EnableRedisHttpSession} annotation.
     */
    public SpringSessionInitializer() {
        super(RedisSessionConfig.class);
    }
}
