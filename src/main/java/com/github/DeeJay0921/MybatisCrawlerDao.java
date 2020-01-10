package com.github.DeeJay0921;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;

public class MybatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {
        try {
            String resource = "db/mybatis/mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        String link;
        // 这里的openSession 的参数autoCommit一定要为true,否则每次的删除就没有被提交到数据库
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            link = session.selectOne(
                    "com.github.DeeJay0921.mybatis.selectNextLink"); // 这边输入Mapper.xml里面的命名空间加Select语句的id
            if (link != null) {
                session.delete("com.github.DeeJay0921.mybatis.deleteLink", link);
            }
        }
        return link;
    }

    @Override
    public void insertNewsIntoDatabase(String link, String articleTitle, String articleContent) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.DeeJay0921.mybatis.insertNews", new News(articleTitle, articleContent, link));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        int count;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            count = session.selectOne("com.github.DeeJay0921.mybatis.countLink", link);
        }
        return 0 != count;
    }

    @Override
    public void insertLinkIntoProcessed(String link) {
        this.insertIntoDifferentTable("LINKS_ALREADY_PROCESSED", link);
    }

    @Override
    public void insertLinkIntoToBeProcessed(String href) {
        this.insertIntoDifferentTable("LINKS_TO_BE_PROCESSED", href);
    }

    private void insertIntoDifferentTable(String tableName, String link) {
        HashMap<String, String> param = new HashMap<>();
        param.put("tableName", tableName);
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.DeeJay0921.mybatis.insertLink", param);
        }
    }
}
