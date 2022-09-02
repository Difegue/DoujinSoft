package com.difegue.doujinsoft.utils;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.difegue.doujinsoft.templates.*;

/**
 * TemplateBuilder extension for User Content pages.
 */
public class TemplateBuilderUserContent extends TemplateBuilder {

    public TemplateBuilderUserContent(ServletContext application, HttpServletRequest request) throws SQLException {
        super(application, request);
    }

    //TODO

}