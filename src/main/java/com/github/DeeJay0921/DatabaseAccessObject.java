package com.github.DeeJay0921;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据访问对象  用来剥离数据库操作
 */
public class DatabaseAccessObject {

    private final Connection connection;

    public DatabaseAccessObject() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file://" + System.getProperty("user.dir") + "/news");
        } catch (SQLException e) {
            throw new RuntimeException("connection init failed!");
        }
    }

    private String getNextLink(String sql) throws SQLException {
        String link = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                link = resultSet.getString(1);
            }
        }
        return link;
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select LINK from LINKS_TO_BE_PROCESSED LIMIT 1"); // 每次取一个链接出来

        if (link != null) {
            updateDataBase(link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    public void updateDataBase(String link, String sql) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, link);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    public void insertNewsIntoDatabase(String link, String articleTitle, String articleContent) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into NEWS (TITLE, CONTENT, URL) values (?,?,?)")) {
            preparedStatement.setString(1, articleTitle);
            preparedStatement.setString(2, articleContent);
            preparedStatement.setString(3, link);
            preparedStatement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?");
            preparedStatement.setString(1, link);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }
}
