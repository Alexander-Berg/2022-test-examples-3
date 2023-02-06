package ru.yandex.market.sc.internal.controller.partner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryBoxDto;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderAction;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderDto;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderStatus;
import ru.yandex.market.sc.internal.controller.dto.delivery.PartnerDeliveryOrderVerificationDto;
import ru.yandex.market.sc.internal.domain.order.PartnerDeliveryOrderService;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerDeliveryOrderControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    @MockBean
    private PartnerDeliveryOrderService partnerDeliveryOrderService;

    private SortingCenter sortingCenter;

    @BeforeEach
    void setup() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    void testGetStaticFilters() {
        mockMvc.perform(get("/internal/partners/" + sortingCenter.getPartnerId() + "/delivery/filters"))
                .andExpect(status().isOk())
                .andExpect(content().json(fileContent("delivery/get_static_filters.json"), true));
    }

    @Test
    @SneakyThrows
    void testGetOrders() {
        when(partnerDeliveryOrderService.getOrders(any(), eq(sortingCenter), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        buildOrderDto(),
                        buildOrderDto().toBuilder()
                                .status(PartnerDeliveryOrderStatus.READY_TO_RETURN)
                                .actions(List.of(PartnerDeliveryOrderAction.DELIVER_RETURN))
                                .build()
                )));

        mockMvc.perform(get("/internal/partners/" + sortingCenter.getPartnerId() + "/delivery/orders"))
                .andExpect(status().isOk())
                .andExpect(content().json(fileContent("delivery/get_orders.json"), true));
    }

    @Test
    @SneakyThrows
    void testGetOrder() {
        when(partnerDeliveryOrderService.getOrder(1L, sortingCenter))
                .thenReturn(buildOrderDto().toBuilder()
                        .actions(List.of(
                                PartnerDeliveryOrderAction.RECEIVE_FINISH,
                                PartnerDeliveryOrderAction.PRINT_LABEL))
                        .build());

        mockMvc.perform(get("/internal/partners/" + sortingCenter.getPartnerId() + "/delivery/orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(fileContent("delivery/get_order.json"), true));
    }

    private PartnerDeliveryOrderDto buildOrderDto() {
        return PartnerDeliveryOrderDto.builder()
                .id(1)
                .externalId("2")
                .status(PartnerDeliveryOrderStatus.AWAITING_RECEIVE)
                .senderName("Отправитель посылок")
                .senderPhone("102")
                .createdAt(LocalDateTime.of(2022, 4, 1, 12, 30, 50))
                .receiveCell("cell")
                .returnCell("cell")
                .verificationCode(PartnerDeliveryOrderVerificationDto.builder()
                        .accepted(false)
                        .attemptsLeftToVerify(3)
                        .build())
                .box(PartnerDeliveryBoxDto.builder()
                        .width(1)
                        .height(2)
                        .length(3)
                        .weight(BigDecimal.TEN)
                        .type("S")
                        .iconUrl("https://yandex.ru/")
                        .build())
                .actions(List.of(PartnerDeliveryOrderAction.RECEIVE_CHECK))
                .build();
    }

}
