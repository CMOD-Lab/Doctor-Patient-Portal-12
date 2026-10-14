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

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * DoctorEditProfileServlet – handles doctor profile edit requests from the doctor panel.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * Flash messages and doctor profile objects written via
 * {@code session.setAttribute()} are stored in the shared Redis store and are
 * available to any application instance that handles the subsequent redirect,
 * enabling stateless horizontal scaling without server affinity (sticky sessions).
 * </p>
 */
@WebServlet("/doctorEditProfile")
public class DoctorEditProfileServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// get all data which is coming from doctor.jsp doctor details
			String fullName = req.getParameter("fullName");
			String dateOfBirth = req.getParameter("dateOfBirth");
			String qualification = req.getParameter("qualification");
			String specialist = req.getParameter("specialist");
			String email = req.getParameter("email");
			String phone = req.getParameter("phone");
			//String password = req.getParameter("password");

			
			int id = Integer.parseInt(req.getParameter("doctorId"));

			Doctor doctor = new Doctor(id, fullName, dateOfBirth, qualification, specialist, email, phone, "");

			DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

			boolean f = docDAO.editDoctorProfile(doctor);

			// req.getSession() returns a Redis-backed HttpSession via Spring Session
			// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
			// so that session data is stored in Amazon ElastiCache for Redis rather
			// than in the local JVM heap, supporting stateless horizontal scaling.
			HttpSession session = req.getSession();

			if (f == true) {
				Doctor updateDoctorObj = docDAO.getDoctorById(id);
				// Flash message stored in distributed Redis session via Spring Session.
				session.setAttribute("successMsgForD", "Doctor update Successfully");
				// Updated doctor object stored in distributed Redis session via Spring Session.
				session.setAttribute("doctorObj", updateDoctorObj); // over ride or update old session value to new updated doctor value.
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {
				// Flash message stored in distributed Redis session via Spring Session.
				session.setAttribute("errorMsgForD", "Something went wrong on server!");
				resp.sendRedirect("doctor/edit_profile.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
