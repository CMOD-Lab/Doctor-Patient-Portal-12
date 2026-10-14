package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065: HTTP Session State Storage — Migrated to Amazon ElastiCache for Redis
// via Spring Session.  The SpringSessionRepositoryFilter (registered by
// com.hms.config.SpringSessionInitializer / RedisSessionConfig) intercepts every
// request and wraps HttpServletRequest so that calls to request.getSession()
// return a Spring-managed Session backed by Amazon ElastiCache for Redis.
// All HttpSession.setAttribute / removeAttribute / invalidate calls are
// transparently routed to the distributed Redis store, eliminating server
// affinity and enabling stateless horizontal scaling across multiple instances.
import javax.servlet.http.HttpSession;

/**
 * UserLogoutServlet – handles user logout requests.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * Calling {@code session.removeAttribute()} and {@code session.setAttribute()}
 * operate against the shared Redis store, ensuring that session cleanup and
 * flash messages are propagated across all application instances.
 * This enables stateless horizontal scaling without server affinity
 * (sticky sessions).
 * </p>
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// cr-java-0065: req.getSession() returns a Redis-backed HttpSession via
		// Spring Session (SpringSessionRepositoryFilter).  Session data is stored
		// in Amazon ElastiCache for Redis rather than in the local JVM heap,
		// supporting stateless horizontal scaling without sticky sessions.
		HttpSession session = req.getSession();

		// removeAttribute operates on the distributed Redis session via Spring Session.
		session.removeAttribute("userObj");
		// successMsg stored in distributed Redis session via Spring Session.
		session.setAttribute("successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");
	}

}
