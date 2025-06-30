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
import java.nio.file.Files;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.sql.Connection;
import java.sql.DriverManager;

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
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 *      Gets Yonderu-compatible information for either a daily comic, a random
	 *      comic, or a specific MIO hash.
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

		if (request.getParameterMap().containsKey("random")) {
			// Get a random comic ID from the database
			try (Connection connection = DriverManager
					.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
				var statement = connection.createStatement();
				var result = statement.executeQuery("SELECT hash FROM Manga ORDER BY RANDOM() LIMIT 1");
				id = result.getString(1);
			} catch (Exception e) {
				e.printStackTrace();
				json.addProperty("error", "Failed to connect to the database.");
				response.getWriter().append(json.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}

		if (request.getParameterMap().containsKey("daily")) {
			// Look in the daily comic file for today's ID
			File dailyComicFile = new File(dataDir + "/yonderu.txt");
			if (!dailyComicFile.exists()) {
				json.addProperty("error", "No daily comic available.");
				response.getWriter().append(json.toString());
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				return;
			}

			try {
				List<String> lines = Files.readAllLines(dailyComicFile.toPath());
				LocalDate today = LocalDate.now();
				int dayOfYear = today.getDayOfYear();
				if (dayOfYear < 1 || dayOfYear > lines.size()) {
					json.addProperty("error", "No daily comic available for today.");
					response.getWriter().append(json.toString());
					response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
					return;
				}
				id = lines.get(dayOfYear - 1).trim();
			} catch (IOException e) {
				e.printStackTrace();
				json.addProperty("error", "Failed to read the daily comic file.");
				response.getWriter().append(json.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}

		}

		File jsonFile = new File(dataDir + "/yonderu/" + id + ".json");
		if (jsonFile.exists()) {
			// If the JSON file for this ID already exists, read it and return it
			String jsonContent = Files.readString(jsonFile.toPath());
			response.getWriter().append(jsonContent);
			return;
		}

		try {
			// It would be better to use the DB instead of file ops, but the DB doesn't have
			// the raw byte data for images.. This is cached so the FS hit will be minimal.
			String filePath = dataDir + "/mio/manga/" + id + ".miozip";

			File mioFile = MioCompress.uncompressMio(new File(filePath));
			byte[] mioData = FileByteOperations.read(mioFile.getAbsolutePath());

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
			json.addProperty("colorLogo", mio.getLogoColor());
			json.addProperty("color", mio.getMangaColor());

			json.add("pages", gson.toJsonTree(pages).getAsJsonArray());

			// Save a copy of the generated JSON so it can be reused
			jsonFile.getParentFile().mkdirs();
			Files.writeString(jsonFile.toPath(), json.toString());

		} catch (IOException e) {
			e.printStackTrace();
			json.addProperty("error", "Failed to read the MIO file.");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		response.getWriter().append(json.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 *      Process a survey answer for a comic.
	 *      POST /yonderu?id=xxxxx&stars=(1-5)&comment=(1-8)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		ServletContext application = getServletConfig().getServletContext();
		String dataDir = application.getInitParameter("dataDirectory");

		JsonObject json = new JsonObject();

		response.setContentType("application/json; charset=UTF-8");

		try (Connection connection = DriverManager
				.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

			String id = request.getParameter("id");
			int note = Integer.parseInt(request.getParameter("stars"));
			int comment = Integer.parseInt(request.getParameter("comment"));

			if (id == null || note < 1 || note > 5 || comment < 1 || comment > 8) {
				json.addProperty("error", "Missing or invalid parameters.");
				response.getWriter().append(json.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			// Check if the MIO hash exists in the database
			var statement = connection.createStatement();
			var result = statement.executeQuery("SELECT * FROM Manga WHERE hash = '" + id + "'");
			if (result.next()) {

				var success = DatabaseUtils.saveSurveyAnswer(dataDir, request.getRemoteAddr(), 2,
						result.getString("name"), note, comment, id);

				if (success) {
					json.addProperty("status", "success");
					response.getWriter().append(json.toString());
				} else {
					json.addProperty("error", "Failed to save the survey answer.");
					response.getWriter().append(json.toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				}

			} else {
				json.addProperty("error", "Comic not found in the database.");
				response.getWriter().append(json.toString());
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
			json.addProperty("error", "An error occurred while processing the request.");
			response.getWriter().append(json.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

}
