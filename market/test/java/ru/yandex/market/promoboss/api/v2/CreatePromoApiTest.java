package ru.yandex.market.promoboss.api.v2;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.promoboss.service.PromoService;
import ru.yandex.mj.generated.client.self_client.api.CreatePromoApiClient;
import ru.yandex.mj.generated.client.self_client.model.PromoMainRequestParams;
import ru.yandex.mj.generated.client.self_client.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CreatePromoApiTest extends AbstractApiTest {

    @MockBean
    private PromoService promoService;

    @Autowired
    private CreatePromoApiClient createPromoApiClient;

    @BeforeEach
    public void resetMocks() {
        reset(promoService);
    }

    @Test
    public void shouldReturn200ok() {

        // act and verify
        assertDoesNotThrow(() -> createPromoApiClient.promoCreateV2(PROMO_REQUEST).scheduleVoid().join());
        verify(promoService).createPromo(any(), any(), any());
    }

    @Test
    public void shouldReturn200ok2() {

        // setup
        Objects.requireNonNull(PROMO_REQUEST.getMain())
                .landingUrl(null)
                .rulesUrl(null);

        // act
        assertDoesNotThrow(() -> createPromoApiClient.promoCreateV2(PROMO_REQUEST).scheduleVoid().join());

        verify(promoService).createPromo(any(), any(), any());
    }

    @Test
    public void shouldReturn200okWithSsku() {

        // act and verify
        assertDoesNotThrow(() -> createPromoApiClient.promoCreateV2(PROMO_REQUEST_WITH_SSKU).scheduleVoid().join());
        verify(promoService).createPromo(any(), any(), any());
    }

    @Test
    public void shouldReturn400BadRequestIfValidationFailed() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(new PromoMainRequestParams());

        // act
        Exception e = assertThrows(Exception.class,
                () -> createPromoApiClient.promoCreateV2(request).schedule().join());

        // verify
        verifyNoInteractions(promoService);
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
        assertTrue(e.getCause().getMessage().contains("Validation type=SourceTypeValidationException"));
    }

    @Test
    public void shouldReturn400BadRequestIfPromoAlreadyExists() {

        // setup
        doThrow(DuplicateKeyException.class).when(promoService).createPromo(any(), any(), any());

        // act
        Exception e = assertThrows(Exception.class,
                () -> createPromoApiClient.promoCreateV2(PROMO_REQUEST).schedule().join());

        // verify
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
        assertTrue(e.getCause().getMessage().contains("Promo with this promo_id already exists"));
    }

    @Test
    public void shouldReturn500InternalServerErrorIfUnknownExceptionThrown() {

        // setup
        doThrow(RuntimeException.class).when(promoService).createPromo(any(), any(), any());

        // act
        Exception e = assertThrows(Exception.class,
                () -> createPromoApiClient.promoCreateV2(PROMO_REQUEST).schedule().join());

        // verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }
}
