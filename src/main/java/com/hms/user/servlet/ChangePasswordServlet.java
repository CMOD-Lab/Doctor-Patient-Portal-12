package com.hms.user.servlet;

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

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;

/**
 * ChangePasswordServlet – handles user password change requests.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * Calling {@code session.setAttribute()} operates against the shared Redis
 * store, ensuring that flash messages (successMsg / errorMsg) are available
 * regardless of which application instance handles the subsequent redirect.
 * This enables stateless horizontal scaling without server affinity
 * (sticky sessions).
 * </p>
 */
@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId      = Integer.parseInt(req.getParameter("userId"));
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");

		UserDAO uDAO = new UserDAO(DBConnection.getConn());

		// req.getSession() returns a Redis-backed HttpSession via Spring Session
		// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
		// so that session data is stored in Amazon ElastiCache for Redis rather
		// than in the local JVM heap, supporting stateless horizontal scaling.
		HttpSession session = req.getSession();

		if (uDAO.checkOldPassword(userId, oldPassword)) {

			if (uDAO.changePassword(userId, newPassword)) {
				// successMsg stored in distributed Redis session via Spring Session.
				session.setAttribute("successMsg", "Password Change Successfully.");
				resp.sendRedirect("change_password.jsp");

			} else {
				// errorMsg stored in distributed Redis session via Spring Session.
				session.setAttribute("errorMsg", "Something wrong on server!");
				resp.sendRedirect("change_password.jsp");
			}

		} else {
			// errorMsg stored in distributed Redis session via Spring Session.
			session.setAttribute("errorMsg", "Old password incorrect");
			resp.sendRedirect("change_password.jsp");
		}
	}

}
