package ru.yandex.market.logistics.lrm.tasks.control_point;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.CreateFirstControlPointPayload;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnCreateStartSegmentsByBoxesPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateFirstControlPointProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.ReturnPointInfoResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Создание первой контрольной точки")
@DatabaseSetup("/database/tasks/control_point/create/before/prepare.xml")
class CreateFirstControlPointProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_ID = 1L;
    private static final String ORDER_EXTERNAL_ID = "order-external-id";
    private static final long SHOP_PARTNER_ID = 50L;
    private static final long SHOP_LOGISTIC_POINT_ID = 500L;

    private static final Instant NOW_TIME = Instant.parse("2021-11-11T11:11:11.00Z");

    @Autowired
    private CreateFirstControlPointProcessor processor;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Ошибка: возврат не найден")
    @ExpectedDatabase(
        value = "/database/tasks/control_point/create/after/no_control_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnNotFound() {
        softly.assertThatThrownBy(() -> execute(404))
            .hasMessage("Failed to find RETURN with ids [404]");
    }

    @Test
    @DisplayName("Точка выдачи не найдена, заказ не найден")
    @ExpectedDatabase(
        value = "/database/tasks/control_point/create/after/no_control_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnPointNotFound_OrderNotFound() throws Exception {
        try (
            var ignored1 = mockEmptyReturnPointForPartner();
            var ignored2 = mockEmptyGetOrder()
        ) {
            softly.assertThatCode(this::execute)
                .hasMessage("Order with barcode order-external-id not found")
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("Точка выдачи не найдена, заказ найден")
    @ExpectedDatabase(
        value = "/database/tasks/control_point/create/after/control_point_from_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnPointNotFound_OrderFound() throws Exception {
        try (
            var ignored1 = mockEmptyReturnPointForPartner();
            var ignored2 = mockGetOrder()
        ) {
            execute();

            softly.assertThat(backLogCaptor.getResults().toString())
                .contains(
                    """
                        level=WARN\t\
                        format=plain\t\
                        code=CREATE_FIRST_CONTROL_POINT\t\
                        payload=Create first control point by order waybill\t\
                        request_id=test-request-id\t\
                        extra_keys=returnId\t\
                        extra_values=1
                        """
                );
        }
    }

    @Test
    @DisplayName("Успех: самопривоз")
    @ExpectedDatabase(
        value = "/database/tasks/control_point/create/after/control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() throws Exception {
        try (
            var ignored1 = mockReturnPointForPartner(true)
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Успех: заборка")
    @ExpectedDatabase(
        value = "/database/tasks/control_point/create/after/control_point_withdraw.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithdraw() throws Exception {
        try (
            var ignored1 = mockReturnPointForPartner(false);
            var ignored2 = mockWarehouseForPartner()
        ) {
            execute();
        }
    }

    private void execute() {
        execute(RETURN_ID);
    }

    private void execute(long returnId) {
        processor.execute(
            CreateFirstControlPointPayload.builder()
                .returnId(returnId)
                .sortingCenters(
                    List.of(
                        ReturnCreateStartSegmentsByBoxesPayload.WaybillSegmentPayload.builder()
                            .logisticsPointId(100L)
                            .partnerId(10L)
                            .build()
                    )
                )
                .build()
        );
    }

    @Nonnull
    private AutoCloseable mockReturnPointForPartner(boolean deliveryByShop) {
        ReturnPointInfoResponse dropoff = new ReturnPointInfoResponse(
            40L,
            400L,
            true,
            deliveryByShop ? SHOP_PARTNER_ID : null,
            deliveryByShop ? "shop" : null
        );
        when(lmsClient.getReturnPointForPartner(SHOP_PARTNER_ID)).thenReturn(Optional.of(dropoff));
        return () -> verify(lmsClient).getReturnPointForPartner(SHOP_PARTNER_ID);
    }

    @Nonnull
    private AutoCloseable mockEmptyReturnPointForPartner() {
        when(lmsClient.getReturnPointForPartner(SHOP_PARTNER_ID)).thenReturn(Optional.empty());
        return () -> verify(lmsClient).getReturnPointForPartner(SHOP_PARTNER_ID);
    }

    @Nonnull
    private AutoCloseable mockEmptyGetOrder() {
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .barcodes(Set.of(ORDER_EXTERNAL_ID))
            .build();
        when(lomClient.searchOrders(filter, Pageable.unpaged())).thenReturn(PageResult.empty(Pageable.unpaged()));
        return () -> verify(lomClient).searchOrders(filter, Pageable.unpaged());
    }

    @Nonnull
    private AutoCloseable mockGetOrder() {
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .barcodes(Set.of(ORDER_EXTERNAL_ID))
            .build();
        when(lomClient.searchOrders(filter, Pageable.unpaged())).thenReturn(
            PageResult.of(
                List.of(
                    new OrderDto()
                        .setReturnSortingCenterId(12345L)
                        .setBarcode(ORDER_EXTERNAL_ID)
                        .setWaybill(List.of(
                            WaybillSegmentDto.builder()
                                .partnerId(1L)
                                .partnerType(PartnerType.DROPSHIP)
                                    .build(),
                            WaybillSegmentDto.builder()
                                .partnerId(12345L)
                                .partnerType(PartnerType.DELIVERY)
                                .segmentType(SegmentType.SORTING_CENTER)
                                .warehouseLocation(
                                    LocationDto.builder()
                                        .warehouseId(111L)
                                        .build()
                                )
                                .build()
                        ))
                ),
                1,
                0,
                1
            )
        );
        return () -> verify(lomClient).searchOrders(filter, Pageable.unpaged());
    }

    @Nonnull
    private AutoCloseable mockWarehouseForPartner() {
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(SHOP_PARTNER_ID))
            .partnerTypes(Set.of(ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(SHOP_LOGISTIC_POINT_ID)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(List.of(logisticsPointResponse));
        return () -> verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }
}
