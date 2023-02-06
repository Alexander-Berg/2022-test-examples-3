package ru.yandex.market.tpl.core.domain.routing;

import java.time.LocalDate;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.market.tpl.core.config.TplAsyncConfiguration;
import ru.yandex.market.tpl.core.config.external.VrpRoutingConfiguration;
import ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.vrp.apiUrl=https://courier.yandex.ru/vrs/api/v1/",
        "external.vrp.apiKey=SECRET"
})
@ContextConfiguration(classes = {
        VrpRoutingConfiguration.class,
        TplAsyncConfiguration.class
})
@Disabled("integrationTest")
@ActiveProfiles(TplProfiles.TESTS)
class VrpRoutingApiIntegrationTest {

    @Autowired
    private TplRoutingManager routingManager;

    private final RoutingApiDataHelper helper = new RoutingApiDataHelper();

    @Test
    void shouldAddAndProcessTask() throws Exception {
        RoutingRequest routingRequest = helper.getRoutingRequest(123L, LocalDate.now(), 0);

        RoutingResult result = routingManager.performRouting(routingRequest, false);

        assertThat(result).isNotNull();
        printResult(routingRequest, result);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 7})
    void shouldAddAndReroute(int visitedNum) throws Exception {
        RoutingRequest routingRequest = helper.getRoutingRequest(123L, LocalDate.now(), visitedNum);

        RoutingResult result = routingManager.performRouting(routingRequest, true);

        assertThat(result).isNotNull();
        printResult(routingRequest, result);
    }

    @Test
    void shouldExcludeLyvaTolstogo() throws Exception {
        RoutingRequest routingRequest = helper.getRoutingRequest(123L, LocalDate.now(), 0);
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        routingRequest = routingRequest.withItems(
                StreamEx.of(routingRequest.getItems())
                        .append(new RoutingRequestItem(RoutingRequestItemType.CLIENT,
                                "9282", 1, "1",
                                new RoutingAddress("Москва, ул. Льва толстого 16",
                                        RoutingGeoPoint.ofLatLon(geoPoint.getLatitude(), geoPoint.getLongitude())),
                                RelativeTimeInterval.valueOf("12:00-18:00"),
                                "yandex, yo", Set.of(), Set.of(), null, false,
                                DimensionsClass.REGULAR_CARGO, 123,
                                MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY,
                                MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT,
                                false,
                                false
                                )
                        )
                        .toList()
        );

        RoutingResult result = routingManager.performRouting(routingRequest, false);

        assertThat(result).isNotNull();
        printResult(routingRequest, result);
    }

    private void printResult(RoutingRequest request, RoutingResult result) throws Exception {
        System.out.println(StringUtils.center("RESULT", 30, '='));
        System.out.println(StringUtils.center("SUMMARY", 30, '-'));
        System.out.println("itemsCnt=" + request.getItems().size());
        RoutingResultShift shift = result.getShiftsByUserId().values().iterator().next();
        System.out.println("order: " + StreamEx.of(shift.getRoutePoints()).flatMap(r -> r.getItems().stream()).flatMap(r -> r.getSubTaskIds().stream()).joining(", "));
        System.out.println("dropped: " + StreamEx.ofKeys(result.getDroppedItems()).joining(", "));
        System.out.println(StringUtils.repeat('=', 30));
    }

}
