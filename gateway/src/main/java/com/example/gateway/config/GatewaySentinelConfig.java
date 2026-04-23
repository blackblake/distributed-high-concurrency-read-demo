package com.example.gateway.config;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Loads Sentinel gateway flow rules. The route ids match application.yml.
 */
@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        GatewayFlowRule api = new GatewayFlowRule("hw1-service-api");
        api.setCount(500);
        api.setIntervalSec(1);
        rules.add(api);

        GatewayRuleManager.loadRules(rules);
    }
}
