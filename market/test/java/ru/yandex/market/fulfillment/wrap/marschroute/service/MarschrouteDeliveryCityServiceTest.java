package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.api.DeliveryCityClient;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.delivery.city.MarschrouteDeliveryCityRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.MarschrouteDeliveryCity;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.MarschrouteDeliveryCityResponse;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class MarschrouteDeliveryCityServiceTest {
    @Mock
    private DeliveryCityClient deliveryCityClient;

    @InjectMocks
    MarschrouteDeliveryCityService service;


    @AfterEach
    void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(deliveryCityClient);
    }

    @Test
    void getDeliveryOptionsSuccess() {
        DeliveryOption deliveryOption = new DeliveryOption();
        MarschrouteDeliveryCityResponse response = createResponse(true, deliveryOption);
        when(deliveryCityClient.getDeliveryCities(any())).thenReturn(response);

        Collection<DeliveryOption> options = service.getDeliveryOptions(
            new MarschrouteDeliveryCityRequest()
                .setCityKladr("KLADR")
        );

        verify(deliveryCityClient, times(1)).getDeliveryCities(any());
        assertTrue(options.contains(deliveryOption));
    }

    @Test
    void getDeliveryOptionsFail() {
        MarschrouteDeliveryCityResponse response = createResponse(true, null);
        when(deliveryCityClient.getDeliveryCities(any())).thenReturn(response);
        expectThrown(() -> service.getDeliveryOptions(new MarschrouteDeliveryCityRequest().setCityKladr("KLADR")), FulfillmentApiException.class);
        verify(deliveryCityClient, times(1)).getDeliveryCities(any());

    }


    @Test
    void isCityIdUnknownPositive() {
        MarschrouteDeliveryCityResponse response = createResponse(true, null);
        when(deliveryCityClient.getDeliveryCities(any())).thenReturn(response);
        assertTrue(service.isCityIdUnknown("KLADR"), "City unknown when we receive info.code = 1 ");
        verify(deliveryCityClient, times(1)).getDeliveryCities(any());

    }


    @Test
    void isCityIdUnknownNegative() {
        MarschrouteDeliveryCityResponse response = createResponse(true, new DeliveryOption());
        when(deliveryCityClient.getDeliveryCities(any())).thenReturn(response);
        assertFalse(service.isCityIdUnknown("KLADR"), "City is known when we receive at least one delivery option");
        verify(deliveryCityClient, times(1)).getDeliveryCities(any());

    }

    @Test
    void isCityIdUnknownResponseFailed() {
        MarschrouteDeliveryCityResponse response = new MarschrouteDeliveryCityResponse();
        response.setSuccess(false);
        when(deliveryCityClient.getDeliveryCities(any()))
            .thenThrow(new FulfillmentApiException(new ErrorPair(ErrorCode.SERVICE_UNAVAILABLE, "someMessage")));

        expectThrown(() -> service.isCityIdUnknown("KLADR"), FulfillmentApiException.class);

        verify(deliveryCityClient, times(1)).getDeliveryCities(any());

    }

    private void expectThrown(Runnable runnable, Class<FulfillmentApiException> expected) {
        try {
            runnable.run();
            fail("No exception thrown. Expected " + expected.getName());
        } catch (Exception e) {
            assertTrue(
                expected.isAssignableFrom(e.getClass()),
                String.format("Thrown exception must be  instance of %s. Thrown %s",
                    expected.getName(),
                    e.getClass().getName()
                )
            );
        }
    }

    private MarschrouteDeliveryCityResponse createResponse(boolean success, DeliveryOption deliveryOption) {
        MarschrouteDeliveryCityResponse response = new MarschrouteDeliveryCityResponse();

        if (deliveryOption != null) {
            MarschrouteDeliveryCity data = new MarschrouteDeliveryCity();
            data.setKladr("kladr");
            data.setDeliveryOptions(Collections.singletonMap("KLADR", deliveryOption));
            response.setData(Collections.singletonList(data));
        }
        response.setCode(1);
        response.setSuccess(success);
        return response;
    }
}
