package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileChangeOrderSegmentRequestProcessor;
import ru.yandex.market.logistics.lom.utils.UpdateLastMileUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты обработки заявки изменения последней мили на курьерку на сегментах")
@DatabaseSetup("/service/change_last_mile/to_courier/segment_request/before/setup.xml")
public class ChangeLastMileToCourierSegmentRequestProcessingTest extends AbstractContextualTest {

    private static final Partner MK_PARTNER = CreateLgwCommonEntitiesUtils.createPartner(63132);
    private static final Partner PICKUP_PARTNER = CreateLgwCommonEntitiesUtils.createPartner(63112);
    private static final ClientRequestMeta REQUEST_META = new ClientRequestMeta("1");
    private static final ChangeOrderSegmentRequestPayload PAYLOAD =
        PayloadFactory.createChangeOrderSegmentRequestPayload(1, "1", 1);

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private ChangeLastMileChangeOrderSegmentRequestProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-10-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Успешный вызов ds-update-order для COURIER сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/courier_request.xml",
        type = DatabaseOperation.INSERT
    )
    @SneakyThrows
    void successfullDsUpdateOrderCall() {
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(MK_PARTNER),
            eq(REQUEST_META)
        );
        Order orderCaptorValue = orderCaptor.getValue();
        softly.assertThat(orderCaptorValue)
            .isEqualTo(UpdateLastMileUtils.expectedDsOrder(
                "mk-external-id",
                DeliveryType.COURIER,
                UpdateLastMileUtils.recipientLocationTo(),
                PaymentMethod.CARD,
                "Комментарий",
                true
            ));
    }

    @Test
    @DisplayName("Успешный вызов ds-cancel-order для PICKUP сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/pickup_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/pickup_inactive.xml",
        type = DatabaseOperation.REFRESH
    )
    @SneakyThrows
    void successfullDsCancelOrderCall() {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).cancelOrder(
            eq(ResourceId.builder().setYandexId("1001").setPartnerId("pickup-external-id").build()),
            eq(PICKUP_PARTNER),
            eq(REQUEST_META)
        );
    }

    @Test
    @DisplayName("У заказа есть активная отмена")
    @DatabaseSetup(
        value = {
            "/service/change_last_mile/to_courier/segment_request/before/courier_request.xml",
            "/service/change_last_mile/to_pickup/segment_request/before/active_cancellation.xml"
        },
        type = DatabaseOperation.INSERT
    )
    void orderHasActiveCancellation() {
        softly.assertThat(processor.processPayload(PAYLOAD)).isEqualTo(ProcessingResult.unprocessed());
    }

    @Test
    @DisplayName("Обработка финальной ошибки обновления для COURIER сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/courier_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_courier/segment_request/after/courier_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void processCourierFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new ChangeOrderSegmentException("LGW Exception"));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Failed to create or update order");
    }

    @Test
    @DisplayName("Обработка финальной ошибки отмены заказа в ПВЗ")
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/pickup_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/pickup_inactive.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_courier/segment_request/after/pickup_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void processPickupFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new ChangeOrderSegmentException("LGW Exception"));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Failed to cancel order");
    }
}
