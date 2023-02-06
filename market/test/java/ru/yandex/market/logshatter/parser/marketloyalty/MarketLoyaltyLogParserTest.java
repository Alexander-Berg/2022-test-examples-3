package ru.yandex.market.logshatter.parser.marketloyalty;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.parser.marketloyalty.MarketLoyaltyLogParser.DATE_PATTERN;

class MarketLoyaltyLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketLoyaltyLogParser());
    URL resource = getClass().getClassLoader().getResource("market-loyalty.log");
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("market-loyalty.log");
        checker.setHost("testing-market-loyalty-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-loyalty@testing--loyalty-health");
        List<String> lines = FileUtils.readLines(new File(resource.toURI()));

        String requestIdLine = lines.get(0);
        String noRequestIdLine = lines.get(1);
        String stacktraceLine = lines.get(2);


        Date date = dateFormat.parse("2022-04-05 16:13:15,483");
        checker.check(
            requestIdLine,
            date,
            LocalDateTime.parse("2022-04-05T16:13:15.483"), // time
            "market-loyalty", // project
            "market-loyalty", // service
            " <== CoinsController.getCoinsCountForPerson, result ru.yandex.market.loyalty.api.model" +
                ".CountResponse@2a0cc81d", // message
            "test", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1649164395249/d4a53fd4838dbca72a7ef401e8db0500/27", // request_id
            "", // trace_id
            "", // span_id
            "market-loyalty.log", // component
            UUID.fromString("3adb477b-7194-3dc6-97a5-26209c5e68d7"), // record_id
            "", // validation_err
            "{\"loggerName\":\"LogAdvice\",\"jobName\":\"\"}" // rest
        );

        date = dateFormat.parse("2022-04-05 16:13:07,690");
        checker.check(
            noRequestIdLine,
            date,
            LocalDateTime.parse("2022-04-05T16:13:07.690"), // time
            "market-loyalty", // project
            "market-loyalty", // service
            "For user: 1152921504608393376 empty trust balances", // message
            "test", // env
            "", // cluster
            Level.WARN, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-loyalty.log", // component
            UUID.fromString("8f5ccac4-470e-3ec0-a582-321fee1086e3"), // record_id
            "", // validation_err
            "{\"loggerName\":\"TrustApiClient\",\"jobName\":\"\"}" // rest
        );

        date = dateFormat.parse("2022-04-05 16:12:43,521");
        checker.check(
            stacktraceLine,
            date,
            LocalDateTime.parse("2022-04-05T16:12:43.521"), // time
            "market-loyalty", // project
            "market-loyalty", // service
            "error getting balance for uid 372301071 org.springframework.web.client" +
                ".HttpServerErrorException$GatewayTimeout: 504 Gateway Timeout: [{\"status\":\"error\",\"code\":504," +
                "\"data\":{\"error\":\"invalid response from billing-wallet, status_code=500, received " +
                "{\\\"code\\\":\\\"500\\\",\\\"message\\\":\\\"Internal Server Error\\\"}\"}}]|       at org" +
                ".springframework.web.client.HttpServerErrorException.create(HttpServerErrorException.java:116)|   at" +
                " org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler" +
                ".java:186)|        at org.springframework.web.client.DefaultResponseErrorHandler.handleError" +
                "(DefaultResponseErrorHandler.java:125)|        at org.springframework.web.client" +
                ".ResponseErrorHandler.handleError(ResponseErrorHandler.java:63)|       at org.springframework.web" +
                ".client.RestTemplate.handleResponse(RestTemplate.java:782)|   at org.springframework.web.client" +
                ".RestTemplate.doExecute(RestTemplate.java:740)|        at org.springframework.web.client" +
                ".RestTemplate.execute(RestTemplate.java:674)|  at ru.yandex.market.loyalty.core.trust.TrustApiClient" +
                ".getForObject(TrustApiClient.java:124)|    at ru.yandex.market.loyalty.core.trust.TrustApiClient" +
                ".getYandexAccountBalance(TrustApiClient.java:71)|  at ru.yandex.market.loyalty.core.trust" +
                ".TrustApiClient$$FastClassBySpringCGLIB$$48ef8f4a.invoke(<generated>)|    at org.springframework" +
                ".cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)|    at org.springframework.aop.framework" +
                ".CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:771)|       at org" +
                ".springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation" +
                ".java:163)|   at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed" +
                "(CglibAopProxy.java:749)|       at org.springframework.aop.interceptor.ExposeInvocationInterceptor" +
                ".invoke(ExposeInvocationInterceptor.java:95)| at org.springframework.aop.framework" +
                ".ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)|   at org.springframework" +
                ".aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:749)|       at org" +
                ".springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy" +
                ".java:691)| at ru.yandex.market.loyalty.core.trust.TrustApiClient$$EnhancerBySpringCGLIB$$531152c8" +
                ".getYandexAccountBalance(<generated>)|    at ru.yandex.market.loyalty.core.service.perks.impl" +
                ".YandexCashbackPerkProcessor.getYandexAccountBalance(YandexCashbackPerkProcessor.java:97)|   at ru" +
                ".yandex.market.loyalty.core.service.perks.impl.YandexCashbackPerkProcessor" +
                ".lambda$getYandexCashbackBalance$0(YandexCashbackPerkProcessor.java:87)| at ru.yandex.market.loyalty" +
                ".core.service.cache.MemcachedCache.queryValueFromSupplier(MemcachedCache.java:157)|  at ru.yandex" +
                ".market.loyalty.core.service.cache.MemcachedCache.lambda$get$0(MemcachedCache.java:70)|     at java" +
                ".base/java.util.Optional.orElseGet(Optional.java:369)|   at ru.yandex.market.loyalty.core.service" +
                ".cache.MemcachedCache.get(MemcachedCache.java:70)|      at ru.yandex.market.loyalty.core.service" +
                ".cache.CacheWithNamespace.get(CacheWithNamespace.java:35)|      at ru.yandex.market.loyalty.core" +
                ".service.perks.impl.YandexCashbackPerkProcessor.getYandexCashbackBalance(YandexCashbackPerkProcessor" +
                ".java:85)|  at ru.yandex.market.loyalty.core.service.perks.impl.YandexCashbackPerkProcessor" +
                ".processFeature(YandexCashbackPerkProcessor.java:62)|    at ru.yandex.market.loyalty.core.service" +
                ".perks.impl.YandexCashbackPerkProcessor$$FastClassBySpringCGLIB$$f5b6c454.invoke(<generated>)|  at " +
                "org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)|    at org.springframework" +
                ".aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:771)|       at" +
                " org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation" +
                ".java:163)|   at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed" +
                "(CglibAopProxy.java:749)|       at org.springframework.retry.annotation" +
                ".AnnotationAwareRetryOperationsInterceptor.invoke(AnnotationAwareRetryOperationsInterceptor" +
                ".java:156)|   at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed" +
                "(ReflectiveMethodInvocation.java:186)|   at org.springframework.aop.framework" +
                ".CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:749)|       at org.springframework" +
                ".aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:691)| at ru" +
                ".yandex.market.loyalty.core.service.perks.impl" +
                ".YandexCashbackPerkProcessor$$EnhancerBySpringCGLIB$$a301f33b.processFeature(<generated>)|   at ru" +
                ".yandex.market.loyalty.core.service.perks.PerkService.processFeature(PerkService.java:580)|        " +
                "at ru.yandex.market.loyalty.core.service.perks.PerkService.lambda$processFeatures$15(PerkService" +
                ".java:572)|     at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline" +
                ".java:195)| at java.base/java.util.Collections$2.tryAdvance(Collections.java:4747)| at java" +
                ".base/java.util.Collections$2.forEachRemaining(Collections.java:4755)|   at java.base/java.util" +
                ".stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)|     at java.base/java.util.stream" +
                ".AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)|      at java.base/java.util.stream" +
                ".ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:913)|        at java.base/java.util.stream" +
                ".AbstractPipeline.evaluate(AbstractPipeline.java:234)|     at java.base/java.util.stream" +
                ".ReferencePipeline.collect(ReferencePipeline.java:578)|    at ru.yandex.market.loyalty.core.service" +
                ".perks.PerkService.processFeatures(PerkService.java:573)|       at ru.yandex.market.loyalty.core" +
                ".service.perks.PerkService.lambda$createFeatureRequestJobs$13(PerkService.java:546)|    at ru.yandex" +
                ".market.loyalty.log.MDCTaskWrapper.lambda$wrap$0(MDCTaskWrapper.java:34)|   at ru.yandex.market" +
                ".loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked(ExceptionUtils.java:61)| at ru.yandex" +
                ".market.loyalty.core.config.DatasourceType.within(DatasourceType.java:52)|  at ru.yandex.market" +
                ".loyalty.core.utils.TaskWrapper.lambda$wrapWithCurrentDatasourceType$0(TaskWrapper.java:12)| at java" +
                ".base/java.util.concurrent.FutureTask.run(FutureTask.java:264)|  at java.base/java.util.concurrent" +
                ".ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)|   at java.base/java.util.concurrent" +
                ".ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)|   at java.base/java.lang.Thread.run" +
                "(Thread.java:834)", // message
            "test", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-loyalty.log", // component
            UUID.fromString("be581fef-b0ef-320c-b29e-404a192cfa83"), // record_id
            "", // validation_err
            "{\"loggerName\":\"TrustApiClient\",\"jobName\":\"\"}" // rest
        );
    }
}
