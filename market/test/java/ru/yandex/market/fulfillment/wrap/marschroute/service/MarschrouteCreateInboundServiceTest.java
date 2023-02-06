package ru.yandex.market.fulfillment.wrap.marschroute.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.yandex.market.fulfillment.wrap.marschroute.api.ProductsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.WaybillsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.client.MarschrouteApiClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.client.method.MarschrouteMethods;
import ru.yandex.market.fulfillment.wrap.marschroute.api.request.waybill.CreateInboundRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.CreateWaybillResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.InboundInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteUpdateProductsRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.notification.Notifier;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.InboundType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarschrouteCreateInboundServiceTest extends IntegrationTest {

    private static final String YANDEX_ID = "SomeId";
    private static final String PARTNER_ID = "SomeDocId";
    private static final InboundInfo INFO = new InboundInfo(YANDEX_ID, PARTNER_ID);

    @Autowired
    private MarschrouteCreateInboundService service;

    @MockBean
    private WaybillsClient waybillsClient;

    @SpyBean
    private ProductsClient productsClient;

    @MockBean
    @Qualifier("marschrouteApiClient")
    private MarschrouteApiClient marschrouteApiClient;

    @MockBean
    private Notifier failNotifier;

    @Test
    void testThrowingErrorWhenFailedProductsUpdate() {

        when(inboundInfoRepository.findByYandexId(YANDEX_ID)).thenReturn(Optional.empty());
        when(inboundInfoRepository.save(INFO)).thenReturn(INFO);

        when(waybillsClient.createWaybill(any(CreateInboundRequest.class)))
            .thenReturn(new CreateWaybillResponse().setDocId("SomeDocId"));

        doThrow(new FulfillmentApiException(new ErrorPair()))
            .when(marschrouteApiClient)
            .exchange(eq(MarschrouteMethods.UPDATE_PRODUCTS), any(MarschrouteUpdateProductsRequest.class));

        Inbound inbound = new Inbound.InboundBuilder(
                new ResourceId("SomeId", "SomeId"),
                null,
                Collections.emptyList(),
                new DateTimeInterval(OffsetDateTime.now(), OffsetDateTime.now())
        ).build();

        ResourceId resourceId = service.execute(inbound);
        verify(productsClient, times(1)).updateProducts(any(MarschrouteUpdateProductsRequest.class));
        verify(failNotifier).notifyUpdateProductsFailed(any(MarschrouteUpdateProductsRequest.class),
            eq(inbound),
            matches("ErrorPair\\{code=.*, message='.*', description='.*', params=.*\\}")
        );

        verify(inboundInfoRepository).save(INFO);

        softly.assertThat(resourceId.getYandexId())
            .as("Inbound yandex ID")
            .isEqualTo("SomeId");
        softly.assertThat(resourceId.getPartnerId())
            .as("Inbound doc ID")
            .isEqualTo("SomeDocId");
    }

    @Test
    void testThrowingErrorWhenTryingToCreateCrossdockInbound() {

        Inbound inbound = new Inbound.InboundBuilder(
                new ResourceId("SomeId", "SomeId"),
                InboundType.CROSSDOCK,
                Collections.emptyList(),
                new DateTimeInterval(OffsetDateTime.now(),
                OffsetDateTime.now())
        ).build();

        softly.assertThatThrownBy(() -> service.execute(inbound)).isInstanceOf(FulfillmentApiException.class);
    }
}
