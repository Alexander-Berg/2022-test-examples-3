package ru.yandex.market.promoboss.api.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.promoboss.exception.PromoNotFoundException;
import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.market.promoboss.service.PromoService;
import ru.yandex.market.promoboss.utils.PromoFieldUtils;
import ru.yandex.mj.generated.client.self_client.api.GetPromoApiClient;
import ru.yandex.mj.generated.client.self_client.model.PromoResponseV2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GetPromoApiTest extends AbstractApiTest {
    private static final Set<PromoField> PROMO_FIELD_SET_ALL = PromoFieldUtils.getAll();
    private static final List<String> REQUEST_PROMO_FIELD_LIST_ALL
            = Arrays.stream(PromoField.values()).map(PromoField::getApiValue).collect(Collectors.toList());

    @MockBean
    private PromoService promoService;

    @Autowired
    private GetPromoApiClient getPromoApiClient;

    @Test
    public void shouldReturn200ok() {

        // setup
        when(promoService.getPromo(any(), any()))
                .thenReturn(PROMO);

        // act
        PromoResponseV2 actualResponse = getPromoApiClient.promoGetV2(PROMO_ID, REQUEST_PROMO_FIELD_LIST_ALL).schedule().join();

        // verify
        verify(promoService).getPromo(PROMO_ID, PROMO_FIELD_SET_ALL);
        assertEquals(PROMO_RESPONSE, actualResponse);
    }

    @Test
    public void shouldReturn200okWithSsku() {

        // setup
        when(promoService.getPromo(any(), any()))
                .thenReturn(PROMO_WITH_SSKU);

        // act
        PromoResponseV2 actualResponse = getPromoApiClient.promoGetV2(PROMO_ID, REQUEST_PROMO_FIELD_LIST_ALL).schedule().join();

        // verify
        verify(promoService).getPromo(PROMO_ID, PROMO_FIELD_SET_ALL);
        assertEquals(PROMO_RESPONSE_WITH_SSKU, actualResponse);
    }

    @Test
    public void shouldReturn404NotFound() {

        // setup
        doThrow(new PromoNotFoundException()).when(promoService).getPromo(any(), any());

        // act
        Exception e = assertThrows(Exception.class, () -> getPromoApiClient.promoGetV2(PROMO_ID, REQUEST_PROMO_FIELD_LIST_ALL)
                .schedule()
                .join());

        // verify
        verify(promoService).getPromo(PROMO_ID, PROMO_FIELD_SET_ALL);
        assertEquals(HttpStatus.NOT_FOUND.value(), ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }

    @Test
    public void shouldReturn500InternalServerErrorIfPromoIdNull() {

        // setup
        doThrow(new PromoNotFoundException()).when(promoService).getPromo(any(), any());

        // act
        Exception e = assertThrows(Exception.class, () -> getPromoApiClient.promoGetV2(null, REQUEST_PROMO_FIELD_LIST_ALL)
                .schedule()
                .join());

        // verify
        verifyNoInteractions(promoService);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }

    @Test
    public void shouldReturn400BadRequestIfPromoIdEmptyString() {

        // act
        Exception e = assertThrows(CompletionException.class, () -> getPromoApiClient.promoGetV2("", REQUEST_PROMO_FIELD_LIST_ALL)
                .schedule()
                .join());

        // verify
        verifyNoInteractions(promoService);
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());

        assertTrue(e.getCause().getMessage().contains("Empty id parameter"));
    }

    @Test
    public void shouldReturn500InternalServerErrorIfUnknownExceptionThrown() {

        // setup
        when(promoService.getPromo(PROMO_ID, PROMO_FIELD_SET_ALL)).thenThrow(new RuntimeException());

        // act
        Exception e = assertThrows(Exception.class, () -> getPromoApiClient.promoGetV2(PROMO_ID, REQUEST_PROMO_FIELD_LIST_ALL)
                .schedule()
                .join());

        // verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }
}
