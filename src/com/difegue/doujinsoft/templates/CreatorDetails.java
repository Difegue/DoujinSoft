package com.difegue.doujinsoft.templates;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/*
 * Instances of this class are used when searching by Cartridge ID or Creator ID
 */

public class CreatorDetails {

    public String cartridgeId, creatorId;
    public boolean legitCart;

    public int totalGames, totalManga, totalRecords;
    public int timesReset;

    public String creatorNames, brandNames;

    public CreatorDetails(Connection connection, String creatorId, String cartridgeId) throws SQLException {
        Statement statement = connection.createStatement();

        this.creatorId = creatorId;
        this.cartridgeId = cartridgeId;

        this.legitCart = !cartridgeId.equals("00000000000000000000000000000000");

        this.totalGames = contentCount(statement, "Games");
        this.totalManga = contentCount(statement, "Manga");
        this.totalRecords = contentCount(statement, "Records");

        this.timesReset = resetCount(statement);

        setCreatorAndBrandNames(statement);

        statement.close();
    }

    private int contentCount(Statement statement, String tableName) throws SQLException {
        int count = 0;

        // Build count query
        String queryBase = "SELECT COUNT(id) FROM " + tableName + " WHERE ";
        queryBase += "creatorID = '" + creatorId + "'";
        queryBase += legitCart ? " OR cartridgeID = '" + cartridgeId + "'" : "";

        count = statement.executeQuery(queryBase).getInt(1);

        return count;
    }

    private int resetCount(Statement statement) throws SQLException {
        int count = 0;

        // Reset counts can only be calculated from .mio files generated on legitimate
        // cartridges
        if (!legitCart)
            return count;

        String query = "SELECT COUNT(*) FROM( SELECT creatorID FROM Games WHERE cartridgeID = '" + cartridgeId +
                "' UNION SELECT creatorID FROM Records WHERE cartridgeID = '" + cartridgeId +
                "' UNION SELECT creatorID FROM Manga WHERE cartridgeID = '" + cartridgeId + "')";

        count = statement.executeQuery(query).getInt(1) - 1;
        return count;
    }

    private void setCreatorAndBrandNames(Statement statement) throws SQLException {
        HashSet<String> creatorNames = new HashSet<String>();
        HashSet<String> brandNames = new HashSet<String>();

        String selectFromStatement = "SELECT creator, brand FROM ";
        String unionStatement = "UNION ";
        String whereStatement = "WHERE creatorID = '" + creatorId + "' ";
        whereStatement += legitCart ? "OR cartridgeID = '" + cartridgeId + "' " : "";

        String query = selectFromStatement + "Games " + whereStatement +
                unionStatement +
                selectFromStatement + "Records " + whereStatement +
                unionStatement +
                selectFromStatement + "Manga " + whereStatement;

        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            creatorNames.add(resultSet.getString(1));
            brandNames.add(resultSet.getString(2));
        }

        this.creatorNames = creatorNames.toString();
        this.brandNames = brandNames.toString();
    }
}
