package ru.yandex.market.delivery.mdbapp.scheduled;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.checkout.checkouter.client.CheckouterDeliveryAPI;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.export.client.DeliveryExportClient;
import ru.yandex.market.delivery.export.client.payload.TariffOptions;
import ru.yandex.market.delivery.export.client.payload.TariffParams;
import ru.yandex.market.delivery.mdbapp.IntegrationTest;
import ru.yandex.market.delivery.mdbapp.integration.enricher.TariffDataEnricher;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.payload.GetTariffData;
import ru.yandex.market.delivery.mdbapp.integration.transformer.GetTariffDataTransformer;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static steps.orderSteps.OrderEventSteps.getOrderHistoryEvent;
import static steps.orderSteps.OrderSteps.getRedSingleOrder;

@RunWith(SpringRunner.class)
@MockBean({
    LMSClient.class,
    PechkinHttpClient.class,
    ScIntClient.class,
})
@IntegrationTest
public class FulfilmentFlowOrderEventsTest {

    private static final String YADO_TARIFF_CODE = "RM";
    private static final String TARIFFICATOR_TARIFF_CODE = "TRM";
    private static final long DELIVERY_SERVICE_ID = 123L;
    private static final long TARIFFICATOR_TARIFF_ID = 2234562L;
    private static final long YADO_TARIFF_ID = 223L;

    private static final TariffDto TARIFFICATOR_TARIFF = TariffDto.builder()
        .id(TARIFFICATOR_TARIFF_ID)
        .code(TARIFFICATOR_TARIFF_CODE)
        .partnerId(DELIVERY_SERVICE_ID)
        .deliveryMethod(DeliveryMethod.PICKUP)
        .currency("RUB")
        .build();

    private static final TariffParams YADO_TARIFF = new TariffParams()
        .setId(YADO_TARIFF_ID)
        .setCode(YADO_TARIFF_CODE)
        .setCarrierId(DELIVERY_SERVICE_ID)
        .setCurrency("RUB")
        .setDeliveryMethod("PICKUP")
        .setTariffOptions(new TariffOptions());

    @Autowired
    private OrderEventsGateway gateway;

    @SpyBean
    private GetTariffDataTransformer getTariffDataTransformer;

    @MockBean
    private CheckouterDeliveryAPI checkouterDeliveryAPI;

    @MockBean
    private DeliveryExportClient deliveryExportClient;

    @MockBean
    private TarifficatorClient tarifficatorClient;

    @SpyBean
    private TariffDataEnricher tariffDataEnricher;

    @Test
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void shouldGetTariffDataIfTarifficatorTariffIdIsPercent() {
        doReturn(TARIFFICATOR_TARIFF).when(tarifficatorClient).getTariff(TARIFFICATOR_TARIFF_ID);
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelTarifficatorTariffId();

        gateway.processEvent(event);
        verifyMockTariffDataHandler(TARIFFICATOR_TARIFF_ID, TARIFFICATOR_TARIFF_CODE);
    }

    @Test
    @DisplayName("Выключен флоу обогащения тарифа")
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/get-tariff-flow-disabled-flag.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/clean_internal_variable.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    public void noTariffHandlingIfGetTariffIsDisabled() {
        gateway.processEvent(getOrderHistoryEventSingleParcelTarifficatorTariffId());

        verify(checkouterDeliveryAPI, never()).putTariffData(
            any(Long.class),
            any(ClientInfo.class),
            any(TariffData.class)
        );
        verifyZeroInteractions(getTariffDataTransformer, tariffDataEnricher);
    }

    @Test
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void shouldGetTariffDataIfYadoTariffIdIsPercent() {
        doReturn(YADO_TARIFF).when(deliveryExportClient).getTariffParams(YADO_TARIFF_ID);
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelYadoTariffId();

        gateway.processEvent(event);
        verifyMockTariffDataHandler(YADO_TARIFF_ID, YADO_TARIFF_CODE);
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryEventSingleParcelTarifficatorTariffId() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedSingleOrder();
        redOrderBefore.setFulfilment(true);
        redOrderBefore.getDelivery().setTariffId(2234562L);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedSingleOrder();
        redOrderAfter.setFulfilment(true);
        redOrderAfter.getDelivery().setTariffId(2234562L);
        event.setOrderAfter(redOrderAfter);

        return event;
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryEventSingleParcelYadoTariffId() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedSingleOrder();
        redOrderBefore.setFulfilment(true);
        redOrderBefore.getDelivery().setTariffId(223L);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedSingleOrder();
        redOrderAfter.setFulfilment(true);
        redOrderAfter.getDelivery().setTariffId(223L);
        event.setOrderAfter(redOrderAfter);

        return event;
    }

    private void verifyMockTariffDataHandler(long tariffId, String tariffCode) {
        long orderId = 123L;
        GetTariffData getTariffData = new GetTariffData(orderId, tariffId);
        TariffData tariffData = new TariffData();
        tariffData.setTariffCode(tariffCode);
        getTariffData.setTariffData(tariffData);

        verify(getTariffDataTransformer).getTariffData(argThat(
            orderWrapper -> Objects.equals(
                tariffId,
                orderWrapper.getOrder().getDelivery().getTariffId()
            )
        ));
        verify(tariffDataEnricher).getTariffData(argThat(
            data -> Objects.equals(data.toString(), getTariffData.toString())
                && Objects.equals(data.getTariffData().toString(), tariffData.toString())
        ));
        verify(checkouterDeliveryAPI).putTariffData(eq(orderId), any(ClientInfo.class), refEq(tariffData));
    }
}
