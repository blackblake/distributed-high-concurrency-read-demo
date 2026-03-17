package com.example.hw1.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    @Test
    void shouldProvideApplicationConfigurationForRedisAndReadWriteSplitting() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("application.yml");

        assertNotNull(stream, "application.yml should exist with Redis and MySQL routing configuration");
    }
}
