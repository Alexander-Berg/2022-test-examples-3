package ru.yandex.market.mbo.cms.exporter.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cms.AbstractTest;
import ru.yandex.market.mbo.cms.core.dao.SaasIndexEntity;
import ru.yandex.market.mbo.cms.core.dto.ExportKeyValue;
import ru.yandex.market.mbo.cms.core.dto.IndexResult;
import ru.yandex.market.mbo.cms.core.models.SaasAction;
import ru.yandex.market.mbo.cms.core.service.Stand;
import ru.yandex.market.mbo.cms.exporter.model.IndexOperationContext;
import ru.yandex.market.mbo.cms.exporter.util.ExporterTestUtils;

public class SaasPushServiceIntegrationTest extends AbstractTest {

    @Autowired
    private SaasPushService saasPushService;

    public SaasPushServiceIntegrationTest() {
    }

    @Before
    public void init() throws Exception {
        saasPushService.httpClient = Mockito.mock(HttpClient.class);
        saasPushService.saasDataChecker.setCheckRetryCount(1);
        saasPushService.saasDataChecker.setCheckRetryInterval(1);
        saasPushService.saasDataChecker = Mockito.spy(saasPushService.saasDataChecker);
        Mockito.when(saasPushService.getHttpClient().execute(Mockito.any()))
                .then(o -> ExporterTestUtils.makeOkHttpResponse("{\"written\": true}"));

        prepareInitialState();
    }

    @SuppressWarnings("magicnumber")
    private void prepareInitialState() throws Exception {
        saasPushAndCheckReqCount(Arrays.asList("1:{000}", "2:{000}", "3:{000}", "4:{000}"),
                Arrays.asList("r11:0", "r12:0", "r2:0", "r3:0", "r4:0"),
                SaasAction.MODIFY, 9, true);
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void pushForce() throws Exception {
        /*
         * Делаем пуш данных
         * W: 1, 2, 3, 4
         * R: r11, r12, r2, r3, r4
         *
         * 9 запросов
         *
         * Удаляем
         * W: 1
         * R: r11, r12
         *
         * 3 запроса
         *
         * Снова удаляем
         * W: 1, 2
         * R: r11, r12, r2
         *
         * 5 запросов
         *
         * Загружаем такие же как и старые
         * W: 3
         * R: r3
         *
         * 2 запроса
         *
         * Загружаем
         * W: 3 (новую), 4 (как старую)
         * R: r3 (новую), 4 (как старую)
         *
         * 4 запроса
         *
         */

        saasPushAndCheckReqCount(Arrays.asList("1:{1}", "2:{2}", "3:{3}", "4:{4}"),
                Arrays.asList("r11:1", "r12:1", "r2:2", "r3:3", "r4:4"),
                SaasAction.MODIFY, 9, true);

        saasPushAndCheckReqCount(Arrays.asList("1"), Arrays.asList("r11", "r12"),
                SaasAction.DELETE, 3, true);

        saasPushAndCheckReqCount(Arrays.asList("1", "2"), Arrays.asList("r11", "r12", "r2"),
                SaasAction.DELETE, 5, true);

        saasPushAndCheckReqCount(Arrays.asList("3:{3}"), Arrays.asList("r3:3"),
                SaasAction.MODIFY, 2, true);

        saasPushAndCheckReqCount(Arrays.asList("3:{3new}", "4:{4}"), Arrays.asList("r3:3,4", "r4:4"),
                SaasAction.MODIFY, 4, true);
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void pushNotForce() throws Exception {
        /*
         * Делаем пуш данных
         * W: 1, 2, 3, 4
         * R: r11, r12, r2, r3, r4
         *
         * 9 запросов
         *
         * Удаляем
         * W: 1
         * R: r11, r12
         *
         * 3 запроса
         *
         * Снова удаляем
         * W: 1, 2
         * R: r11, r12, r2
         *
         * 2 запроса
         *
         * Загружаем такие же как и старые
         * W: 3
         * R: r3
         *
         * 0 запросов
         *
         * Загружаем
         * W: 3 (новую), 4 (как старую)
         * R: r3 (новую), 4 (как старую)
         *
         * 2 запроса
         *
         */

        saasPushAndCheckReqCount(Arrays.asList("1:{1}", "2:{2}", "3:{3}", "4:{4}"),
                Arrays.asList("r11:1", "r12:1", "r2:2", "r3:3", "r4:4"),
                SaasAction.MODIFY, 9, false);

        saasPushAndCheckReqCount(Arrays.asList("1"), Arrays.asList("r11", "r12"),
                SaasAction.DELETE, 3, false);

        saasPushAndCheckReqCount(Arrays.asList("1", "2"), Arrays.asList("r11", "r12", "r2"),
                SaasAction.DELETE, 2, false);

        saasPushAndCheckReqCount(Arrays.asList("3:{3}"), Arrays.asList("r3:3"),
                SaasAction.MODIFY, 0, false);

        saasPushAndCheckReqCount(Arrays.asList("3:{3new}", "4:{4}"), Arrays.asList("r3:3,4", "r4:4"),
                SaasAction.MODIFY, 2, false);
    }

    @SuppressWarnings("magicnumber")
    private void saasPushAndCheckReqCount(List<String> widgets, List<String> relations, SaasAction action,
                                          int count, boolean forcePush) throws IOException {

        List<IndexResult> container = new ArrayList<>();
        List<Future<IndexResult>> widgetList;
        List<Future<IndexResult>> relationList;
        long timestamp = System.currentTimeMillis();

        if (action == SaasAction.MODIFY) {
            widgetList = saasPushService.pushDataAsync(
                    timestamp, prepairPairs(widgets),
                    SaasIndexEntity.WIDGET, Stand.MAIN,
                    new IndexOperationContext(0, forcePush), () -> {
                    });

            relationList = saasPushService.pushDataAsync(
                    timestamp, prepairPairs(relations),
                    SaasIndexEntity.WIDGET, Stand.MAIN,
                    new IndexOperationContext(0, forcePush), () -> {
                    });
        } else {
            widgetList = saasPushService.removeDataAsync(
                    timestamp, widgets,
                    SaasIndexEntity.WIDGET, Stand.MAIN,
                    new IndexOperationContext(0, forcePush), () -> {
                    });

            relationList = saasPushService.removeDataAsync(
                    timestamp, relations,
                    SaasIndexEntity.WIDGET, Stand.MAIN,
                    new IndexOperationContext(0, forcePush), () -> {
                    });
        }

        container.addAll(unwrap(widgetList));
        container.addAll(unwrap(relationList));

        Mockito.verify(saasPushService.getHttpClient(), Mockito.times(count)).execute(Mockito.any());
        Mockito.clearInvocations(saasPushService.getHttpClient());
    }

    private List<IndexResult> unwrap(List<Future<IndexResult>> list) {
        return list.stream().map(o -> {
            try {
                return o.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private List<ExportKeyValue> prepairPairs(List<String> pairs) {
        return pairs.stream().map(o -> {
            String[] splits = o.split(":");
            return new ExportKeyValue(splits[0], splits[1]);
        }).collect(Collectors.toList());
    }
}
