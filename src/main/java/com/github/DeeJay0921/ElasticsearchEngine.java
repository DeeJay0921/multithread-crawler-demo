package com.github.DeeJay0921;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 借助ES实现一个简易的搜索引擎来搜索爬取到的数据
 */
public class ElasticsearchEngine {
    public static void main(String[] args) throws IOException {
        while (true) {
            System.out.println("Please input a keyword to search: ");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String inputKeyword = bufferedReader.readLine();

            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
                SearchRequest searchRequest = new SearchRequest("news");
                // 采用 MultiMatchQueryBuilder 同时匹配title和content
                searchRequest.source(new SearchSourceBuilder().query(new MultiMatchQueryBuilder(inputKeyword, "title", "content")));

                SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
                response.getHits().forEach(hit -> System.out.println(hit.getSourceAsString()));
            }
        }
    }
}
