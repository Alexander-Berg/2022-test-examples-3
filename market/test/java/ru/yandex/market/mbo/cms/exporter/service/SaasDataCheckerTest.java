package ru.yandex.market.mbo.cms.exporter.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.SaasIndexEntity;
import ru.yandex.market.mbo.cms.core.dto.IndexKey;
import ru.yandex.market.mbo.cms.core.dto.IndexResult;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.SaasAction;
import ru.yandex.market.mbo.cms.core.service.Stand;
import ru.yandex.market.mbo.cms.exporter.model.IndexOperationContextBuilder;
import ru.yandex.market.mbo.cms.exporter.util.ExporterTestUtils;

public class SaasDataCheckerTest {

    private SaasDataChecker saasDataChecker;
    private SaasIndexContentService saasIndexContentService;
    private MetricsLogger metricsLogger;

    @Before
    public void init() throws IOException {
        metricsLogger = Mockito.mock(MetricsLogger.class);
        saasIndexContentService = Mockito.mock(SaasIndexContentService.class);
        Mockito.when(saasIndexContentService.checkIsActionActual(Mockito.any())).thenReturn(true);
        saasDataChecker = Mockito.spy(new SaasDataChecker(saasIndexContentService, metricsLogger));
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any()))
                .thenAnswer((o) -> ExporterTestUtils.makeOkHttpResponse("{}"));
        saasDataChecker.httpClient = httpClient;
        saasDataChecker.setCheckRetryInterval(0);
        saasDataChecker.setCheckRetryCount(1);
    }

    @Test
    @Ignore
    @SuppressWarnings("magicnumber")
    public void testRetry() throws InterruptedException {
        IndexResult indexResult = new IndexResult(
                new IndexKey(
                        Stand.MAIN,
                        SaasIndexEntity.WIDGET,
                        SaasAction.MODIFY,
                        "key",
                        System.currentTimeMillis()
                )
        );

        AtomicInteger counter = new AtomicInteger();
        saasDataChecker.doCheck(indexResult,
                new IndexOperationContextBuilder()
                        .withMaxRetryCount(1)
                        .withRetryCallback(o -> counter.incrementAndGet())
                        .withForce(true)
                        .build());
        Thread.sleep(500);
        Assert.assertEquals(1, counter.get());
    }

    @Test
    @Ignore
    @SuppressWarnings("magicnumber")
    public void testNoRetry() throws InterruptedException {
        IndexResult indexResult = new IndexResult(
                new IndexKey(
                        Stand.MAIN,
                        SaasIndexEntity.WIDGET,
                        SaasAction.MODIFY,
                        "key",
                        System.currentTimeMillis()
                )
        );

        AtomicInteger counter = new AtomicInteger();
        saasDataChecker.doCheck(indexResult,
                new IndexOperationContextBuilder()
                        .withMaxRetryCount(0)
                        .withRetryCallback(o -> counter.incrementAndGet())
                        .withForce(true)
                        .build());

        Thread.sleep(500);
        Assert.assertEquals(0, counter.get());
    }
}
