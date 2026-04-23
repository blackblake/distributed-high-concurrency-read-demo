package com.example.hw1.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = {
        "com.example.hw1.mapper",
        "com.example.hw1.order.mapper",
        "com.example.hw1.inventory.mapper",
        "com.example.hw1.outbox.mapper"
})
public class MybatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));
        factoryBean.setTypeAliasesPackage(
                "com.example.hw1.domain,com.example.hw1.order.domain,com.example.hw1.inventory.domain,com.example.hw1.outbox.domain");
        return factoryBean.getObject();
    }
}
