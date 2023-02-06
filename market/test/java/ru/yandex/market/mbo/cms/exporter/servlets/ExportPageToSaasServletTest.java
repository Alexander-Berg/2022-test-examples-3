package ru.yandex.market.mbo.cms.exporter.servlets;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.SaasIndexEntity;
import ru.yandex.market.mbo.cms.core.dto.ExportKeyValue;
import ru.yandex.market.mbo.cms.core.dto.IndexKey;
import ru.yandex.market.mbo.cms.core.dto.IndexResult;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.SaasAction;
import ru.yandex.market.mbo.cms.core.service.Stand;
import ru.yandex.market.mbo.cms.exporter.service.SaasPushService;

public class ExportPageToSaasServletTest {
    public static final String DELETE_RELATIONS_URL = "delete-relations";
    public static final String ADD_RELATIONS_URL = "add-relations";
    public static final String ADD_WIDGETS_URL = "add-widgets";
    private ExportPageToSaasServlet servlet;

    private SaasPushService saasPushService;
    private MetricsLogger metricsLogger;

    @Before
    public void init() {
        saasPushService = Mockito.mock(SaasPushService.class);
        metricsLogger = Mockito.mock(MetricsLogger.class);

        ExportPageToSaasServlet exportPageToSaasServlet = new ExportPageToSaasServlet(saasPushService, metricsLogger);
        exportPageToSaasServlet.httpClient = Mockito.mock(HttpClient.class);
        exportPageToSaasServlet = Mockito.spy(exportPageToSaasServlet);

        servlet = exportPageToSaasServlet;

        configurePushService();
    }

    @Test
    public void testCommonCase() {
        Mockito.doReturn(ADD_WIDGETS_URL).when(servlet)
                .getWidgetsUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn(ADD_RELATIONS_URL).when(servlet)
                .getActualKeysUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn(DELETE_RELATIONS_URL).when(servlet)
                .getDeletedKeysUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn("1:{}\n2:{}").when(servlet)
                .getRequestResult(ADD_WIDGETS_URL);
        Mockito.doReturn("r1:1\nr2:2").when(servlet)
                .getRequestResult(ADD_RELATIONS_URL);
        Mockito.doReturn("r3\nr4").when(servlet)
                .getRequestResult(DELETE_RELATIONS_URL);

        servlet.doProcess(1L, true, Stand.MAIN, true, null);

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.WIDGET), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).pushDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(saasPushService).removeDataAsync(Mockito.anyLong(), ArgumentMatchers.argThat(o -> o.size() == 2),
                Mockito.eq(SaasIndexEntity.RELATION), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testRemoveKeysReductionIfKeyIsForAddition() {
        Mockito.doReturn(ADD_WIDGETS_URL).when(servlet)
                .getWidgetsUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn(ADD_RELATIONS_URL).when(servlet)
                .getActualKeysUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn(DELETE_RELATIONS_URL).when(servlet)
                .getDeletedKeysUrl(Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn("1:{}\n2:{}").when(servlet)
                .getRequestResult(ADD_WIDGETS_URL);
        Mockito.doReturn("r1:1\nr2:2").when(servlet)
                .getRequestResult(ADD_RELATIONS_URL);
        Mockito.doReturn("r1\nr3").when(servlet)
                .getRequestResult(DELETE_RELATIONS_URL);

        servlet.doProcess(1L, true, Stand.MAIN, true, null);

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
                                        new IndexKey(stand,
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
