package com.github.DeeJay0921;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于向Elasticsearch中插入数据
 */
public class ElasticsearchDataGenerator {
    private SqlSessionFactory sqlSessionFactory;

    public ElasticsearchDataGenerator() {
        try {
            String resource = "db/mybatis/mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        ElasticsearchDataGenerator elasticsearchDataGenerator = new ElasticsearchDataGenerator();
        elasticsearchDataGenerator.insertDataIntoES(elasticsearchDataGenerator.selectNewsFromDatabase());
    }

    private void insertDataIntoES(List<News> newsFromDatabase) throws IOException {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            for (News news : newsFromDatabase) {
                IndexRequest request = new IndexRequest("news");

                Map<String, Object> param = new HashMap<>();
                param.put("title", news.getTitle());
                param.put("url", news.getUrl());
                param.put("content", news.getContent());
                request.source(param, XContentType.JSON);

                IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                System.out.println("response = " + response.status().getStatus());
            }
        }
    }

    public List<News> selectNewsFromDatabase() {
        try {
            SqlSession session = sqlSessionFactory.openSession();
            return session.selectList("com.github.DeeJay0921.mock.selectNews");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
