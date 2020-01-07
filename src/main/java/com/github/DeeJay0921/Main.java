package com.github.DeeJay0921;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet = new HttpGet("http://sina.cn");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            System.out.println("EntityUtils.toString(entity1) = " + EntityUtils.toString(entity1));
        }
    }

    private static CloseableHttpClient getHttpClient() throws IOException {
        // 先调用一次 判断是否为内网 如果内网则要做代理
        CloseableHttpClient testClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://sina.cn");
        try (CloseableHttpResponse response1 = testClient.execute(httpGet)) {
            if (response1.getStatusLine().getStatusCode() == 403) {
                HttpHost proxy = new HttpHost("10.30.6.49", 9090);
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                return HttpClients.custom()
                        .setRoutePlanner(routePlanner)
                        .build();
            }
        }
        return HttpClients.createDefault();
    }
}
