# multithread-crawler-demo

本项目为一个练手的爬虫项目，通过爬虫爬取`https://sina.cn`来获取数据存放到本地`H2`数据库

通过`Elasticsearch`对爬取到的新闻信息可以做检索。

## 使用

初始化数据库: `mvn flyway:migrate`

爬取数据: 执行运行`Main.main()`即可

假造数据扩充数据库: `FakeDataGenerator.main()`

从数据库中读取数据插入到`ES`: `ElasticsearchDataGenerator.main()`

运行搜索引擎: `ElasticsearchEngine.main()`