package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.FF4ShopsOutboundPayload;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Сохранение отправок")
@DatabaseSetup("/queue/processor/save_outbound/prepare.xml")
@ParametersAreNonnullByDefault
class SaveOutboundProcessorTest extends AbstractIntegrationTest {
    private static final Instant NOW = Instant.parse("2022-02-21T14:30:00.00Z");
    private static final Instant FROM = Instant.parse("2022-02-21T15:30:00.00Z");
    private static final Instant TO = Instant.parse("2022-02-21T16:30:00.00Z");

    @Autowired
    private SaveOutboundProcessor processor;

    @Autowired
    private LomClient lomClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успех: отправка в базе")
    @DatabaseSetup(
        value = "/queue/processor/save_outbound/after/all_orders_from_l4s.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/after/all_orders_from_l4s.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successOutboundAlreadyCreated() {
        processor.execute(ff4shopsOutboundPayload(List.of(100101L, 4500L)));
    }

    @Test
    @DisplayName("Успех: все заказы есть в базе")
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/after/all_orders_from_l4s.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAllOrdersInBase() {
        processor.execute(ff4shopsOutboundPayload(List.of(100101L, 100103L)));
    }

    @Test
    @DisplayName("Успех: некоторые заказы есть в базе, остальные есть в ломе")
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/after/some_orders_from_l4s.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successSomeOrdersInBase() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("4500")).build(),
            Pageable.unpaged()
        ))
            .thenReturn(PageResult.of(
                List.of(new OrderDto().setExternalId("4500").setSenderId(200L).setWaybill(
                    List.of(waybillSegment(PartnerType.DROPSHIP, 102L), waybillSegment(PartnerType.DELIVERY, 202L))
                )),
                1,
                1,
                1
            ));

        processor.execute(ff4shopsOutboundPayload(List.of(100101L, 4500L)));

        verify(lomClient).searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("4500")).build(),
            Pageable.unpaged()
        );
    }

    @Test
    @DisplayName("Успех: заказов нет в базе, есть в ломе")
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/after/no_orders_from_l4s.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNoOrdersInBase() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("3500", "4500")).build(),
            Pageable.unpaged()
        ))
            .thenReturn(lomOrders(List.of(
                new OrderDto().setExternalId("4500").setSenderId(200L).setWaybill(
                    List.of(waybillSegment(PartnerType.DROPSHIP, 102L), waybillSegment(PartnerType.DELIVERY, 202L))
                ),
                new OrderDto().setExternalId("3500").setSenderId(300L).setWaybill(
                    List.of(waybillSegment(PartnerType.DROPSHIP, 103L), waybillSegment(PartnerType.DELIVERY, 203L))
                )
            )));

        processor.execute(ff4shopsOutboundPayload(List.of(3500L, 4500L)));

        verify(lomClient).searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("3500", "4500")).build(),
            Pageable.unpaged()
        );
    }

    @Test
    @DisplayName("Заказов нигде нет")
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderNotExist() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("888", "999")).build(),
            Pageable.unpaged()
        ))
            .thenReturn(PageResult.of(List.of(), 1, 1, 1));

        softly.assertThatCode(() -> processor.execute(ff4shopsOutboundPayload(List.of(999L, 888L))))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [888, 999]");

        verify(lomClient).searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("888", "999")).build(),
            Pageable.unpaged()
        );
    }

    @Test
    @DisplayName("Нет дропшип сегмента")
    @ExpectedDatabase(
        value = "/queue/processor/save_outbound/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noPartnerMappings() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("3500", "4500")).build(),
            Pageable.unpaged()
        ))
            .thenReturn(lomOrders(List.of(
                new OrderDto().setExternalId("4500").setSenderId(999L).setWaybill(
                    List.of(waybillSegment(PartnerType.FULFILLMENT, 100L), waybillSegment(PartnerType.DELIVERY, 200L))
                ),
                new OrderDto().setExternalId("3500").setSenderId(300L)
            )));

        softly.assertThatCode(() -> processor.execute(ff4shopsOutboundPayload(List.of(3500L, 4500L))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No dropship segment found for order 4500");

        verify(lomClient).searchOrders(
            OrderSearchFilter.builder().barcodes(Set.of("3500", "4500")).build(),
            Pageable.unpaged()
        );
    }

    @Nonnull
    private FF4ShopsOutboundPayload ff4shopsOutboundPayload(List<Long> orderIds) {
        return FF4ShopsOutboundPayload.builder()
            .id(100002L)
            .externalId("12345")
            .yandexId("ya-id")
            .confirmed(NOW)
            .intervalFrom(FROM)
            .intervalTo(TO)
            .orderIds(orderIds)
            .build();
    }

    @Nonnull
    private PageResult<OrderDto> lomOrders(List<OrderDto> dtos) {
        return PageResult.of(dtos, 1, 1, 1);
    }

    @Nonnull
    private WaybillSegmentDto waybillSegment(PartnerType partnerType, Long partnerId) {
        return WaybillSegmentDto.builder().partnerType(partnerType).partnerId(partnerId).build();
    }
}
