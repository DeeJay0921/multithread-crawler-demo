package com.github.DeeJay0921;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        String workDir = System.getProperty("user.dir");
        String DBurl = "jdbc:h2:file://" + workDir + "/news";
        Connection connection = DriverManager.getConnection(DBurl);

        while (true) {
            List<String> linkPool = loadLinkPoolFromDataBase(connection, "select LINK from LINKS_TO_BE_PROCESSED"); // 从库里读取未处理过的连接池

            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1); // 获取连接池最后一个链接并从数据库及内存中删除该链接
            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");

            // 直接去查询数据库看该link有没有被处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) { // 如果是感兴趣的页面
                String stringHtml = getStringHtml(validateLink(link));
                System.out.println("link = " + link);
                insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED values ( ? )");
                Document document = Jsoup.parse(stringHtml);
                Elements aLinks = document.select("a"); // 获取所有的a标签

                // 将链接加入连接池
                for (Element alink : aLinks) {
                    String href = alink.attr("href");
                    if (isInterestingLink(href)) {
                        insertLinkIntoDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED values ( ? )");
                    }
                }
                // 对于新闻页做额外处理
                storeIntoDataBaseIfIsNews(connection, link, document);
            }
        }
    }

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
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

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void storeIntoDataBaseIfIsNews(Connection connection, String link, Document document) throws SQLException {
        Elements articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String articleTitle = articleTag.child(0).text();
                String articleContent = articleTag.select(".art_content").text();
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into NEWS (TITLE, CONTENT, URL) values (?,?,?)")) {
                    preparedStatement.setString(1, articleTitle);
                    preparedStatement.setString(2, articleContent);
                    preparedStatement.setString(3, link);
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    private static List<String> loadLinkPoolFromDataBase(Connection connection, String sql) throws SQLException {
        List<String> linkPool = new ArrayList<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                linkPool.add(resultSet.getString(1));
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return linkPool;
    }

    private static boolean isInterestingLink(String url) {
        List<String> notInterestingWords = Arrays.asList("signin", "passport", "share", "dp", "site", "reload");
        boolean isInteresting = url.contains("sina.cn");
        for (String notInterestingWord : notInterestingWords) {
            if (url.contains(notInterestingWord)) {
                isInteresting = false;
            }
        }
        return isInteresting;
    }

    private static String validateLink(String link) {
        if (link.startsWith("//")) {
            return "https:" + link;
        }
        return link;
    }

    private static String getStringHtml(String url) {
        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
        String html = "";
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            if (entity1 != null) {
                html = EntityUtils.toString(entity1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    private static CloseableHttpClient getHttpClient() {
        // 使httpClient信任所有证书 为了解决内网访问问题
        SSLContext sslcontext = null;  //建立证书实体
        try {
            sslcontext = SSLContext.getInstance("SSLv3");
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
            javax.net.ssl.TrustManager tm = new MiTM();
            trustAllCerts[0] = tm;
            sslcontext.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        // 先调用一次 判断是否为内网 如果内网则要做代理
        CloseableHttpClient testClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://sina.cn");
        try (CloseableHttpResponse response1 = testClient.execute(httpGet)) {
            if (response1.getStatusLine().getStatusCode() == 403) {
                HttpHost proxy = new HttpHost("10.30.6.49", 9090);
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                return HttpClients.custom()
                        .setRoutePlanner(routePlanner)
                        .setSSLSocketFactory(sslsf)
                        .build();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }
}
