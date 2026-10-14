package com.hms.admin.servlet;

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

/**
 * DeleteDoctorServlet – handles deletion of a doctor record by the admin.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * Flash messages written via {@code session.setAttribute()} are stored in the
 * shared Redis store and are available to any application instance that handles
 * the subsequent redirect, enabling stateless horizontal scaling without
 * server affinity (sticky sessions).
 * </p>
 */
@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get id(which is coming as string value) and convert into int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

		// req.getSession() returns a Redis-backed HttpSession via Spring Session
		// (cr-java-0065).  The SpringSessionRepositoryFilter wraps the request
		// so that session data is stored in Amazon ElastiCache for Redis rather
		// than in the local JVM heap, supporting stateless horizontal scaling.
		HttpSession session = req.getSession();
		
		boolean f = docDAO.deleteDoctorById(id);
		
		if(f==true) {
			// Flash message stored in distributed Redis session via Spring Session.
			session.setAttribute("successMsg", "Doctor Deleted Successfully.");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
		else {
			// Flash message stored in distributed Redis session via Spring Session.
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
	
	

}
