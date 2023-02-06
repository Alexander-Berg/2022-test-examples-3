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
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileChangeOrderSegmentRequestProcessor;
import ru.yandex.market.logistics.lom.utils.UpdateLastMileUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты обработки заявки на сегментах для изменения последней мили с самовывоза на самовывоз")
@DatabaseSetup("/service/change_last_mile/from_pickup_to_pickup/segment_request/before/setup.xml")
public class ChangeLastMileFromPickupToPickupSegmentRequestProcessingTest extends AbstractContextualTest {

    private static final Partner PICKUP_PARTNER = CreateLgwCommonEntitiesUtils.createPartner(1005720);
    private static final Partner MK_PARTNER = CreateLgwCommonEntitiesUtils.createPartner(1005705);
    private static final Partner INACTIVE_PICKUP_PARTNER = CreateLgwCommonEntitiesUtils.createPartner(100558);
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
    @DisplayName("Успешный вызов ds-create-order для PREPARING PICKUP сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/pickup_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/after/history_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successfullDsCreateOrderCall() {
        Order expectedDsOrder = UpdateLastMileUtils.expectedDsOrder(
            null,
            DeliveryType.PICKUP_POINT,
            UpdateLastMileUtils.pickupLocationTo(),
            PaymentMethod.CARD,
            null,
            false
        );
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).createOrder(
            eq(expectedDsOrder),
            eq(PICKUP_PARTNER),
            any(CreateOrderRestrictedData.class),
            eq(REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешный вызов ds-update-order для PREPARING MOVEMENT сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/movement_request.xml",
        type = DatabaseOperation.INSERT
    )
    @SneakyThrows
    void successfullDsUpdateOrderCall() {
        Order expectedDsOrder = UpdateLastMileUtils.expectedDsOrder(
            "external-id-mk",
            DeliveryType.MOVEMENT,
            UpdateLastMileUtils.pickupLocationTo(),
            PaymentMethod.PREPAID,
            null,
            false
        );
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrder(
            eq(expectedDsOrder),
            eq(MK_PARTNER),
            eq(REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешный вызов ds-cancel-order для INACTIVE PICKUP сегмента")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/inactive_pickup_request.xml",
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
            eq(INACTIVE_PICKUP_PARTNER),
            eq(REQUEST_META)
        );
    }

    @Test
    @DisplayName("У заказа есть активная заявка на отмену")
    @DatabaseSetup(
        value = {
            "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/pickup_request.xml",
            "/service/change_last_mile/to_pickup/segment_request/before/active_cancellation.xml"
        },
        type = DatabaseOperation.INSERT
    )
    void orderHasActiveCancellationRequest() {
        softly.assertThat(processor.processPayload(PAYLOAD)).isEqualTo(ProcessingResult.unprocessed());
    }

    @Test
    @DisplayName("Обработка финальной ошибки создания заказа в ПВЗ")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/pickup_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/after/pickup_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processPreparingPickupFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new ChangeOrderSegmentException("LGW Exception"));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Failed to create or update order");
    }

    @Test
    @DisplayName("Обработка финальной ошибки обновления заказа в TPL")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/movement_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/after/movement_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processPreparingMovementFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new ChangeOrderSegmentException("LGW Exception"));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Failed to create or update order");
    }


    @Test
    @DisplayName("Обработка финальной ошибки отмены заказа в ПО постаматов")
    @DatabaseSetup(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/before/inactive_pickup_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/change_last_mile/to_courier/segment_request/before/pickup_inactive.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/from_pickup_to_pickup/segment_request/after/"
            + "inactive_pickup_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processInactivePickupFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new ChangeOrderSegmentException("LGW Exception"));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Failed to cancel order");
    }
}
