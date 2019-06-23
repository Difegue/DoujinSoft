package com.difegue.doujinsoft;

import com.difegue.doujinsoft.utils.MioCompress;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DownloadServlet
 */
@WebServlet("/download")
public class DownloadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DownloadServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Returns .mio files from the dataDirectory so they can be downloaded.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// obtains ServletContext
		ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");
		
		
		if (request.getParameterMap().containsKey("type") && request.getParameterMap().containsKey("id")) 
		{
			
			String id = request.getParameter("id");
			String type = request.getParameter("type");
			
			String filePath = "";
			
			switch (type) {
				case "game":
					filePath = dataDir + "/mio/game/" + id + ".miozip";
					break;
				case "record":
					filePath = dataDir + "/mio/record/" + id + ".miozip";
					break;
				case "manga":
					filePath = dataDir + "/mio/manga/" + id + ".miozip";
					break;
				default:
					filePath = null;
					break;
			}
			
			if (filePath != null) {

				File downloadFile = MioCompress.uncompressMio(new File(filePath));
				FileInputStream inStream = new FileInputStream(downloadFile);
				
				// gets MIME type of the file
				String mimeType = "application/octet-stream";
				// Set response
				response.setContentType(mimeType);
				response.setCharacterEncoding("UTF-8");
				response.setContentLength((int) downloadFile.length());
				
				// forces download
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", URLEncoder.encode(downloadFile.getName(), "UTF-8"));
				response.setHeader(headerKey, headerValue);
				
				// obtains response's output stream
				OutputStream outStream = response.getOutputStream();
				
				byte[] buffer = new byte[4096];
				int bytesRead = -1;
				
				while ((bytesRead = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				
				inStream.close();
				outStream.close();
				
			}
			
		}	
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
