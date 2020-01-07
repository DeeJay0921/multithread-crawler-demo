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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        ArrayList<String> linkPool = new ArrayList<>(); // 未处理过的连接池
        linkPool.add("https://sina.cn");
        Set<String> handledLinkPool = new HashSet<>(); // 已经处理过的链接池
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.get(0);
            linkPool.remove(0); // 处理过后从未处理的连接池中删除该链接

            if (handledLinkPool.contains(link)) { // 如果该链接已经处理过 跳出本次循环
                continue;
            }

            if (!link.contains("sina.cn")) { // 如果不包含sina.cn 等关键字 说明不是新浪本站的页面 不做处理 直接跳过
                continue;
            } else { // 合法页面  进行请求
                System.out.println("link = " + link);
                String stringHtml = getStringHtml(link);
                handledLinkPool.add(link); // 处理完成后加入已经处理的连接池
                Document document = Jsoup.parse(stringHtml);
                Elements aLinks = document.select("a"); // 获取所有的a标签

                // 将链接加入连接池
                for (Element alink : aLinks) {
                    String aLinkHref = alink.attr("href");
                    if (aLinkHref != null && aLinkHref.contains("sina.cn")) {
                        linkPool.add(alink.attr("href"));
                    }
                }

                // 对于新闻页做额外处理
                Elements articleTags = document.select("article");
                if (!articleTags.isEmpty()) {
                    for (Element articleTag : articleTags) {
                        String articleTitle = articleTag.child(0).text(); // 获取新闻文章标题  输出 之后改为入库
                        System.out.println("articleTitle = " + articleTitle);
                    }
                }
            }
        }
    }

    private static String getStringHtml(String url) {
        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
        String html = null;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            html = EntityUtils.toString(entity1);
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
//        HttpHost proxy = new HttpHost("10.30.6.49", 9090);
//        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
//        return HttpClients.custom()
//                .setRoutePlanner(routePlanner)
//                .setSSLSocketFactory(sslsf)
//                .build();
    }
}
