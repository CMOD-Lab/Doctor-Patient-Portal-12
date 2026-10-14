package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// Spring Session transparently replaces the container-managed HttpSession with
// a Redis-backed session stored in Amazon ElastiCache for Redis (cr-java-0065).
// The SpringSessionRepositoryFilter registered by RedisSessionConfig intercepts
// every request and wraps HttpSession so that all setAttribute / getAttribute
// calls are routed to the shared Redis store, enabling stateless horizontal
// scaling without sticky sessions.
import javax.servlet.http.HttpSession;

/**
 * DoctorLogoutServlet – invalidates the doctor's session on logout.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * Calling {@code session.removeAttribute()} and {@code session.setAttribute()}
 * on logout operates against the shared Redis store, ensuring that the
 * session is properly cleaned up across all application instances and that
 * the logout flash message is available regardless of which instance handles
 * the subsequent redirect.  This enables stateless horizontal scaling without
 * server affinity (sticky sessions).
 * </p>
 */
@WebServlet("/doctorLogout")
public class DoctorLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// req.getSession() returns a Redis-backed HttpSession via Spring Session
		// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
		// so that session data is stored in Amazon ElastiCache for Redis rather
		// than in the local JVM heap, supporting stateless horizontal scaling.
		HttpSession session = req.getSession();
		// doctorObj removed from distributed Redis session via Spring Session.
		session.removeAttribute("doctorObj");
		// successMsg stored in distributed Redis session via Spring Session.
		session.setAttribute("successMsg", "Doctor Logout Successfully.");
		resp.sendRedirect("doctor_login.jsp");
	}

}
