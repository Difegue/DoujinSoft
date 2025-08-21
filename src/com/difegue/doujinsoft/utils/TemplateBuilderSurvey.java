package com.difegue.doujinsoft.utils;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.difegue.doujinsoft.utils.MioUtils.Types;

/**
 * TemplateBuilder Extension for Surveys.
 */
public class TemplateBuilderSurvey extends TemplateBuilder {

    public TemplateBuilderSurvey(ServletContext application, HttpServletRequest request) throws SQLException {
        super(application, request);
    }

    public String doGetSurveys() throws Exception {

        initializeTemplate(Types.SURVEY, false);
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("select * from "+tableName+" ORDER BY timestamp DESC LIMIT 50");
        
        while(result.next()) 
            items.add(classConstructor.newInstance(result));

        result.close();

        result = statement.executeQuery("select COUNT(timestamp) from "+tableName);
        context.put("items", items);
        context.put("totalitems", result.getInt(1));
        
        result.close();
        statement.close();
        connection.close();
        //Output to client
        return writeToTemplate();
    }

    public String doPostSurveys() throws Exception {

        initializeTemplate(Types.SURVEY, true);
        
        // Check if this is a name search
        boolean isNameSearch = request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty();
        
        String query, queryCount;
        PreparedStatement ret, retCount;
        
        if (isNameSearch) {
            // Build search query with name filter
            String nameFilter = request.getParameter("name") + "%";
            query = "SELECT * FROM " + tableName + " WHERE name LIKE ? ORDER BY timestamp DESC LIMIT 50 OFFSET ?";
            queryCount = "SELECT COUNT(timestamp) FROM " + tableName + " WHERE name LIKE ?";
            
            ret = connection.prepareStatement(query);
            retCount = connection.prepareStatement(queryCount);
            
            // Set search parameters
            ret.setString(1, nameFilter);
            retCount.setString(1, nameFilter);
            
            // Add search term to context for display
            context.put("nameSearch", request.getParameter("name"));
        } else {
            // Standard query without filter
            query = "SELECT * FROM " + tableName + " ORDER BY timestamp DESC LIMIT 50 OFFSET ?";
            queryCount = "SELECT COUNT(timestamp) FROM " + tableName;
            
            ret = connection.prepareStatement(query);
            retCount = connection.prepareStatement(queryCount);
            
            // Clear search context
            context.put("nameSearch", "");
        }

        int page = 1;
        if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
            page = Integer.parseInt(request.getParameter("page"));
        
        // Set pagination offset (different parameter position depending on search)
        if (isNameSearch) {
            ret.setInt(2, page*50-50);
        } else {
            ret.setInt(1, page*50-50);
        }
        
        ResultSet result = ret.executeQuery();
        
        while(result.next()) 
            items.add(classConstructor.newInstance(result));
        
        result.close();
        ret.close();

        context.put("items", items);
        context.put("totalitems", retCount.executeQuery().getInt(1));
        
        retCount.close();
        connection.close();
        return writeToTemplate();
    }
}