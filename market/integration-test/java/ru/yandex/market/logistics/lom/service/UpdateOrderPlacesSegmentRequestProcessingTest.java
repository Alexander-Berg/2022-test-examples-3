package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderPlacesSegmentProcessor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup("/service/update_order_places_segment_request/before/prepare.xml")
public class UpdateOrderPlacesSegmentRequestProcessingTest extends AbstractContextualTest {

    private static final Long SEQUENCE_ID = 123454321L;
    private static final ClientRequestMeta REQUEST_META = new ClientRequestMeta(SEQUENCE_ID.toString());

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private UpdateOrderPlacesSegmentProcessor processor;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient, fulfillmentClient);
    }

    @Test
    @DisplayName("Успех (DS-API)")
    @SneakyThrows
    void successDsApi() {
        softly.assertThat(processor.processPayload(payload(22L))).isEqualTo(ProcessingResult.success());
        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.delivery.Order> orderCaptor = ArgumentCaptor
            .forClass(ru.yandex.market.logistic.gateway.common.model.delivery.Order.class);

        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(new Partner(202L)),
            eq(REQUEST_META)
        );

        ru.yandex.market.logistic.gateway.common.model.delivery.Order argValue = orderCaptor.getValue();
        softly.assertThat(argValue.getPlaces()).isEqualTo(expectedDsPlaces());
    }

    @Test
    @DisplayName("Успех (FF-api)")
    @SneakyThrows
    void successFfApi() {
        softly.assertThat(processor.processPayload(payload(21L))).isEqualTo(ProcessingResult.success());
        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> orderCaptor = ArgumentCaptor
            .forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);

        verify(fulfillmentClient).updateOrder(
            orderCaptor.capture(),
            eq(new Partner(201L)),
            eq(REQUEST_META)
        );

        ru.yandex.market.logistic.gateway.common.model.fulfillment.Order argValue = orderCaptor.getValue();
        softly.assertThat(argValue.getPlaces()).isEqualTo(expectedFfPlaces());
    }

    @Test
    @DisplayName("У заказа есть активная отмена")
    @SneakyThrows
    @DatabaseSetup(
        value = "/service/update_order_places_segment_request/before/active_cancellation.xml",
        type = DatabaseOperation.INSERT
    )
    void hasActiveCancellation() {
        softly.assertThat(processor.processPayload(payload(21L))).isEqualTo(ProcessingResult.success());
        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> orderCaptor = ArgumentCaptor
            .forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);

        verify(fulfillmentClient).updateOrder(
            orderCaptor.capture(),
            eq(new Partner(201L)),
            eq(REQUEST_META)
        );

        ru.yandex.market.logistic.gateway.common.model.fulfillment.Order argValue = orderCaptor.getValue();
        softly.assertThat(argValue.getPlaces()).isEqualTo(expectedFfPlaces());
    }

    @Test
    @DisplayName("Неподдерживаемый тип API")
    void unsupportedApiType() {
        softly.assertThatThrownBy(() -> processor.processPayload(payload(23L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order id=1: updating places is unsupported for a partner with type YANDEX_GO_SHOP.");
    }

    @Nonnull
    private ChangeOrderSegmentRequestPayload payload(long changeRequestId) {
        ChangeOrderSegmentRequestPayload payload = new ChangeOrderSegmentRequestPayload(
            REQUEST_ID,
            changeRequestId
        );

        payload.setSequenceId(SEQUENCE_ID);
        return payload;
    }

    @Nonnull
    private List<ru.yandex.market.logistic.gateway.common.model.delivery.Place> expectedDsPlaces() {
        return List.of(
            new ru.yandex.market.logistic.gateway.common.model.delivery.Place(
                ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId.builder()
                    .setYandexId("1001")
                    .setDeliveryId("place external id 1001")
                    .setPartnerId("place external id 1001")
                    .build(),
                new ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte(
                    6,
                    4,
                    2,
                    BigDecimal.valueOf(8),
                    null,
                    null
                ),
                List.of(
                    new ru.yandex.market.logistic.gateway.common.model.delivery.PartnerCode(
                        "101",
                        "place external id 1001"
                    )
                ),
                List.of(
                    new ru.yandex.market.logistic.gateway.common.model.delivery.ItemPlace(
                        new ru.yandex.market.logistic.gateway.common.model.delivery.UnitId(
                            null,
                            100L,
                            "item article 1"
                        ),
                        1
                    )
                )
            )
        );
    }

    @Nonnull
    private List<ru.yandex.market.logistic.gateway.common.model.fulfillment.Place> expectedFfPlaces() {
        return List.of(
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.Place(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                    .setYandexId("1001")
                    .setFulfillmentId("place external id 1001")
                    .setPartnerId("place external id 1001")
                    .build(),
                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte(
                    6,
                    4,
                    2,
                    BigDecimal.valueOf(8),
                    null,
                    null
                ),
                List.of(
                    new ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerCode(
                        "101",
                        "place external id 1001"
                    )
                ),
                List.of(
                    new ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace(
                        new ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId(
                            null,
                            100L,
                            "item article 1"
                        ),
                        1
                    )
                )
            )
        );
    }
}
