package com.difegue.doujinsoft;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;



/**
 * Servlet implementation class GameListing
 */
@WebServlet("/games")
public class GameListing extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private String dataDir;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GameListing() {
        super();
        
    }
   
  
    

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();
		dataDir = application.getInitParameter("dataDirectory");
		
		String mioTestName = "G-Doofy(Marisa)-0021 (UE) (2010-04-13) Cirno's Math.mio";
		File f = new File(dataDir+"\\mio\\game", mioTestName);
		
		byte[] mioFile = FileByteOperations.read(f.getAbsolutePath());
		
		//Metadata mioData = new Metadata(mioFile);
		GameEdit gameMeta = new GameEdit(mioFile);
		response.getWriter().append("<html><head></head><body>");

		response.getWriter().append("mio File Test info:<br/>");
		response.getWriter().append(gameMeta.getName()+"<br/>"+gameMeta.getDescription()+
									"<br/>"+gameMeta.getCreator()+" - "+gameMeta.getBrand());
		
		String mioPreview = MioUtils.getBase64GamePreview(mioFile);
	    response.getWriter().append("<br/><img src='"+mioPreview+"'/><br/>");
	    
	    
	    mioTestName = "M-A47-0004 (J) (2009-06-22) 鱆鰛鱆鰛稷秧餡.mio";
		f = new File(dataDir+"\\mio\\manga", mioTestName);
		
		byte[] mangaFile = FileByteOperations.read(f.getAbsolutePath());
		MangaEdit mangaMeta = new MangaEdit(mangaFile);
		response.getWriter().append(mangaMeta.getName()+"<br/>"+mangaMeta.getDescription()+
				"<br/>"+mangaMeta.getCreator()+" - "+mangaMeta.getBrand());
		
		
		mioPreview = MioUtils.getBase64Manga(mangaFile,0);
	    response.getWriter().append("<br/><img src='"+mioPreview+"'/>");
	    mioPreview = MioUtils.getBase64Manga(mangaFile,1);
	    response.getWriter().append("<br/><img src='"+mioPreview+"'/>");
	    mioPreview = MioUtils.getBase64Manga(mangaFile,2);
	    response.getWriter().append("<br/><img src='"+mioPreview+"'/>");
	    mioPreview = MioUtils.getBase64Manga(mangaFile,3);
	    response.getWriter().append("<br/><img src='"+mioPreview+"'/>");
	    
	    response.getWriter().append("</body>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
