package com.example.hw1.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = DataSourceRoutingTest.TestConfig.class)
class DataSourceRoutingTest {

    @Autowired
    private SampleRoutingService sampleRoutingService;

    @Test
    void shouldUseMasterByDefault() {
        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.get());
    }

    @Test
    void shouldSwitchToSlaveForAnnotatedReadMethod() {
        DataSourceType route = sampleRoutingService.readRoute();

        assertEquals(DataSourceType.SLAVE, route);
    }

    @Test
    void shouldClearRouteAfterAnnotatedReadMethod() {
        sampleRoutingService.readRoute();

        assertEquals(DataSourceType.MASTER, DataSourceContextHolder.get());
    }

    @TestConfiguration
    @EnableAspectJAutoProxy
    @Import(ReadOnlyRouteAspect.class)
    static class TestConfig {

        @Bean
        SampleRoutingService sampleRoutingService() {
            return new SampleRoutingService();
        }
    }

    static class SampleRoutingService {

        @ReadOnlyRoute
        DataSourceType readRoute() {
            return DataSourceContextHolder.get();
        }
    }
}
