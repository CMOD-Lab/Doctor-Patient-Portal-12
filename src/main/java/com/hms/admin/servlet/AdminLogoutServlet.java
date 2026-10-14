package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AdminLogoutServlet – handles admin logout.
 *
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session (cr-java-0065).  The SpringSessionRepositoryFilter (registered by
 * RedisSessionConfig) transparently replaces the container-managed HttpSession
 * with a Redis-backed session.  Calling session.invalidate() or
 * session.removeAttribute() removes the entry from the shared Redis store,
 * ensuring logout is effective across all application instances.
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// HttpSession is now backed by Amazon ElastiCache for Redis via
		// Spring Session (RedisSessionConfig / @EnableRedisHttpSession).
		// Removing attributes here removes them from the shared Redis store,
		// so logout is effective regardless of which instance handles the request.
		HttpSession session = req.getSession();
		// Remove admin session object from Redis-backed distributed session store.
		session.removeAttribute("adminObj");
		// Invalidate the entire Redis session entry to fully log out the admin.
		session.invalidate();

		// Create a new session to carry the success flash message.
		HttpSession newSession = req.getSession(true);
		//show message after logout
		newSession.setAttribute("successMsg", "Admin Logout Successfully");
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
