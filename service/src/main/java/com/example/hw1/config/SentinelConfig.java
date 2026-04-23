package com.example.hw1.config;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Registers built-in Sentinel rules so rate limiting + circuit breaking work
 * out of the box without needing the Sentinel dashboard.
 *
 * In production, these would be loaded from Nacos instead — but for a
 * homework demo we wire minimal rules in code.
 */
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        // ---------- Flow control (QPS limiting) ----------
        List<FlowRule> flowRules = new ArrayList<>();

        FlowRule seckill = new FlowRule("seckill");
        seckill.setGrade(RuleConstant.FLOW_GRADE_QPS);
        seckill.setCount(200); // 200 QPS per process
        flowRules.add(seckill);

        FlowRule productDetail = new FlowRule("product-detail");
        productDetail.setGrade(RuleConstant.FLOW_GRADE_QPS);
        productDetail.setCount(500);
        flowRules.add(productDetail);

        FlowRuleManager.loadRules(flowRules);

        // ---------- Circuit breaker (degrade) ----------
        List<DegradeRule> degradeRules = new ArrayList<>();
        DegradeRule productCb = new DegradeRule("product-detail");
        productCb.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
        productCb.setCount(0.5);   // 50% error ratio
        productCb.setMinRequestAmount(20);
        productCb.setStatIntervalMs(10_000);
        productCb.setTimeWindow(10); // re-probe after 10s
        degradeRules.add(productCb);
        DegradeRuleManager.loadRules(degradeRules);
    }
}
