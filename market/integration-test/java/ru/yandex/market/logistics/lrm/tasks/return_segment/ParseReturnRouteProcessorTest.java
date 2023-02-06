package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.config.LocalsConfiguration;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.ParseReturnRouteProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription.ReturnRouteHistory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@ParametersAreNonnullByDefault
@DatabaseSetup("/database/tasks/return-segment/parse-route/before/minimal.xml")
class ParseReturnRouteProcessorTest extends AbstractIntegrationYdbTest {

    private static final Long DROPSHIP_PARTNER_ID = 111L;
    private static final Long SC_PARTNER_ID = 220L;
    private static final Long FULFILLMENT_PARTNER_ID = 333L;
    private static final Long DROPSHIP_WAREHOUSE_ID = 1234567L;
    private static final String ROUTE_UUID = "e133583b-fc9e-4bbe-8285-14ea54307d74";
    private static final ReturnSegmentIdPayload PAYLOAD = ReturnSegmentIdPayload.builder()
        .returnSegmentId(1000L)
        .build();

    @Autowired
    private ReturnRouteHistoryTableDescription route;
    @Autowired
    private ReturnRouteHistoryConverter converter;
    @Autowired
    private ParseReturnRouteProcessor processor;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private LomClient lomClient;
    @Autowired
    private UuidGenerator uuidGenerator;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(route);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-12-11T10:09:08.00Z"), MOSCOW_ZONE);
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString(LocalsConfiguration.TEST_UUID),
            UUID.fromString(LocalsConfiguration.TEST_UUID2)
        );
    }

    @Test
    @DisplayName("Маршрут от СЦ")
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/database/tasks/return-segment/parse-route/before/ff_return.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/sorting_center.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sortingCenter() {
        mockRoute("route/sorting_center.json");
        mockLogisticPoint(Set.of(2200L, 2400L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут от СЦ с BACKWARD_WAREHOUSE сегментами")
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/database/tasks/return-segment/parse-route/before/ff_return.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/sorting_center.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void backwardWarehouse() {
        mockRoute("route/sorting_center_bwh.json");
        mockLogisticPoint(Set.of(2200L, 2400L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут от ПВЗ")
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/database/tasks/return-segment/parse-route/before/shop_return.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void pickupPoint() {
        mockRoute("route/pickup_point.json");
        mockLogisticPoint(Set.of(2200L, 2400L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут в Дропофф")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/no_segment_status_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropoff() {
        mockRoute("route/dropoff.json");
        mockLogisticPoint(Set.of(2200L, 2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Следующий сегмент - Утилизатор")
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/database/tasks/return-segment/parse-route/before/return_sc_current.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/utilizer_next.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void utilizerIsNext() {
        mockRoute("route/from_return_sc_to_utilizer.json");
        mockLogisticPoint(Set.of(5590L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут в финальную точку")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/final_destination_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void finalDestinationPoint() {
        mockRoute("route/final_destination_point.json");
        mockLogisticPoint(Set.of(2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут от правильного сегмента с другой логистической точкой: дропшип заборка")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/logistic_segment_id.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/two_segments_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void differentLogisticPointSameSegment_DropshipWithdraw() {
        mockRoute("route/different_logistic_point.json");
        mockLogisticPoint(2200L, 220L);
        mockGetOrder(createDropshipWithdrawOrder());

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут от правильного сегмента с другой логистической точкой: дропшип самопривоз")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/logistic_segment_id.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/two_segments_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void differentLogisticPointSameSegment_DropshipImport() {
        mockRoute("route/different_logistic_point.json");
        mockLogisticPoint(2200L, 220L);
        mockGetOrder(createDropshipImportOrder());

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут от правильного сегмента с другой логистической точкой: Фулфилмент")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/logistic_segment_id.xml",
        type = DatabaseOperation.UPDATE
    )
    void differentLogisticPointSameSegment_Fulfillment() {
        mockRoute("route/different_logistic_point.json");
        mockLogisticPoint(2200L, 220L);
        mockGetOrder(createFulfillmentOrder());

        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .hasMessage("Partner type is FULFILLMENT, not DROPSHIP, don't handle");
    }

    @Test
    @DisplayName("Контрольная точка перед мерчем, создаём сегмент SHOP")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/control_point_before_shop.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/two_segments_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void controlPointRightBeforeShop() {
        mockRoute("route/different_logistic_point.json");
        mockLogisticPoint(2200L, 220L);
        mockGetPartner(111L);
        mockGetWarehouseForDropship(111L, 1234567L);

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Маршрут не найден")
    void noRoute() {
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .hasMessage("Failed to find RETURN_ROUTE with id e133583b-fc9e-4bbe-8285-14ea54307d74");
    }

    @Test
    @DisplayName("Лог. точки не найдены")
    void noLogisticsPoint() {
        mockRoute("route/sorting_center.json");
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .hasMessage("Failed to find logistics points [2200, 2400]");
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Валидация маршрута")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/logistic_segment_id.xml",
        type = DatabaseOperation.UPDATE
    )
    void routeValidation(String routePath, String message) {
        mockRoute(routePath);

        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .hasMessageStartingWith(message);
    }

    @Nonnull
    static Stream<Arguments> routeValidation() {
        return Stream.of(
            Arguments.of("route/invalid/size.json", "Route must have at least 3 points"),
            Arguments.of(
                "route/invalid/segment_type.json",
                "Expected segment 3100 to have type BACKWARD_MOVEMENT but was MOVEMENT"
            ),
            Arguments.of(
                "route/invalid/next_warehouse_shipment_warehouse_segment_type.json",
                "Expected segment 3300 to have type BACKWARD_MOVEMENT but was MOVEMENT"
            ),
            Arguments.of(
                "route/invalid/logistic_point.json",
                "Route must start either from segment 3000 or logistic point 2000"
            ),
            Arguments.of("route/invalid/partner_type.json", "Invalid partner type LINEHAUL on segment 3200"),
            Arguments.of(
                "route/invalid/next_warehouse_shipment_warehouse_partner_type.json",
                "Invalid partner type LINEHAUL on segment 3400"
            )
        );
    }

    @Test
    @DisplayName("Валидация маршрута для сегмента возврата без id логистического сегмента")
    void routeValidationNoSegmentId() {
        mockRoute("route/invalid/logistic_point.json");

        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .hasMessageStartingWith("Route must start from logistic point 2000");
    }

    @Test
    @DisplayName("Сохраняем плановые даты статусов сегмента")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/segment_status_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveReturnSegmentStatusPlan() {
        mockRoute("route/dropoff_with_plan.json");
        mockLogisticPoint(Set.of(2200L, 2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Неизвестный код сервиса не приводит к исключению")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/segment_status_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unknownServiceCode() {
        mockRoute("route/dropoff_with_plan_unknown_code.json");
        mockLogisticPoint(Set.of(2200L, 2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Сохраняем плановую дату возврата коробки")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/new_box_planned_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void saveReturnPlannedDateForBox(String displayName, String routePath) {
        mockRoute(routePath);
        mockLogisticPoint(Set.of(2200L, 2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    static Stream<Arguments> saveReturnPlannedDateForBox() {
        return Stream.of(
            Arguments.of("Last segment is SHOP", "route/dropoff_with_plan.json"),
            Arguments.of("Last segment is FULFILLMENT", "route/sorting_center_with_plan.json"),
            Arguments.of("Last segment is control point", "route/control_point_with_plan.json")
        );
    }

    @Test
    @DisplayName("Если плановая дата возврата коробки сохранена, то не меняем дату")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/parse-route/before/box_planned_date.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/existing_box_planned_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notSavePlannedDateForBox() {
        mockRoute("route/dropoff_with_plan.json");
        mockLogisticPoint(Set.of(2200L, 2300L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Разбор маршрута для сегмента, у которого уже есть следующий сегмент")
    @DatabaseSetup("/database/tasks/return-segment/parse-route/before/existing_next_segment.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/parse-route/after/existing_next_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void existingNextSegment() {
        mockRoute("route/from_existing_segment.json");
        mockLogisticPoint(Set.of(2200L, 2400L));

        softly.assertThatCode(() -> processor.execute(PAYLOAD))
            .doesNotThrowAnyException();
    }

    // FIXME поменять на генерацию из ДТО комбинатора
    private void mockRoute(String routeFile) {
        ydbInsert(
            route,
            List.of(new ReturnRouteHistory(
                ROUTE_UUID,
                jsonFile(routeFile),
                Instant.parse("2021-12-11T10:09:08.00Z")
            )),
            converter::convert
        );
    }

    private void mockGetPartner(long partnerId) {
        when(lmsClient.getPartner(partnerId))
            .thenReturn(Optional.of(
                PartnerResponse.newBuilder()
                    .id(partnerId)
                    .name("partner-" + partnerId)
                    .build()
            ));
    }

    private void mockGetWarehouseForDropship(long partnerId, long logisticPointId) {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .active(true)
                .partnerIds(Set.of(partnerId))
                .partnerTypes(Set.of(ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP))
                .type(PointType.WAREHOUSE)
                .build()
        )).thenReturn(
            List.of(
                LogisticsPointResponse.newBuilder()
                    .id(logisticPointId)
                    .partnerId(partnerId)
                    .externalId("external-" + logisticPointId)
                    .name("partner-" + partnerId)
                    .build()
            )
        );
    }

    private void mockLogisticPoint(Long id, Long partnerId) {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(id))
                .build()
        ))
            .thenReturn(List.of(
                    LogisticsPointResponse.newBuilder()
                        .id(id)
                        .externalId("external-" + id)
                        .partnerId(partnerId)
                        .name("point-" + id)
                        .build()
                )
            );
    }

    private void mockLogisticPoint(Set<Long> ids) {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(ids)
                .build()
        ))
            .thenReturn(ids.stream()
                .map(id ->
                    LogisticsPointResponse.newBuilder()
                        .id(id)
                        .externalId("external-" + id)
                        .name("point-" + id)
                        .build()
                )
                .toList()
            );
    }

    private void mockGetOrder(OrderDto order) {
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .barcodes(Set.of("order-external-id"))
            .build();

        when(lomClient.searchOrders(filter, Pageable.unpaged()))
            .thenReturn(
                PageResult.of(
                    List.of(order),
                    1,
                    0,
                    1
                )
            );
    }

    @Nonnull
    private OrderDto createDropshipWithdrawOrder() {
        return new OrderDto()
            .setWaybill(
                List.of(
                    createWaybill(DROPSHIP_PARTNER_ID, PartnerType.DROPSHIP, null),
                    createWaybill(SC_PARTNER_ID, PartnerType.SORTING_CENTER, ShipmentType.WITHDRAW)
                )
            );
    }

    @Nonnull
    private OrderDto createDropshipImportOrder() {
        return new OrderDto()
            .setWaybill(
                List.of(
                    createWaybill(DROPSHIP_PARTNER_ID, PartnerType.DROPSHIP, null),
                    createWaybill(SC_PARTNER_ID, PartnerType.SORTING_CENTER, ShipmentType.IMPORT)
                )
            );
    }

    @Nonnull
    private OrderDto createFulfillmentOrder() {
        return new OrderDto()
            .setWaybill(
                List.of(
                    createWaybill(FULFILLMENT_PARTNER_ID, PartnerType.FULFILLMENT, null),
                    createWaybill(SC_PARTNER_ID, PartnerType.SORTING_CENTER, ShipmentType.IMPORT)
                )
            );
    }

    @Nonnull
    private WaybillSegmentDto createWaybill(
        Long partnerId,
        PartnerType partnerType,
        @Nullable ShipmentType shipmentType
    ) {
        return WaybillSegmentDto.builder()
            .partnerId(partnerId)
            .partnerType(partnerType)
            .partnerName("partner-" + partnerId)
            .shipment(
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(shipmentType)
                    .locationFrom(
                        LocationDto.builder()
                            .warehouseId(DROPSHIP_WAREHOUSE_ID)
                            .warehouseExternalId("external-1234567")
                            .build()
                    )
                    .build()
            )
            .build();
    }
}
