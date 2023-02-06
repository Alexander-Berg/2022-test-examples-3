package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateReturnInPvzProcessor;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.pvz.client.logistics.dto.ReturnItemCreateDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestCreateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Создание возврата в ПВЗ")
class CreateReturnInPvzProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_SEGMENT_ID = 1;
    private static final long CHECKOUTER_ORDER_ID = 654987;

    @Autowired
    private CreateReturnInPvzProcessor processor;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private PvzLogisticsClient pvzLogisticsClient;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(checkouterAPI, pvzLogisticsClient);
    }

    @Test
    @DisplayName("Успешная обработка")
    @DatabaseSetup("/database/tasks/return-segment/create-in-pvz/before/setup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-pvz/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        mock();
        processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build());
        verifyTask();
    }

    @Test
    @DisplayName("Отсутствует идентификатор ПВЗ")
    @DatabaseSetup("/database/tasks/return-segment/create-in-pvz/before/setup_no_pvz_id.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noPvzId() {
        mock();
        softly.assertThatThrownBy(
                () -> processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build())
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Unable to convert return segment to pvz ReturnRequestCreateDto without market pvz id, segment id 1"
            );
        verify(checkouterAPI).getOrder(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, null)),
            safeRefEq(OrderRequest.builder(CHECKOUTER_ORDER_ID).build())
        );
    }

    @Test
    @DisplayName("Ошибка обработки от ПВЗ")
    @DatabaseSetup("/database/tasks/return-segment/create-in-pvz/before/setup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fail() {
        mock();
        when(pvzLogisticsClient.createReturnRequest(any(ReturnRequestCreateDto.class)))
            .thenThrow(new RuntimeException("error message"));
        softly.assertThatThrownBy(
                () -> processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build())
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("error message");
        verifyTask();
    }

    private void mock() {
        Buyer buyer = new Buyer();
        buyer.setFirstName("first");
        buyer.setLastName("last");

        OrderItem item = new OrderItem();
        item.setSupplierId(4321L);
        item.setShopSku("KJH876");
        item.setPrice(BigDecimal.ONE);

        Order order = new Order();
        order.setBuyer(buyer);
        order.setItems(List.of(item));
        when(checkouterAPI.getOrder(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, null)),
            safeRefEq(OrderRequest.builder(CHECKOUTER_ORDER_ID).build())
        ))
            .thenReturn(order);
    }

    private void verifyTask() {
        verify(checkouterAPI).getOrder(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, null)),
            safeRefEq(OrderRequest.builder(CHECKOUTER_ORDER_ID).build())
        );
        verify(pvzLogisticsClient).createReturnRequest(eq(
            ReturnRequestCreateDto.builder()
                .orderId(String.valueOf(CHECKOUTER_ORDER_ID))
                .buyerName("first last")
                .barcode("box-external-id")
                .requestDate(LocalDate.of(2021, 11, 20))
                .pickupPointId(12345L)
                .items(List.of(
                    ReturnItemCreateDto.builder()
                        .count(1L)
                        .price(1.)
                        .build()
                ))
                .build()
        ));
    }
}
