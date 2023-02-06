package ru.yandex.common.services.maps.router;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.common.services.maps.router.model.RouterGpsCoordinate;
import ru.yandex.common.services.maps.router.model.RouterRequest;
import ru.yandex.common.services.maps.router.model.RouterResponse;

import static ru.yandex.common.services.maps.router.YandexMapsRouterTestUtils.checkCoordinates;
import static ru.yandex.common.services.maps.router.YandexMapsRouterTestUtils.checkSegments;

/**
 * Тесты для {@link MapsRouterService}.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class YandexMapsRouterServiceTest {

    /**
     * URI тестового окружения для Маршрутизатора от Яндекс Карт.
     */
    private static final String BASE_ROUTER_URL = "http://route.tst.maps.yandex.net/1.x/";

    private static final MapsRouterService ROUTER = createMapsRouterService();

    private static final RouterRequest[] REQUESTS = {
            new RouterRequest(
                    gps(54.858052, 83.110501), // Пункт отправления: Новосибирск, Николаева, 11
                    gps(55.040361, 82.900737) // Пункт назначения: Новосибирск, Красноярская 35
            ),
            new RouterRequest(
                    gps(55.753215, 37.622504), // Пункт отправления: Москва
                    gps(55.437102, 37.768004) // Пункт назначения: Домодедово
            )
    };


    private final RouterRequest request;


    public YandexMapsRouterServiceTest(final RouterRequest request) {
        this.request = request;
    }


    /**
     * Тест для отладки клиента, может не хватать доступов, поэтому {@link Ignore}.
     */
    @Ignore
    @Test
    public void testRoute() {
        final RouterResponse routerData = ROUTER.route(request)
                .orElseThrow(() -> new MapsRouterException("Data must be defined for " + request));

        checkCoordinates(routerData.getMetaData(), routerData.getPath());
        checkSegments(routerData.getSegments());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Stream.of(REQUESTS)
                .map(req -> new Object[] { req })
                .collect(Collectors.toList());
    }

    private static RouterGpsCoordinate gps(final double latitude, final double longitude) {
        return new RouterGpsCoordinate(latitude, longitude);
    }

    private static MapsRouterService createMapsRouterService() {
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        return new YandexMapsRouterService(httpClient, BASE_ROUTER_URL);
    }

}
