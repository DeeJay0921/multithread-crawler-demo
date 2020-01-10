package com.github.DeeJay0921;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main主类用来调度线程
 */
public class Main {
    public static void main(String[] args) {
        CrawlerDao dao = new MybatisCrawlerDao();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i ++) {
            threadPool.submit(new Crawler(dao));
        }
    }
}
