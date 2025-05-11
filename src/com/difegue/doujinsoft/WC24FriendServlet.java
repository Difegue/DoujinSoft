package com.difegue.doujinsoft;

import com.difegue.doujinsoft.wc24.MailItem;
import com.difegue.doujinsoft.wc24.WiiConnect24Api;
import com.difegue.doujinsoft.utils.DatabaseUtils;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet implementation class for automated Friend Requests.
 */
@WebServlet("/friendreq")
public class WC24FriendServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public WC24FriendServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Sends a friend request to the specified friend code through WiiConnect24.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		// obtains ServletContext
		ServletContext application = getServletConfig().getServletContext();
		dataDir = application.getInitParameter("dataDirectory");

		response.setContentType("text/html; charset=UTF-8");

		if (request.getParameterMap().containsKey("code")) {

			String code = request.getParameter("code");

			if (DatabaseUtils.isFriendCodeSaved(dataDir, code)) {
				response.getOutputStream().print("This friend code is already registered.");
				return;
			}

			try {
				// Friend Request mail
				MailItem friendReq = new MailItem(code);
				WiiConnect24Api wc24 = new WiiConnect24Api(application);
				String wc24Response = wc24.sendMails(List.of(friendReq));

				response.getOutputStream().print(wc24Response);
			} catch (Exception e) {
				response.getOutputStream().print(e.getMessage());
			}
		} else
			response.getOutputStream().print("Please add a friend code.");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}

}
