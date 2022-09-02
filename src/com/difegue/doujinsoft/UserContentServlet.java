package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.utils.CollectionUtils;
import com.difegue.doujinsoft.utils.TemplateBuilderCollection;

/**
 * Servlet implementation class for User Content
 * User Content are all MIO types (games, records, manga) generated from a unique save file.
 */
@WebServlet("/collection")
public class UserContentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Logger ServletLog;

    //TODO

}
