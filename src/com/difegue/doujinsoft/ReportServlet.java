package com.difegue.doujinsoft;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Lets visitors report a game as NSFW. Reports show up in the admin panel.
 */
@WebServlet("/report")
public class ReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Max reports per client IP per window. Uses getRemoteAddr: behind a reverse proxy, enable Tomcat's RemoteIpValve so this sees real client IPs.
    private static final int MAX_REPORTS = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000;
    // ip -> {window start, count}
    private static final ConcurrentHashMap<String, long[]> hits = new ConcurrentHashMap<>();

    static boolean allow(String ip, long now) {
        // Drop expired windows so the map can't grow forever
        hits.values().removeIf(w -> now - w[0] >= WINDOW_MS);
        long[] w = hits.compute(ip, (k, v) -> {
            if (v == null || now - v[0] >= WINDOW_MS)
                return new long[] { now, 1 };
            v[1]++;
            return v;
        });
        return w[1] <= MAX_REPORTS;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!allow(req.getRemoteAddr(), System.currentTimeMillis())) {
            res.sendError(429);
            return;
        }
        String hash = req.getParameter("hash");
        String reason = req.getParameter("reason");
        if (hash == null || hash.isEmpty()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (reason == null)
            reason = "";
        if (reason.length() > 200)
            reason = reason.substring(0, 200);

        ServletContext application = getServletConfig().getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
            // Only accept reports for games that exist
            PreparedStatement check = connection.prepareStatement("select 1 from Games where hash=?");
            check.setString(1, hash);
            if (!check.executeQuery().next()) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            PreparedStatement ps = connection.prepareStatement("insert into Reports values (?, ?, ?)");
            ps.setString(1, hash);
            ps.setString(2, reason);
            ps.setLong(3, System.currentTimeMillis() / 1000);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
