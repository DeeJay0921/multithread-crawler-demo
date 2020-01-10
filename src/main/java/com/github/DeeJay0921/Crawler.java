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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Crawler extends Thread{

    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    @Override
    public void run() {
        try {
            String link;
            while ((link = dao.getNextLinkThenDelete()) != null) { // 从库里去加载下一条链接 如果能加载到才进行循环
                // 直接去查询数据库看该link有没有被处理过
                if (dao.isLinkProcessed(link)) {
                    continue;
                }

                if (isInterestingLink(link)) { // 如果是感兴趣的页面
                    System.out.println(Thread.currentThread().getName() + " is crawling " + link);
                    Document document = Jsoup.parse(getStringHtml(validateLink(link)));
                    // 将爬取到的新页面上的链接入库
                    insertNewLinksToDatabase(document);
                    // 对于新闻页做额外处理
                    storeIntoDataBaseIfIsNews(link, document);
                    // 将访问过的链接加入已处理的数据库
                    dao.insertLinkIntoProcessed(link);
//                dao.updateDataBase(link, "insert into LINKS_ALREADY_PROCESSED values ( ? )");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertNewLinksToDatabase(Document document) throws SQLException {

        Elements aLinks = document.select("a"); // 获取所有的a标签
        // 将链接加入连接池
        for (Element alink : aLinks) {
            String href = alink.attr("href");
            if (isInterestingLink(href)) {
                dao.insertLinkIntoToBeProcessed(href);
//                dao.updateDataBase(href, "insert into LINKS_TO_BE_PROCESSED values ( ? )");
            }
        }
    }

    private void storeIntoDataBaseIfIsNews(String link, Document document) throws SQLException {
        Elements articleTags = document.select(".art_box");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String articleTitle = articleTag.child(0).text();
                String articleContent = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase(link, articleTitle, articleContent);
            }
        }
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
