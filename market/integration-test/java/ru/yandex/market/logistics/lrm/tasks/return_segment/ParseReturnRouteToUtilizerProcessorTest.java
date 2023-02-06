package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.exception.ModelResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.payload.ParseRouteToUtilizerPayload;
import ru.yandex.market.logistics.lrm.queue.processor.ParseReturnRouteToUtilizerProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@ParametersAreNonnullByDefault
@DisplayName("Парсинг маршрута в сторону Утилизатора")
@DatabaseSetup("/database/tasks/return-segment/parse-route-to-utilizer/before/minimal.xml")
class ParseReturnRouteToUtilizerProcessorTest extends AbstractIntegrationYdbTest {
    private static final Long PARTNER_ALLOWED_TO_USE_RETURN_SC = 245087L;
    private static final String ROUTE_UUID = "e133583b-fc9e-4bbe-8285-14ea54307d74";
    private static final ParseRouteToUtilizerPayload PAYLOAD = ParseRouteToUtilizerPayload.builder()
        .returnId(1L)
        .returnSegmentIds(List.of(1L, 2L))
        .routeUuid(UUID.fromString(ROUTE_UUID))
        .build();

    @Autowired
    private ReturnRouteHistoryTableDescription route;
    @Autowired
    private ParseReturnRouteToUtilizerProcessor processor;
    @Autowired
    private ReturnRouteHistoryConverter converter;
    @Autowired
    private LMSClient lmsClient;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(route);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-12-11T10:09:08.00Z"), MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Маршрут не найден")
    void routeNotFound() {
        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .isInstanceOf(ModelResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN_ROUTE with id e133583b-fc9e-4bbe-8285-14ea54307d74");
    }

    @Test
    @DisplayName("Было SHORT_TERM хранение. В маршруте есть московский СЦ. Ставим контрольную точку на ВСЦ")
    @DatabaseSetup("/database/tasks/return-segment/parse-route-to-utilizer/before/short_term.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route-to-utilizer/after/long_term_with_special_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wasShortTermStorage_controlPointOnReturnSc() {
        mockRoute("route/from_sc_through_special_sc_to_utilizer.json");
        mockGetNextLogisticPoint(300L, 245087L);

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Было SHORT_TERM хранение. В маршруте есть региональный СЦ. Ставим контрольную точку на СЦ")
    @DatabaseSetup("/database/tasks/return-segment/parse-route-to-utilizer/before/short_term.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route-to-utilizer/after/long_term_with_regional_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wasShortTermStorage_controlPointOnRegionalSc() {
        mockRoute("route/from_sc_through_regional_sc_to_utilizer.json");
        mockGetNextLogisticPoint(300L, 3L);

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Было LONG_TERM хранение на СЦ. Везём через ВСЦ, но ставим контрольную точку на Утилизаторе")
    @DatabaseSetup("/database/tasks/return-segment/parse-route-to-utilizer/before/long_term.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route-to-utilizer/after/utilization.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void wasLongTermStorage() {
        mockRoute("route/from_sc_to_utilizer.json");
        mockGetNextLogisticPoint(5500L, 550L);
        mockGetUtilizerLogisticPoint();

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Было LONG_TERM хранение на ВСЦ. Пересортировываем на Утилизатор")
    @DatabaseSetup("/database/tasks/return-segment/parse-route-to-utilizer/before/long_term_on_return_sc.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route-to-utilizer/after/utilization_after_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void changeShipmentToUtilizer() {
        mockRoute("route/from_return_sc_to_utilizer.json");
        mockGetNextLogisticPoint(5590L, 559L);
        mockGetUtilizerLogisticPoint();

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    // FIXME поменять на генерацию из ДТО комбинатора
    private void mockRoute(String routeFile) {
        ydbInsert(
            route,
            List.of(new ReturnRouteHistoryTableDescription.ReturnRouteHistory(
                ROUTE_UUID,
                jsonFile(routeFile),
                Instant.parse("2021-12-11T10:09:08.00Z")
            )),
            converter::convert
        );
    }

    private void mockGetNextLogisticPoint(Long id, Long partnerId) {
        when(lmsClient.getLogisticsPoint(id))
            .thenReturn(
                Optional.of(
                    LogisticsPointResponse.newBuilder()
                        .id(id)
                        .externalId("external-" + id)
                        .name("SC-" + id)
                        .partnerId(partnerId)
                        .type(PointType.WAREHOUSE)
                        .build()
                )
            );
    }

    private void mockGetUtilizerLogisticPoint() {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(559L))
                .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        ))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .id(5590L)
                    .externalId("external-5590")
                    .name("Утилизатор")
                    .build()
            ));
    }
}
