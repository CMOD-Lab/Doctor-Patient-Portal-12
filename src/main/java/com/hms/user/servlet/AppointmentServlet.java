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

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Appointment;

/**
 * AppointmentServlet – records a new patient appointment.
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
@WebServlet("/addAppointment")
public class AppointmentServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId          = Integer.parseInt(req.getParameter("userId"));
		String fullName        = req.getParameter("fullName");
		String gender          = req.getParameter("gender");
		String age             = req.getParameter("age");
		String appointmentDate = req.getParameter("appointmentDate");
		String email           = req.getParameter("email");
		String phone           = req.getParameter("phone");
		String diseases        = req.getParameter("diseases");
		int    doctorId        = Integer.parseInt(req.getParameter("doctorNameSelect"));
		String address         = req.getParameter("address");

		Appointment appointment = new Appointment(userId, fullName, gender, age, appointmentDate, email, phone, diseases, doctorId, address, "Pending");

		AppointmentDAO appointmentDAO = new AppointmentDAO(DBConnection.getConn());
		boolean f = appointmentDAO.addAppointment(appointment);

		// req.getSession() returns a Redis-backed HttpSession via Spring Session
		// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
		// so that session data is stored in Amazon ElastiCache for Redis rather
		// than in the local JVM heap, supporting stateless horizontal scaling.
		HttpSession session = req.getSession();

		if (f == true) {
			// successMsg stored in distributed Redis session via Spring Session.
			session.setAttribute("successMsg", "Appointment is recorded Successfully.");
			resp.sendRedirect("user_appointment.jsp");

		} else {
			// errorMsg stored in distributed Redis session via Spring Session.
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("user_appointment.jsp");
		}
	}

}
