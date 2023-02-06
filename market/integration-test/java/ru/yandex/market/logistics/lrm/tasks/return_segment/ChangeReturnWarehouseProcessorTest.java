package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeReturnWarehousePayload;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeReturnWarehousePayload.ChangeReturnWarehouseRequest;
import ru.yandex.market.logistics.lrm.queue.processor.ChangeReturnWarehouseProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Изменение возвратного склада")
@ParametersAreNonnullByDefault
class ChangeReturnWarehouseProcessorTest extends AbstractIntegrationTest {

    private static final long NON_EXISTING_LOGISTIC_POINT_ID = 1999L;
    private static final long LOGISTIC_POINT_ID = 13L;
    private static final long NEW_LOGISTIC_POINT_ID = 999L;
    private static final long PARTNER_ID = 75735L;
    private static final long NEW_PARTNER_ID = 666L;

    @Autowired
    private ChangeReturnWarehouseProcessor processor;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private UuidGenerator uuidGenerator;
    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-01-01T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        mockLms(PartnerType.SORTING_CENTER, PointType.WAREHOUSE);
        mockUUID();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Пустой список запросов")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noRequests() {
        execute(List.of());
    }

    @Test
    @DisplayName("Возврата не существует")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/return_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnNotFound() {
        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Возвратного склада не существует")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/logistics_point_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void logisticPointNotFound() {
        execute(List.of(createRequest(1L, NON_EXISTING_LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(NON_EXISTING_LOGISTIC_POINT_ID));
    }

    @Test
    @DisplayName("Партнера возвратного склада не существует")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/partner_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void partnerNotFound() {
        when(lmsClient.searchPartners(getSearchPartnerFilter(PARTNER_ID)))
            .thenReturn(List.of());

        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Многокоробочный возврат был готов к выдаче магазину")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleBoxReadyForIM() {
        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Дублирующиеся запросы запрещены")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/duplicated_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void duplicatedRequests() {
        execute(List.of(
            createRequest(1L, LOGISTIC_POINT_ID),
            createRequest(1L, LOGISTIC_POINT_ID)
        ));
    }

    @Test
    @DisplayName("Неоднозначные запросы запрещены")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/ambiguous_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void ambiguousRequests() {
        execute(List.of(
            createRequest(1L, LOGISTIC_POINT_ID),
            createRequest(1L, NON_EXISTING_LOGISTIC_POINT_ID)
        ));
    }

    @Test
    @DisplayName("Два возврата, один неуспешный")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/not_last_mile.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/one_ready_one_not_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/success_and_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAndFailRequests() {
        execute(List.of(
            createRequest(1L, LOGISTIC_POINT_ID),
            createRequest(2L, LOGISTIC_POINT_ID)
        ));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Текущий сегмент совпадает с заданным складом")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/same_point.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/same_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/same_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void samePointDifferentShipment() {
        execute(List.of(createRequest(3L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Отгрузка текущего сегмента совпадает с заданным складом")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/same_shipment.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/same_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void sameShipment() {
        execute(List.of(createRequest(3L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @EnumSource(value = PartnerType.class, names = {"SORTING_CENTER", "DELIVERY"}, mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Неправильный тип логистической точки")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/wrong_partner_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/skipped_multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void logisticPointHasWrongType(PartnerType partnerType) {
        mockLms(partnerType, PointType.WAREHOUSE);

        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Возвратный склад имеет тип Дропофф")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/multibox_ready_for_im.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/multibox_to_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/multibox_ready_for_im.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void warehouseIsDropoff() {
        mockLms(PartnerType.DELIVERY, PointType.PICKUP_POINT);

        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Ошибка изменения: грузоместа на разных сегментах")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/two_boxes_pvz_sc_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/history/different_actual_segments.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/different_actual_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void differentActualSegments() {
        mockLms(PartnerType.DELIVERY, PointType.PICKUP_POINT);

        execute(List.of(createRequest(1L, LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(PARTNER_ID));
    }

    @Test
    @DisplayName("Успех: еще не готов к отгрузке, но коробки на одном сегменте")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/two_boxes_pvz_sc_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/history/not_ready_for_shop.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/success_from_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNotReadyForShop() {
        mockLms(PartnerType.SORTING_CENTER, PointType.WAREHOUSE, NEW_PARTNER_ID, NEW_LOGISTIC_POINT_ID);

        execute(List.of(createRequest(1L, NEW_LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(NEW_LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(NEW_PARTNER_ID));
    }

    @Test
    @DisplayName("Успех: коробки еще не приехали в точку назначения, но находятся в одном сегменте")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/two_boxes_pvz_sc_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/history/destination_not_reached.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/destination_not_reached.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDestinationPointHasNoBeenReachedYet() {
        mockLms(PartnerType.SORTING_CENTER, PointType.WAREHOUSE, NEW_PARTNER_ID, NEW_LOGISTIC_POINT_ID);

        execute(List.of(createRequest(1L, NEW_LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(NEW_LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(NEW_PARTNER_ID));
    }

    @Test
    @DisplayName("Ошибка: сегмент последней мили еще не создан")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/two_boxes_pvz_sc_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/history/destination_not_reached.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/delete_last_mile_destination.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/without_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failWithoutLastMile() {
        mockLms(PartnerType.SORTING_CENTER, PointType.WAREHOUSE, NEW_PARTNER_ID, NEW_LOGISTIC_POINT_ID);

        execute(List.of(createRequest(1L, NEW_LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(NEW_LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(NEW_PARTNER_ID));
    }

    @Test
    @DisplayName("Успех: возврат ошибочно числится выданным")
    @DatabaseSetup("/database/tasks/return-segment/change-return-warehouse/before/two_boxes_pvz_sc_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-warehouse/before/history/wrongly_delivered.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/success_from_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-warehouse/after/history/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successChangeForDelivered() {
        mockLms(PartnerType.SORTING_CENTER, PointType.WAREHOUSE, NEW_PARTNER_ID, NEW_LOGISTIC_POINT_ID);

        execute(List.of(createRequest(1L, NEW_LOGISTIC_POINT_ID)));

        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter(NEW_LOGISTIC_POINT_ID));
        verify(lmsClient).searchPartners(getSearchPartnerFilter(NEW_PARTNER_ID));
    }

    @Nonnull
    private ChangeReturnWarehouseRequest createRequest(Long returnId, Long logisticPointId) {
        return ChangeReturnWarehouseRequest.builder()
            .returnId(returnId)
            .logisticPointId(logisticPointId)
            .build();
    }

    private void execute(List<ChangeReturnWarehouseRequest> requests) {
        processor.execute(
            ChangeReturnWarehousePayload.builder()
                .changeRequests(requests)
                .build()
        );
    }

    private void mockLms(PartnerType partnerType, PointType pointType) {
        LogisticsPointResponse logisticPoint = LogisticsPointResponse.newBuilder()
            .id(LOGISTIC_POINT_ID)
            .partnerId(PARTNER_ID)
            .externalId("logistics-point-external-id-13")
            .name("sc-13")
            .type(pointType)
            .build();
        when(lmsClient.getLogisticsPoints(getLogisticsPointFilter(LOGISTIC_POINT_ID)))
            .thenReturn(List.of(logisticPoint));

        PartnerResponse partner = PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .name("partner name")
            .partnerType(partnerType)
            .build();
        when(lmsClient.searchPartners(getSearchPartnerFilter(PARTNER_ID)))
            .thenReturn(List.of(partner));
    }

    private void mockLms(PartnerType partnerType, PointType pointType, long partnerId, long logisticPointId) {
        LogisticsPointResponse logisticPoint = LogisticsPointResponse.newBuilder()
            .id(logisticPointId)
            .partnerId(partnerId)
            .externalId("logistics-point-external-id-" + logisticPointId)
            .name("%s-%d".formatted(pointType, logisticPointId))
            .type(pointType)
            .build();
        when(lmsClient.getLogisticsPoints(getLogisticsPointFilter(logisticPointId)))
            .thenReturn(List.of(logisticPoint));

        PartnerResponse partner = PartnerResponse.newBuilder()
            .id(partnerId)
            .name("%s-%d".formatted(partnerType, logisticPointId))
            .partnerType(partnerType)
            .build();
        when(lmsClient.searchPartners(getSearchPartnerFilter(partnerId)))
            .thenReturn(List.of(partner));
    }

    @Nonnull
    private LogisticsPointFilter getLogisticsPointFilter(Long logisticPointId) {
        return LogisticsPointFilter.newBuilder()
            .ids(Set.of(logisticPointId))
            .build();
    }

    @Nonnull
    private SearchPartnerFilter getSearchPartnerFilter(Long id) {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(id))
            .build();
    }

    private void mockUUID() {
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa091"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa092")
        );
    }
}
