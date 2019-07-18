package com.difegue.doujinsoft.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.difegue.doujinsoft.utils.MioUtils.Types;

/**
 * Template Builder for Surveys.
 */
public class TemplateBuilderSurvey extends TemplateBuilder {

    public TemplateBuilderSurvey(ServletContext application, HttpServletRequest request) throws SQLException {
        super(application, request);
    }

    public String doGetSurveys() throws Exception {

        initializeTemplate(Types.SURVEY, false);
        ResultSet result = statement.executeQuery("select * from "+tableName+" ORDER BY timestamp ASC LIMIT 50");
        
        while(result.next()) 
            items.add(classConstructor.newInstance(result));
        
        result = statement.executeQuery("select COUNT(timestamp) from "+tableName);
        context.put("items", items);
        context.put("totalitems", result.getInt(1));
        
        //Output to client
        return writeToTemplate();
    }

    public String doPostSurveys() throws Exception {

        initializeTemplate(Types.SURVEY, true);
        
        String query = "SELECT * FROM "+tableName + " ORDER BY timestamp ASC LIMIT 50 OFFSET ?";
        String queryCount = "SELECT COUNT(timestamp) FROM "+tableName;
        
        PreparedStatement ret = connection.prepareStatement(query);
        PreparedStatement retCount = connection.prepareStatement(queryCount);

        int page = 1;
        if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
            page = Integer.parseInt(request.getParameter("page"));
        
        ret.setInt(1, page*50-50);
        ResultSet result = ret.executeQuery();
        
        while(result.next()) 
            items.add(classConstructor.newInstance(result));
        
        context.put("items", items);
        context.put("totalitems", retCount.executeQuery().getInt(1));
        
        return writeToTemplate();
    }
}