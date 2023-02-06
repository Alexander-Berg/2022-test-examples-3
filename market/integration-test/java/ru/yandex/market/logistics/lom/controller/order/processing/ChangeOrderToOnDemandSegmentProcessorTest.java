package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.LgwClientRequestMetaConverter;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderToOnDemandSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@DisplayName("Обработка заявки на изменение сегмента Яндекс.Go, для преобразования заказа в заказ с доставкой по клику")
@DatabaseSetup("/controller/order/change_order_to_on_demand/segment/before/setup.xml")
class ChangeOrderToOnDemandSegmentProcessorTest extends AbstractContextualTest {

    private static final ChangeOrderSegmentRequestPayload PAYLOAD =
        PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private ChangeOrderToOnDemandSegmentProcessor processor;

    @Autowired
    private DeliveryClient deliveryClient;

    @Test
    @DisplayName("Заявка на изменение сегмента Яндекс.Go с типом COURIER обработана - выполнен DS.updateOrder в LGW")
    void testProcessCourierSegmentRequestSuccess() throws Exception {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);

        softly.assertThat(processingResult)
            .as("Asserting that the processing result is success")
            .isEqualTo(ProcessingResult.success());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(new Partner(50441L)),
            eq(LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(1L))
        );

        softly.assertThat(orderCaptor.getValue().getTags())
            .as("Asserting that tags are valid")
            .containsExactlyInAnyOrder("DEFERRED_COURIER", "ON_DEMAND");

        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName(
        "Заявка на изменение сегмента Яндекс.Go с типом GO_PLATFORM обработана - выполнен DS.updateOrder в LGW"
    )
    @DatabaseSetup(
        value = "/controller/order/change_order_to_on_demand/segment/before/setup_go_platform.xml",
        type = DatabaseOperation.UPDATE
    )
    void testProcessGOPlatformSegmentRequestSuccess() throws Exception {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);

        softly.assertThat(processingResult)
            .as("Asserting that the processing result is success")
            .isEqualTo(ProcessingResult.success());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(new Partner(50441L)),
            eq(LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(1L))
        );

        softly.assertThat(orderCaptor.getValue().getTags())
            .as("Asserting that tags are valid")
            .containsExactlyInAnyOrder("DEFERRED_COURIER", "ON_DEMAND");

        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Заявка на изменение сегмента Яндекс.Go не обработана, так как заказ отменён")
    @DatabaseSetup("/controller/order/change_order_to_on_demand/segment/before/cancelled_order.xml")
    void testProcessSegmentRequestForCancelledOrderUnprocessed() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);

        softly.assertThat(processingResult)
            .as("Asserting that the processing result is unprocessed")
            .isEqualTo(ProcessingResult.unprocessed());

        verifyZeroInteractions(deliveryClient);
    }
}
