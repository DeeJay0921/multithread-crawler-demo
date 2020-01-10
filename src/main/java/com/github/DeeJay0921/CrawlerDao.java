package com.github.DeeJay0921;

import java.sql.SQLException;

/**
 * 爬虫数据访问方式的标准接口
 */
public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;

//    void updateDataBase(String link, String sql) throws SQLException;

    void insertNewsIntoDatabase(String link, String articleTitle, String articleContent) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertLinkIntoProcessed(String link) throws SQLException;

    void insertLinkIntoToBeProcessed(String href) throws SQLException;
}
