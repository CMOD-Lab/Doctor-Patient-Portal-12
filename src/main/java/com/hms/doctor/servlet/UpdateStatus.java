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

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;

/**
 * UpdateStatus – updates the doctor's appointment comment/status.
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
@WebServlet("/updateStatus")
public class UpdateStatus extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			int    id       = Integer.parseInt(req.getParameter("id"));
			int    doctorId = Integer.parseInt(req.getParameter("doctorId"));
			String comment  = req.getParameter("comment");

			AppointmentDAO appDAO = new AppointmentDAO(DBConnection.getConn());
			boolean f = appDAO.updateDrAppointmentCommentStatus(id, doctorId, comment);

			// req.getSession() returns a Redis-backed HttpSession via Spring Session
			// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
			// so that session data is stored in Amazon ElastiCache for Redis rather
			// than in the local JVM heap, supporting stateless horizontal scaling.
			HttpSession session = req.getSession();

			if (f == true) {
				// successMsg stored in distributed Redis session via Spring Session.
				session.setAttribute("successMsg", "Comment updated");
				resp.sendRedirect("doctor/patient.jsp");

			} else {
				// errorMsg stored in distributed Redis session via Spring Session.
				session.setAttribute("errorMsg", "Something went wrong on server!");
				resp.sendRedirect("doctor/patient.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
