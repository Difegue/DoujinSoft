package com.difegue.doujinsoft;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Each item's pubDate is when it was added to DoujinSoft; the in-game creation date is in its description.
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
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final ZonedDateTime ORIGIN = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		boolean hideNsfw = "false".equals(request.getParameter("nsfw"));
		String typeParam = request.getParameter("type");

		// table, endpoint, label, storage directory
		String[][] types = { { "Games", "games", "Game", "game" }, { "Manga", "comics", "Comic", "manga" },
				{ "Records", "records", "Record", "record" } };
		List<String[]> selected = new ArrayList<>();
		for (String[] t : types)
			if (typeParam == null || typeParam.isEmpty() || ("," + typeParam + ",").contains("," + t[1] + ","))
				selected.add(t);

		if (selected.isEmpty()) {
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

		// The database doesn't store when a mio was added, but the server writes its .miozip at that moment.
		// Newest rows by insertion order are fetched per table, then merged by added date.
		List<Item> items = new ArrayList<>();
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
			for (String[] t : selected) {
				String query = "SELECT hash, name, creator, description, timeStamp FROM " + t[0]
						+ " WHERE id NOT LIKE '%them%'" + (hideNsfw && t[0].equals("Games") ? " AND isNsfw = 0" : "")
						+ " ORDER BY rowid DESC LIMIT ?";
				try (PreparedStatement statement = connection.prepareStatement(query)) {
					statement.setInt(1, limit);
					ResultSet rs = statement.executeQuery();
					while (rs.next()) {
						Item i = new Item();
						i.path = t[1];
						i.label = t[2];
						i.hash = rs.getString("hash");
						i.name = rs.getString("name");
						i.creator = rs.getString("creator");
						i.description = rs.getString("description");
						i.created = ORIGIN.plusDays(rs.getInt("timeStamp"));
						long modified = new File(dataDir + "/mio/" + t[3] + "/" + i.hash + ".miozip").lastModified();
						// lastModified is 0 if the file is missing: fall back to the in-game date
						i.added = modified > 0 ? Instant.ofEpochMilli(modified).atZone(ZoneOffset.UTC) : i.created;
						items.add(i);
					}
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage());
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		items.sort(Comparator.comparing((Item i) -> i.added).reversed());

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"><channel>")
				.append("<title>DoujinSoft - Latest additions</title>")
				.append("<link>").append(esc(base)).append("/</link>")
				.append("<description>Latest Mio content added to DoujinSoft</description>")
				.append("<atom:link href=\"").append(esc(self + (request.getQueryString() == null ? "" : "?" + request.getQueryString())))
				.append("\" rel=\"self\" type=\"application/rss+xml\"/>");

		for (Item i : items.subList(0, Math.min(limit, items.size()))) {
			String name = i.name == null || i.name.trim().isEmpty() ? "No Title" : i.name;
			String link = base + "/" + i.path + "?id=" + i.hash;

			// pubDate is when the item was added to DoujinSoft; the in-game date is in the description
			xml.append("<item><title>").append(esc(name)).append("</title>")
					.append("<link>").append(esc(link)).append("</link>")
					.append("<guid isPermaLink=\"true\">").append(esc(link)).append("</guid>")
					.append("<category>").append(i.label).append("</category>")
					.append("<pubDate>").append(i.added.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>")
					.append("<description>").append(esc(i.label + " by " + i.creator + "\n"
							+ "Added to DoujinSoft: " + i.added.format(DAY) + "\n"
							+ "Created in-game: " + i.created.format(DAY) + "\n\n" + i.description))
					.append("</description></item>");
		}

		xml.append("</channel></rss>");
		response.setContentType("application/rss+xml; charset=UTF-8");
		response.getWriter().append(xml);
	}

	private static class Item {
		String path, label, hash, name, creator, description;
		ZonedDateTime created, added;
	}

	private static String esc(String s) {
		// Mio text can contain control characters, which are illegal in XML
		return s == null ? "" : s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
