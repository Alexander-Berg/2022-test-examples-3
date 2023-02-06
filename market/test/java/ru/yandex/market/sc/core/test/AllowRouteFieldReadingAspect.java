package ru.yandex.market.sc.core.test;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;

@Aspect
@Component
public class AllowRouteFieldReadingAspect {

    @Pointcut("within(@AllowRouteFieldReading *)")
    void allowRouteReadingAnnotated() {}

    @Before("allowRouteReadingAnnotated()")
    public void beforeMethod(JoinPoint joinPoint)  {
        String signature = joinPoint.getSignature().toShortString(); // Для дебага
        RouteSoMigrationHelper.allowRouteReading();
    }

    @After("allowRouteReadingAnnotated()")
    public void afterMethod(JoinPoint joinPoint)  {
        String signature = joinPoint.getSignature().toShortString();
        RouteSoMigrationHelper.revokeRouteReadingPermission();
    }

}