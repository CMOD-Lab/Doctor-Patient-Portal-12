package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.entity.User;

/**
 * AdminLoginServlet – handles admin authentication.
 *
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session (cr-java-0065).  The SpringSessionRepositoryFilter (registered by
 * RedisSessionConfig) transparently replaces the container-managed HttpSession
 * with a Redis-backed session, so all HttpSession API calls below work
 * identically while enabling stateless, horizontally-scalable instances.
 *
 * No server affinity (sticky sessions) is required; any application instance
 * can serve any request because session data lives in the shared Redis store.
 */
@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
			//create one static Admin for this project
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// HttpSession is now backed by Amazon ElastiCache for Redis via
			// Spring Session (RedisSessionConfig / @EnableRedisHttpSession).
			// Session attributes are stored in Redis, not in JVM heap, enabling
			// horizontal scaling without sticky sessions.
			HttpSession session = req.getSession();
			
			//logic for a static Admin
			if ("admin@gmail.com".equals(email) && "admin".equals(password)) {
				
				//if "adminObj" obj available then give the access of admin page, 
				//otherwise "adminObj" is not present in obj then others user is login(which is not admin). so dont give him the access of Admin.
				//the below line specially check the admin is log in or not! "adminObj" object is available that means admin is log in.
				// Stored in Redis via Spring Session – survives instance restarts and load balancing.
				session.setAttribute("adminObj", new User());
				resp.sendRedirect("admin/index.jsp");
			}
			else {
				// Error message stored in Redis-backed session for cross-instance availability.
				session.setAttribute("errorMsg", "Invalid Username or Password.");
				resp.sendRedirect("admin_login.jsp");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
