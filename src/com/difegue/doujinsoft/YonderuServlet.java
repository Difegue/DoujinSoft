package com.difegue.doujinsoft;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.difegue.doujinsoft.utils.DatabaseUtils;
import com.difegue.doujinsoft.utils.MioCompress;
import com.difegue.doujinsoft.utils.MioUtils;

import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.editors.MangaEdit;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Servlet implementation class for daily comics and non-WC24 surveys.
 */
@WebServlet("/yonderu")
public class YonderuServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public YonderuServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Gets Yonderu-compatible information for either a daily comic, a random comic, or a specific MIO hash.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		ServletContext application = getServletConfig().getServletContext();
		String dataDir = application.getInitParameter("dataDirectory");

		Gson gson = new Gson();
		JsonObject json = new JsonObject();
		String id = "";

		response.setContentType("application/json; charset=UTF-8");

		if (request.getParameterMap().containsKey("id")) {
			id = request.getParameter("id");
		}

		try {
			String filePath = dataDir + "/mio/manga/" + id + ".miozip";
			File f = new File(filePath);

			File mioFile = MioCompress.uncompressMio(new File(filePath));
			byte[] mioData = FileByteOperations.read(mioFile.getAbsolutePath());

			// TODO: Use DB instead
			/*
			 * try (Connection connection = DriverManager
                        .getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
			 */
			MangaEdit mio = new MangaEdit(mioData);
			List<String> pages = new ArrayList<String>();
			for (int i = 0; i < 4; i++) {
				pages.add(MioUtils.getRLEManga(mioData, i));
			}

			int timestamp = mio.getTimestamp();
			// If the timestamp is larger than today's date, set it to today's date
			if (MioUtils.DIY_TIMESTAMP_ORIGIN.plusDays(timestamp).toLocalDate().isAfter(LocalDate.now()))
				timestamp = (int) MioUtils.DIY_TIMESTAMP_ORIGIN.until(ZonedDateTime.now(), ChronoUnit.DAYS);


			json.addProperty("id", id);
			json.addProperty("name", mio.getName());
			json.addProperty("date", MioUtils.getTimeString(timestamp));
			json.addProperty("creator", mio.getCreator());
			json.addProperty("brand", mio.getBrand());
			json.addProperty("description", mio.getDescription());
			json.addProperty("logo", mio.getLogo());
			json.addProperty("colorLogo", MioUtils.mapColorByte(mio.getLogoColor()));
			json.addProperty("color", MioUtils.mapColorByte(mio.getMangaColor()));

			json.add("pages", gson.toJsonTree(pages).getAsJsonArray());
		}
		catch (IOException e) {
			e.printStackTrace();
			json.addProperty("error", "Failed to read the MIO file.");
		}

		response.getWriter().append(json.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}

}
