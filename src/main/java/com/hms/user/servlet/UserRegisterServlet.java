package com.hms.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;

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

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * UserRegisterServlet – handles new user registration requests.
 *
 * <p><b>Cloud Readiness – cr-java-0065 (HTTP Session State Storage)</b><br>
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The {@code SpringSessionRepositoryFilter} (registered by
 * {@link com.hms.config.RedisSessionConfig}) transparently replaces the
 * container-managed {@code HttpSession} with a Redis-backed session.
 * All {@code session.setAttribute()} calls below operate against the shared
 * Redis store, ensuring that flash messages (successMsg / errorMsg) are
 * propagated correctly across all application instances without server
 * affinity (sticky sessions).
 * </p>
 */
@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// PrintWriter out = resp.getWriter();

			// get all data/value which is coming from signup.jsp page for new User
			// registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB
			UserDAO userDAO = new UserDAO(DBConnection.getConn());

			// cr-java-0065: req.getSession() returns a Redis-backed HttpSession via
			// Spring Session (SpringSessionRepositoryFilter).  Session data is stored
			// in Amazon ElastiCache for Redis rather than in the local JVM heap,
			// supporting stateless horizontal scaling without sticky sessions.
			HttpSession session = req.getSession();

			// call userRegister() and pass user object to insert or save user into DB.
			boolean f = userDAO.userRegister(user); // userRegister() method return boolean type value

			if (f == true) {

				// cr-java-0065: successMsg stored in distributed Redis session via Spring Session.
				// The attribute is available to all application instances sharing the same
				// ElastiCache cluster, ensuring consistent flash-message delivery under
				// load-balanced, horizontally-scaled deployments.
				session.setAttribute("successMsg", "Register Successfully");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg
				//System.out.println("register successfull");
				// out.println("success");

			} else {

				// cr-java-0065: errorMsg stored in distributed Redis session via Spring Session.
				// The attribute is available to all application instances sharing the same
				// ElastiCache cluster, ensuring consistent flash-message delivery under
				// load-balanced, horizontally-scaled deployments.
				session.setAttribute("errorMsg", "Something went wrong!");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg

				//System.out.println("Error! Something went wrong");
				// out.println("error");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
