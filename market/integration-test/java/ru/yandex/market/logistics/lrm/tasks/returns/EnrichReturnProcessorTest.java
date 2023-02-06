package ru.yandex.market.logistics.lrm.tasks.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.model.exception.ModelResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.EnrichReturnProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обогащение возврата")
class EnrichReturnProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_ID = 1;
    private static final String CHECKOUTER_ORDER_ID = "order-external-id";
    private static final Long VENDOR_ID = 111L;
    private static final String ARTICLE = "article";
    private static final long SHOP_PARTNER_ID = 104038L;
    private static final long SHOP_LOGISTIC_POINT_ID = 5038L;

    @Autowired
    private EnrichReturnProcessor processor;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setupMocks() {
        clock.setFixed(Instant.parse("2022-03-04T11:12:13.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(featureProperties.isEnableControlPointStarting()).thenReturn(true);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lomClient, lmsClient);
    }

    @Test
    @DisplayName("Не найдены некоторые позиции возрата")
    @DatabaseSetup("/database/tasks/returns/enrich/before/not_found_items.xml")
    void returnItemsNotFound() throws Exception {
        try (var ignored1 = mockOrderSearch(new OrderDto())) {
            softly.assertThatThrownBy(this::execute)
                .isInstanceOf(ModelResourceNotFoundException.class)
                .hasMessage("Failed to find RETURN_ITEM with ids [1, 2]");
        }
    }

    @Test
    @DisplayName("Возврат в ФФ, курьерский")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal_courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/fulfillment_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fulfillmentCourier() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(100))
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(600L)
                    .partnerType(PartnerType.FULFILLMENT)
                    .build()
            ));
        try (var ignored1 = mockOrderSearch(orderDto)) {
            execute();
        }
    }

    @Test
    @DisplayName("Возврат в мерча, курьерский")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal_courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/shop_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shopCourier() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(200))
            .setSenderId(1165929L)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(SHOP_PARTNER_ID)
                    .partnerType(PartnerType.DROPSHIP)
                    .build()
            ));
        try (
            var ignored1 = mockOrderSearch(orderDto)
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Возврат в ФФ")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/fulfillment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fulfillment() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(300))
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(600L)
                    .partnerType(PartnerType.FULFILLMENT)
                    .build()
            ));
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .externalId("12345")
            .partnerId(2001L)
            .build();
        try (
            var ignored1 = mockOrderSearch(orderDto);
            var ignored2 = mockLogisticPoint(logisticsPointResponse)
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Возврат в мерча")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/shop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shop() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(400))
            .setSenderId(1165929L)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(SHOP_PARTNER_ID)
                    .partnerType(PartnerType.DROPSHIP)
                    .build()
            ));
        try (
            var ignored1 = mockOrderSearch(orderDto);
            var ignore2 = mockLogisticPoint()
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Возврат в мерча, создание контрольной точки выключено")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/shop_not_start_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shopNotCreateControlPoint() throws Exception {
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(false);
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(400))
            .setSenderId(1165929L)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(SHOP_PARTNER_ID)
                    .partnerType(PartnerType.DROPSHIP)
                    .build()
            ));
        try (
            var ignored1 = mockOrderSearch(orderDto);
            var ignore2 = mockLogisticPoint()
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Невыкуп")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal_cancellation_return.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/cancellation_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationReturn() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(500))
            .setSenderId(1165929L)
            .setPickupPointId(1234L)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(SHOP_PARTNER_ID)
                    .partnerType(PartnerType.DROPSHIP)
                    .waybillSegmentStatusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.IN)
                            .build()
                    ))
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerId(75735L)
                    .segmentType(SegmentType.SORTING_CENTER)
                    .warehouseLocation(
                        LocationDto.builder()
                            .warehouseId(10001700279L)
                            .build()
                    )
                    .waybillSegmentStatusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.IN)
                            .build()
                    ))
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .waybillSegmentStatusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.INFO_RECEIVED)
                            .build()
                    ))
                    .build()
            ));
        try (
            var ignored1 = mockOrderSearch(orderDto)
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Невыкуп в ПВЗ")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal_cancellation_return.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/after/cancellation_return_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationReturnPickup() throws Exception {
        OrderDto orderDto = new OrderDto()
            .setItems(createItemDtos(600))
            .setSenderId(1165929L)
            .setPickupPointId(1234L)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerId(SHOP_PARTNER_ID)
                    .partnerType(PartnerType.DROPSHIP)
                    .build(),
                WaybillSegmentDto.builder()
                    .partnerId(75735L)
                    .segmentType(SegmentType.SORTING_CENTER)
                    .warehouseLocation(
                        LocationDto.builder()
                            .warehouseId(10001700279L)
                            .build()
                    )
                    .waybillSegmentStatusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.OUT)
                            .build()
                    ))
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .waybillSegmentStatusHistory(List.of(
                        WaybillSegmentStatusHistoryDto.builder()
                            .status(SegmentStatus.TRANSIT_PICKUP)
                            .build()
                    ))
                    .build()
            ));
        try (
            var ignored1 = mockOrderSearch(orderDto);
            var ignore2 = mockLogisticPoint()
        ) {
            execute();
        }
    }

    @Test
    @DisplayName("Заказ не найден")
    @DatabaseSetup("/database/tasks/returns/enrich/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/enrich/before/minimal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderNotFound() throws Exception {
        try (var ignored1 = mockOrderSearch()) {
            softly.assertThatThrownBy(this::execute)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to find ORDER with id order-external-id");
        }
    }

    @Nonnull
    private AutoCloseable mockOrderSearch(PageResult<OrderDto> pageResult) {
        OrderSearchFilter orderSearchFilter = OrderSearchFilter.builder()
            .barcodes(Set.of(CHECKOUTER_ORDER_ID))
            .build();
        when(lomClient.searchOrders(orderSearchFilter, Pageable.unpaged())).thenReturn(pageResult);
        return () -> verify(lomClient).searchOrders(orderSearchFilter, Pageable.unpaged());
    }

    @Nonnull
    private AutoCloseable mockOrderSearch(OrderDto orderDto) {
        return mockOrderSearch(PageResult.of(List.of(orderDto), 1, 1, 1));
    }

    @Nonnull
    private AutoCloseable mockOrderSearch() {
        return mockOrderSearch(PageResult.empty(Pageable.unpaged()));
    }

    @Nonnull
    private AutoCloseable mockLogisticPoint(LogisticsPointResponse logisticsPointResponse) {
        when(lmsClient.getLogisticsPoint(1234L)).thenReturn(Optional.of(logisticsPointResponse));
        return () -> verify(lmsClient).getLogisticsPoint(1234L);
    }

    @Nonnull
    private AutoCloseable mockLogisticPoint() {
        return mockLogisticPoint(
            LogisticsPointResponse.newBuilder()
                .externalId("external")
                .build()
        );
    }

    @Nonnull
    private List<ItemDto> createItemDtos(int assessedValue) {
        return List.of(
            ItemDto.builder()
                .vendorId(VENDOR_ID)
                .article(ARTICLE)
                .assessedValue(
                    MonetaryDto.builder()
                        .value(BigDecimal.valueOf(assessedValue))
                        .build()
                )
                .build()
        );
    }

    private void execute() {
        processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build());
    }
}
