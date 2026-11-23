package com.difegue.doujinsoft;

import java.io.IOException;
import java.sql.*;

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

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
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
            // shortcut: no rate limiting, add per-IP throttling if the queue gets spammed
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
