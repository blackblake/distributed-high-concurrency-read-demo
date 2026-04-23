package com.example.hw1.routing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadOnlyRouteAspect {

    @Around("@annotation(com.example.hw1.routing.ReadOnlyRoute)")
    public Object routeToSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.use(DataSourceType.SLAVE);
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
