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
import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * DoctorLoginServlet – authenticates a doctor and establishes a login session.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * The authenticated {@code Doctor} object and any flash messages written via
 * {@code session.setAttribute()} are stored in the shared Redis store and are
 * available to any application instance that handles the subsequent redirect,
 * enabling stateless horizontal scaling without server affinity (sticky sessions).
 * </p>
 */
@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		// req.getSession() returns a Redis-backed HttpSession via Spring Session
		// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
		// so that session data is stored in Amazon ElastiCache for Redis rather
		// than in the local JVM heap, supporting stateless horizontal scaling.
		HttpSession session = req.getSession();

		//create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		//call loginDoctor() method for doctor login which method declared in DoctorDAO 
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			//means doctor is valid or exist
			//then store particular logged in doctor object in session
			// doctorObj stored in distributed Redis session via Spring Session.
			session.setAttribute("doctorObj", doctor);
			//and redirect the particular doctor index page which is reside doctor folder
			resp.sendRedirect("doctor/index.jsp");//doctor index means dashboard of doctors
		} else {
			// errorMsg stored in distributed Redis session via Spring Session.
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
