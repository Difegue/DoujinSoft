package com.difegue.doujinsoft;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.difegue.doujinsoft.utils.MioUtils.Types;

/**
 * Servlet implementation class MusicListing
 */
@WebServlet("/records")
public class MusicServlet extends ContentServletBase {

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.doGet(request, response, Types.RECORD);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		super.doPost(request, response, Types.RECORD);
	}

    public MusicServlet() {
        super("MusicServlet"); 
    }
   
}
