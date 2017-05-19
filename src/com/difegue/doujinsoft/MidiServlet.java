package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.difegue.doujinsoft.ExportMidi;
import com.xperia64.diyedit.FileByteOperations;

/**
 * Servlet implementation class DownloadServlet
 */
@WebServlet("/midi")
public class MidiServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MidiServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servlet for extracting midi files from .mios. Extracted midis are stored in the dataDirectory so they can be re-used later on.
	 * (And also because the DIYEdit API doesn't let me just grab the midi data as-is)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// obtains ServletContext
		ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");
		
		
		if (request.getParameterMap().containsKey("id")) 
		{
			
			String id = request.getParameter("id");
			String filePath = dataDir+"/mio/record/"+id+".mio";
			
			if (!new File (dataDir+"/midi/").exists())
		    	  new File(dataDir+"/midi/").mkdirs();
			
			String midiPath = dataDir+"/midi/"+id+".midi";
			
			File recordFile = new File(filePath);
			File midiFile = new File(midiPath);
			
			//If mio file exists, spawn a ExportMidi object from it
			if (!midiFile.exists() && recordFile.exists()) {
				
				byte[] mioFile = FileByteOperations.read(recordFile.getAbsolutePath());
				
				ExportMidi mid = new ExportMidi(mioFile);
				
				mid.export(midiPath, false);
			}
			
			
			if (midiFile.exists()) {
				
				// gets MIME type of the file
				String mimeType = "audio/midi";
				FileInputStream inStream = new FileInputStream(midiFile);
				
				// Set response
				response.setContentType(mimeType);
				response.setContentLength((int) midiFile.length());
				
				// forces download
				//String headerKey = "Content-Disposition";
				//String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
				//response.setHeader(headerKey, headerValue);
				
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
