package com.difegue.doujinsoft.templates;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;

import com.difegue.doujinsoft.utils.MioStorage;
import com.difegue.doujinsoft.utils.MioUtils;
import com.google.gson.internal.sql.SqlTypesSupport;
import com.xperia64.diyedit.metadata.Metadata;


/*
 * Instances of this class are used when searching by Cartridge ID or Creator ID
 */

public class CreatorDetails {
    
    public String cartridgeId, creatorId;
    public boolean legitCart;

    public int totalGames, totalManga, totalRecords;
    public int timesReset;

    //HashSet<String> creatorIds;
    //HashSet<String> creatorNames, brandNames;
    public String creatorNames, brandNames;

    public CreatorDetails(Connection connection, String creatorId, String cartridgeId) throws SQLException
    {
        Statement statement = connection.createStatement();

        this.creatorId = creatorId;
        this.cartridgeId = cartridgeId;

        if (!cartridgeId.equals("00000000000000000000000000000000"))
            legitCart = true;
        else
            legitCart = false;
        
        this.totalGames = getContentCount(statement, "Games");
        this.totalManga = getContentCount(statement, "Manga");
        this.totalRecords = getContentCount(statement, "Records");

        this.timesReset = getResetCount(statement);

        this.creatorNames = getCreatorNames(statement);
        this.brandNames = getBrandNames(statement);

        statement.close();
    }

    private int getContentCount(Statement statement, String tableName) throws SQLException
    {
        int count = 0;

		// Build count query
		String queryBase = "SELECT COUNT(id) FROM " + tableName + " WHERE ";
        queryBase += "creatorID = '" + creatorId + "'";
		queryBase += legitCart ? " OR cartridgeID = '" + cartridgeId +"'" : "";

        count = statement.executeQuery(queryBase).getInt(1);

        return count;
    }

    private int getResetCount(Statement statement) throws SQLException
    {
        int count = 0;

        // Reset counts can only be calculated from .mio files generated on legitimate cartridges
        if (!legitCart)
            return count;

            String query = "SELECT COUNT(*) FROM( SELECT creatorID FROM Games WHERE cartridgeID = '" + cartridgeId +
            "' UNION SELECT creatorID FROM Records WHERE cartridgeID = '" + cartridgeId +
            "' UNION SELECT creatorID FROM Manga WHERE cartridgeID = '" + cartridgeId + "')";

        count = statement.executeQuery(query).getInt(1) - 1;
        return count;
    }

    //TODO can be refactored with getBrandNames
    private String getCreatorNames(Statement statement) throws SQLException
    {
        ArrayList<String> creatorNamesList = new ArrayList<String>();

        // SELECT creator FROM Games
        // WHERE creatorID = ? OR cartridgeID = ?
        // UNION
        // SELECT creator FROM Records
        // WHERE creatorID = ? OR cartridgeID = ?
        // UNION
        // SELECT creator FROM Manga
        // WHERE creatorID = ? OR cartridgeID = ?

        String selectFromStatement = "SELECT creator FROM ";
        String unionStatement = "UNION ";
        String whereStatement = "WHERE creatorID = '" + creatorId + "' ";
        whereStatement += legitCart ? "OR cartridgeID = '" + cartridgeId + "' " : "";

        String query = selectFromStatement + "Games " + whereStatement + 
        unionStatement +
        selectFromStatement + "Records " + whereStatement + 
        unionStatement +
        selectFromStatement + "Manga " + whereStatement;

        ResultSet resultSet = statement.executeQuery(query);

        while(resultSet.next())
            creatorNamesList.add(resultSet.getString(1));

        return creatorNamesList.toString();
    }

    //TODO can be refactored with getCreatorNames
    private String getBrandNames(Statement statement) throws SQLException
    {
        ArrayList<String> brandNamesList = new ArrayList<String>();

        // SELECT brand FROM Games
        // WHERE creatorID = ? OR cartridgeID = ?
        // UNION
        // SELECT brand FROM Records
        // WHERE creatorID = ? OR cartridgeID = ?
        // UNION
        // SELECT brand FROM Manga
        // WHERE creatorID = ? OR cartridgeID = ?

        String selectFromStatement = "SELECT brand FROM ";
        String unionStatement = "UNION ";
        String whereStatement = "WHERE creatorID = '" + creatorId + "' ";
        whereStatement += legitCart ? "OR cartridgeID = '" + cartridgeId + "' " : "";

        String query = selectFromStatement + "Games " + whereStatement + 
        unionStatement +
        selectFromStatement + "Records " + whereStatement + 
        unionStatement +
        selectFromStatement + "Manga " + whereStatement;

        ResultSet resultSet = statement.executeQuery(query);

        while(resultSet.next())
            brandNamesList.add(resultSet.getString(1));

        return brandNamesList.toString();
    }
}
