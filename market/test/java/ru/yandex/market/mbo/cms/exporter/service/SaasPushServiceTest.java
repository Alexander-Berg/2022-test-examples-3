package ru.yandex.market.mbo.cms.exporter.service;


import java.io.File;
import java.util.Arrays;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.dao.SaasIndexEntity;
import ru.yandex.market.mbo.cms.core.dto.IndexKey;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.SaasAction;
import ru.yandex.market.mbo.cms.core.service.Stand;
import ru.yandex.market.mbo.cms.exporter.util.ExporterTestUtils;

public class SaasPushServiceTest {
    private SaasPushService saasPushService;

    private SaasIndexContentService saasIndexContentService;
    private SaasDataChecker saasDataChecker;
    private MetricsLogger metricsLogger;
    private HttpClient httpClient;

    @Before
    public void init() throws Exception {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        saasIndexContentService = Mockito.mock(SaasIndexContentService.class);
        saasDataChecker = Mockito.mock(SaasDataChecker.class);
        metricsLogger = Mockito.mock(MetricsLogger.class);
        httpClient = Mockito.mock(HttpClient.class);

        saasPushService = new SaasPushService(saasIndexContentService, saasDataChecker, metricsLogger);
        saasPushService = Mockito.spy(saasPushService);
        Mockito.doReturn(httpClient).when(saasPushService).getHttpClient();
        Mockito.doReturn(new File(classLoader.getResource("saas_push_service_test/pages.txt").getFile()))
                .when(saasPushService).getPagesFile();
        Mockito.doReturn(new File(classLoader.getResource("saas_push_service_test/relations.txt").getFile()))
                .when(saasPushService).getRelationsFile();
        Mockito.when(httpClient.execute(Mockito.any()))
                .then(o -> ExporterTestUtils.makeOkHttpResponse("{\"written\": true}"));
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void syncSaasWithExtractions() throws Exception {
        /*
         * В Саасе (и в БД статистики) уже лежат
         * W: 1, 2, 3
         * R: r11, r12, r2, r3
         *
         * В выгрузках
         * W: 1, 2, 4
         * R: r11, r2, r41, r42
         *
         * Значит должно быть:
         * 3 запроса на добавление/изменение данных W (1,2,4)
         * 4 запроса на добавление/изменение данных R (r11, r2, r41, r42)
         * 1 запрос на удаление (3) из W
         * 2 запроса на удаление (r12, r3) из R
         * Всего 10
         */

        Mockito.when(saasIndexContentService.getAllPresentedKeys(Stand.MAIN, SaasIndexEntity.WIDGET))
                .thenReturn(Arrays.asList("1", "2", "3"));
        Mockito.when(saasIndexContentService.getAllPresentedKeys(Stand.MAIN, SaasIndexEntity.RELATION))
                .thenReturn(Arrays.asList("r11", "r12", "r2", "r3"));
        Mockito.when(saasIndexContentService.checkAndRegisterAction(Mockito.anyString(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyLong(),
                Mockito.any(),
                Mockito.anyBoolean()))
                .thenReturn(true);


        saasPushService.syncSaasWithExtractions(Stand.MAIN, true, false);
        Mockito.verify(httpClient, Mockito.times(10)).execute(Mockito.any());
        verifyMakeRequest("1", SaasIndexEntity.WIDGET, SaasAction.MODIFY);
        verifyMakeRequest("2", SaasIndexEntity.WIDGET, SaasAction.MODIFY);
        verifyMakeRequest("4", SaasIndexEntity.WIDGET, SaasAction.MODIFY);

        verifyMakeRequest("r11", SaasIndexEntity.RELATION, SaasAction.MODIFY);
        verifyMakeRequest("r2", SaasIndexEntity.RELATION, SaasAction.MODIFY);
        verifyMakeRequest("r41", SaasIndexEntity.RELATION, SaasAction.MODIFY);
        verifyMakeRequest("r42", SaasIndexEntity.RELATION, SaasAction.MODIFY);

        verifyMakeRequest("3", SaasIndexEntity.WIDGET, SaasAction.DELETE);
        verifyMakeRequest("r3", SaasIndexEntity.RELATION, SaasAction.DELETE);
        verifyMakeRequest("r12", SaasIndexEntity.RELATION, SaasAction.DELETE);
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void syncSaasWithExtractionsSomeNotChanged() throws Exception {
        /*
         * В Саасе (и в БД статистики) уже лежат
         * W: 1, 2, 3
         * R: r11, r12, r2, r3
         *
         * В выгрузках
         * W: 1, 2, 4
         * R: r11, r2, r41, r42
         *
         * Притом, ключ r11 и 1 не изменились
         *
         * Значит должно быть:
         * 2 запроса на добавление/изменение данных W (2,4)
         * 3 запроса на добавление/изменение данных R (r2, r41, r42)
         * 1 запрос на удаление (3) из W
         * 2 запроса на удаление (r12, r3) из R
         * Всего 8
         */

        Mockito.when(saasIndexContentService.getAllPresentedKeys(Stand.MAIN, SaasIndexEntity.WIDGET))
                .thenReturn(Arrays.asList("1", "2", "3"));
        Mockito.when(saasIndexContentService.getAllPresentedKeys(Stand.MAIN, SaasIndexEntity.RELATION))
                .thenReturn(Arrays.asList("r11", "r12", "r2", "r3"));

        Mockito.when(saasIndexContentService.checkAndRegisterAction(
                ArgumentMatchers.argThat(o -> Arrays.asList("2", "3", "4", "r12", "r2", "r3", "r41", "r42")
                    .contains(o)),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyLong(),
                Mockito.any(),
                Mockito.anyBoolean()))
                .thenReturn(true);

        saasPushService.syncSaasWithExtractions(Stand.MAIN, true, false);
        verifyMakeRequest("2", SaasIndexEntity.WIDGET, SaasAction.MODIFY);
        verifyMakeRequest("4", SaasIndexEntity.WIDGET, SaasAction.MODIFY);

        verifyMakeRequest("r2", SaasIndexEntity.RELATION, SaasAction.MODIFY);
        verifyMakeRequest("r41", SaasIndexEntity.RELATION, SaasAction.MODIFY);
        verifyMakeRequest("r42", SaasIndexEntity.RELATION, SaasAction.MODIFY);

        verifyMakeRequest("3", SaasIndexEntity.WIDGET, SaasAction.DELETE);
        verifyMakeRequest("r3", SaasIndexEntity.RELATION, SaasAction.DELETE);
        verifyMakeRequest("r12", SaasIndexEntity.RELATION, SaasAction.DELETE);
        Mockito.verify(httpClient, Mockito.times(8)).execute(Mockito.any());
    }

    private void verifyMakeRequest(String key, SaasIndexEntity entity, SaasAction action) {
        Mockito.verify(saasPushService, Mockito.times(1))
                .makeRequest(Mockito.argThat(makeArgumentMatcher(key, entity, action)),
                        Mockito.any(),
                        Mockito.any());

    }

    private static ArgumentMatcher<IndexKey> makeArgumentMatcher(String key,
                                                                 SaasIndexEntity entity,
                                                                 SaasAction action) {
        return (indexKey) -> indexKey.getKey().equals(key) &&
                indexKey.getEntity().equals(entity) &&
                indexKey.getAction().equals(action);
    }
}
