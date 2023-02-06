package ru.yandex.market.fmcg.bff.test;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ru.yandex.market.fmcg.bff.connector.BlackboxConnector;
import ru.yandex.market.fmcg.bff.pers.GradeService;
import ru.yandex.market.fmcg.bff.region.GeobaseRegionService;
import ru.yandex.market.fmcg.bff.region.model.Coords;
import ru.yandex.market.fmcg.bff.region.model.GeobaseRegion;
import ru.yandex.market.fmcg.bff.suggestion.SuggestionService;
import ru.yandex.market.fmcg.client.backend.FmcgBackClient;
import ru.yandex.market.fmcg.core.search.index.IndexService;
import ru.yandex.market.pers.notify.PersNotifyClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author semin-serg
 */
public class FmcgBffMockFactory {

    private static final ConcurrentHashMap<Object, Runnable> MOCK_INITIALIZERS = new ConcurrentHashMap<>();

    public static void resetMocks() {
        for (Map.Entry<Object, Runnable> entry : MOCK_INITIALIZERS.entrySet()) {
            reset(entry.getKey());
            entry.getValue().run();
        }
    }

    public static RestTemplate restTemplate() {
        RestTemplate mock = mock(RestTemplate.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    public static GradeService gradeService() {
        GradeService mock = mock(GradeService.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(GradeService gradeService) {
    }

    public static FmcgBackClient fmcgBackClient() {
        FmcgBackClient mock = mock(FmcgBackClient.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(FmcgBackClient fmcgBackClient) {
    }

    public static RestTemplate geoexportRestTemplate() {
        RestTemplate mock = mock(RestTemplate.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(RestTemplate restTemplate) {
    }

    public static GeobaseRegionService geobaseRegionService() {
        GeobaseRegionService mock = mock(GeobaseRegionService.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(GeobaseRegionService geobaseRegionService) {
        GeobaseRegion msk = new GeobaseRegion(
            213, "Москва", new Coords(50d, 37d), "Europe/Moscow", 11
        );
        when(geobaseRegionService.getFmcgRegion(anyInt())).thenReturn(Optional.of(msk));
        when(geobaseRegionService.getFmcgRegion(any(Coords.class))).thenReturn(Optional.of(msk));
    }

    public static BlackboxConnector authenticationService() {
        BlackboxConnector mock = mock(BlackboxConnector.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(BlackboxConnector enticationService) {
    }

    public static ObjectPostProcessor<?> objectPostProcessor() {
        ObjectPostProcessor<?> mock = mock(ObjectPostProcessor.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(ObjectPostProcessor<?> mock) {
    }

    public static AuthenticationConfiguration authenticationConfiguration() {
        AuthenticationConfiguration mock = mock(AuthenticationConfiguration.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(AuthenticationConfiguration mock) {
    }

    public static IndexService indexService() {
        IndexService mock = mock(IndexService.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(IndexService mock) {
    }

    public static SuggestionService suggestionService() {
        SuggestionService mock = mock(SuggestionService.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(SuggestionService mock) {
    }

    public static PersNotifyClient persNotifyClient() {
        PersNotifyClient mock = mock(PersNotifyClient.class);
        init(mock);
        MOCK_INITIALIZERS.put(mock, () -> init(mock));
        return mock;
    }

    private static void init(PersNotifyClient mock) {
        when(mock.registerMobileAppInfo(any())).thenReturn(true);
        when(mock.unregisterMobileAppInfo(any(), any(), any())).thenReturn(true);
    }
}
