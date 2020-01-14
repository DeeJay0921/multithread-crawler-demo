package com.github.DeeJay0921;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * 用来通过现有的爬取到的一些数据 通过复制 在库里创造百万级别的数据
 */
public class FakeDataGenerator {
    private final int TARGET_COUNT = 1000000; // 目标一百万条数据
    private SqlSessionFactory sqlSessionFactory;

    public FakeDataGenerator() {
        try {
            String resource = "db/mybatis/mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        FakeDataGenerator fakeDataGenerator = new FakeDataGenerator();
        fakeDataGenerator.insertFakeNewsToDatabase(fakeDataGenerator.selectNewsFromDatabase());
    }

    public List<News> selectNewsFromDatabase() {
        try {
            SqlSession session = sqlSessionFactory.openSession(true);
            return session.selectList("com.github.DeeJay0921.mock.selectNews");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insertFakeNewsToDatabase(List<News> news) {
        try {
            int count = TARGET_COUNT - news.size();
            Random random = new Random();
            SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH);
            try {
                while (count-- > 0) {
                    int randomIndex = random.nextInt(news.size());
                    News newsToBeInserted = news.get(randomIndex); // 随机从news获取一个元素

                    Instant createdAt = newsToBeInserted.getCreatedAt();
                    Instant randomCreatedAt = createdAt.minusSeconds(365 * 3600 * 24);
                    newsToBeInserted.setCreatedAt(randomCreatedAt);
                    newsToBeInserted.setUpdatedAt(randomCreatedAt); // 修改一下2个时间戳

                    session.insert("com.github.DeeJay0921.mock.insertFakeData", newsToBeInserted);
                    if (count % 2000 == 0) {
                        session.flushStatements();
                        System.out.println("left: " + count);
                    }
                }
                session.commit(); // 如果上述操作都成功则commit
            } catch (Exception e) {
                session.rollback(); // 如果不成功则回滚 即保证原子性
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
