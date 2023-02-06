package ru.yandex.market.mbo.cms.exporter.servlets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.SaasIndexEntity;
import ru.yandex.market.mbo.cms.core.dto.ExportKeyValue;
import ru.yandex.market.mbo.cms.core.dto.IndexKey;
import ru.yandex.market.mbo.cms.core.dto.IndexResult;
import ru.yandex.market.mbo.cms.core.dto.KeyValueDataContainer;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.SaasAction;
import ru.yandex.market.mbo.cms.core.service.Stand;
import ru.yandex.market.mbo.cms.exporter.service.SaasPushService;

public class ExportKeysToSaasServletTest {
    private ExportKeysToSaasServlet servlet;

    private SaasPushService saasPushService;
    private MetricsLogger metricsLogger;

    @Before
    public void init() {
        saasPushService = Mockito.mock(SaasPushService.class);
        metricsLogger = Mockito.mock(MetricsLogger.class);

        ExportKeysToSaasServlet exportKeysToSaasServlet = new ExportKeysToSaasServlet(saasPushService, metricsLogger);
        exportKeysToSaasServlet = Mockito.spy(exportKeysToSaasServlet);

        servlet = exportKeysToSaasServlet;

        configurePushService();
    }

    @Test
    public void testCommonCase() {
        KeyValueDataContainer container = new KeyValueDataContainer(
                Arrays.asList(new ExportKeyValue("1", "{}"), new ExportKeyValue("2", "{}")),
                Arrays.asList(new ExportKeyValue("r1", "1"), new ExportKeyValue("r2", "2")),
                Arrays.asList("r3", "r4"),
                System.currentTimeMillis()
        );

        servlet.doProcess(container, Stand.MAIN, true, 1);

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.WIDGET), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).removeDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testRemoveKeysReductionIfKeyIsForAddition() {
        KeyValueDataContainer container = new KeyValueDataContainer(
                Arrays.asList(new ExportKeyValue("1", "{}"), new ExportKeyValue("2", "{}")),
                Arrays.asList(new ExportKeyValue("r1", "1"), new ExportKeyValue("r2", "2")),
                Arrays.asList("r1", "r3"),
                System.currentTimeMillis()
        );

        servlet.doProcess(container, Stand.MAIN, true, 1);

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.WIDGET), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).removeDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 1),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @SuppressWarnings("magicnumber")
    private void configurePushService() {
        Mockito.doAnswer(o -> {
            Collection<ExportKeyValue> keyValuePairs = o.getArgument(1);
            SaasIndexEntity entity = o.getArgument(2);
            Stand stand = o.getArgument(3);
            return keyValuePairs.stream()
                    .map(kv -> {
                        FutureTask task = new FutureTask(() ->
                                new IndexResult(
                                        new IndexKey(
                                                stand,
                                                entity,
                                                SaasAction.MODIFY,
                                                kv.getKey(),
                                                (new Date()).getTime())));
                        task.run();
                        return task;
                    })
                    .collect(Collectors.toList());
        })
                .when(saasPushService)
                .pushDataAsync(Mockito.anyLong(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer(o -> {
            Collection<String> keyValuePairs = o.getArgument(1);
            SaasIndexEntity entity = o.getArgument(2);
            Stand stand = o.getArgument(3);
            return keyValuePairs.stream()
                    .map(k -> {
                        FutureTask task = new FutureTask(() ->
                                new IndexResult(
                                        new IndexKey(
                                                stand,
                                                entity,
                                                SaasAction.DELETE,
                                                k,
                                                (new Date()).getTime())));
                        task.run();
                        return task;
                    })
                    .collect(Collectors.toList());
        })
                .when(saasPushService)
                .removeDataAsync(Mockito.anyLong(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any());
    }
}
