package com.difegue.doujinsoft;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * RSS feed of the latest content added to the database.
 *
 * Parameters (all optional):
 *  - type: comma-separated list of games, comics, records. Defaults to all three.
 *  - limit: number of items, 1-100. Defaults to 20.
 *  - nsfw: set to "false" to hide NSFW games.
 */
@WebServlet("/feed")
public class FeedServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger("FeedServlet");
	private static final int DEFAULT_LIMIT = 20, MAX_LIMIT = 100;

	// Mio timestamps are a number of days since 2000-01-01
	private static final ZonedDateTime ORIGIN = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		boolean hideNsfw = "false".equals(request.getParameter("nsfw"));
		String typeParam = request.getParameter("type");
		List<String> selects = new ArrayList<>();

		// table, endpoint, label
		String[][] types = { { "Games", "games", "Game" }, { "Manga", "comics", "Comic" },
				{ "Records", "records", "Record" } };
		for (String[] t : types) {
			if (typeParam != null && !typeParam.isEmpty() && !("," + typeParam + ",").contains("," + t[1] + ","))
				continue;
			selects.add("SELECT '" + t[1] + "' AS path, '" + t[2] + "' AS label, hash, id, name, creator, description, "
					+ "timeStamp, rowid AS rid FROM " + t[0] + " WHERE id NOT LIKE '%them%'"
					+ (hideNsfw && t[0].equals("Games") ? " AND isNsfw = 0" : ""));
		}

		if (selects.isEmpty()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "type must be games, comics and/or records");
			return;
		}

		int limit = DEFAULT_LIMIT;
		try {
			limit = Math.max(1, Math.min(MAX_LIMIT, Integer.parseInt(request.getParameter("limit"))));
		} catch (NumberFormatException e) {
			// Missing or invalid, use the default
		}

		ServletContext application = getServletConfig().getServletContext();
		String dataDir = application.getInitParameter("dataDirectory");
		String self = request.getRequestURL().toString();
		String base = self.substring(0, self.length() - request.getRequestURI().length()) + request.getContextPath();

		String query = String.join(" UNION ALL ", selects) + " ORDER BY timeStamp DESC, rid DESC LIMIT ?";

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"><channel>")
				.append("<title>DoujinSoft - Latest additions</title>")
				.append("<link>").append(esc(base)).append("/</link>")
				.append("<description>Latest Mio content added to DoujinSoft</description>")
				.append("<atom:link href=\"").append(esc(self + (request.getQueryString() == null ? "" : "?" + request.getQueryString())))
				.append("\" rel=\"self\" type=\"application/rss+xml\"/>");

		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite");
				PreparedStatement statement = connection.prepareStatement(query)) {

			statement.setInt(1, limit);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				String name = rs.getString("name");
				if (name == null || name.trim().isEmpty())
					name = "No Title";
				String link = base + "/" + rs.getString("path") + "?id=" + rs.getString("hash");
				String pubDate = ORIGIN.plusDays(rs.getInt("timeStamp")).format(DateTimeFormatter.RFC_1123_DATE_TIME);

				xml.append("<item><title>").append(esc(name)).append("</title>")
						.append("<link>").append(esc(link)).append("</link>")
						.append("<guid isPermaLink=\"true\">").append(esc(link)).append("</guid>")
						.append("<category>").append(rs.getString("label")).append("</category>")
						.append("<pubDate>").append(pubDate).append("</pubDate>")
						.append("<description>").append(esc(rs.getString("label") + " by " + rs.getString("creator")
								+ ": " + rs.getString("description")))
						.append("</description></item>");
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage());
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		xml.append("</channel></rss>");
		response.setContentType("application/rss+xml; charset=UTF-8");
		response.getWriter().append(xml);
	}

	private static String esc(String s) {
		// Mio text can contain control characters, which are illegal in XML
		return s == null ? "" : s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
