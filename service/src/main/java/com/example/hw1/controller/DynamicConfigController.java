package com.example.hw1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates Nacos dynamic config: change `app.dynamic.welcome-message`
 * in Nacos console and the next call to `/api/dynamic/welcome` reflects it
 * without restarting the app (thanks to @RefreshScope).
 */
@RestController
@RequestMapping("/api/dynamic")
@RefreshScope
public class DynamicConfigController {

    @Value("${app.dynamic.welcome-message:Hello from local yaml}")
    private String welcomeMessage;

    @Value("${app.instance-id:local}")
    private String instanceId;

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage + " | instance=" + instanceId;
    }
}
