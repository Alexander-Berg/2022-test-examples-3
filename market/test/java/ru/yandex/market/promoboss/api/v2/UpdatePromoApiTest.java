package ru.yandex.market.promoboss.api.v2;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.promoboss.exception.PromoNotFoundException;
import ru.yandex.market.promoboss.service.PromoService;
import ru.yandex.mj.generated.client.self_client.api.UpdatePromoApiClient;
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

public class UpdatePromoApiTest extends AbstractApiTest {

    @Autowired
    private UpdatePromoApiClient updatePromoApiClient;
    @MockBean
    private PromoService promoService;

    @BeforeEach
    public void resetMocks() {
        reset(promoService);
    }

    @Test
    public void shouldReturn200Ok() {

        // act
        assertDoesNotThrow(() -> updatePromoApiClient.promoUpdateV2(PROMO_REQUEST).scheduleVoid().join());

        // verify
        InOrder inOrder = Mockito.inOrder(promoService);
        inOrder.verify(promoService).updatePromo(any(), any(), any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldReturn400BadRequestIfPromoIdIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.CATEGORYIFACE)
                )
                .ssku(List.copyOf(SSKU));

        // act
        Exception e = assertThrows(Exception.class, () -> updatePromoApiClient.promoUpdateV2(request)
                .schedule()
                .join());

        // verify
        verifyNoInteractions(promoService);

        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());

        assertTrue(e.getCause().getMessage().contains("Validation type=PromoIdValidationException"));
    }

    @Test
    public void shouldReturn400BadRequestIfPromoIdIsEmpty() {

        // setup
        PROMO_REQUEST
                .promoId("");

        // act
        Exception e = assertThrows(Exception.class, () -> updatePromoApiClient.promoUpdateV2(PROMO_REQUEST)
                .schedule()
                .join());

        // verify
        verifyNoInteractions(promoService);

        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
        assertTrue(e.getCause().getMessage().contains("Validation type=PromoIdValidationException"));
    }

    @Test
    public void shouldReturn400BadRequestIfSourceTypeInvalid() {

        // setup
        Objects.requireNonNull(PROMO_REQUEST.getMain())
                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.ANAPLAN);

        // act
        Exception e = assertThrows(Exception.class, () -> updatePromoApiClient.promoUpdateV2(PROMO_REQUEST)
                .schedule()
                .join());

        // verify
        verifyNoInteractions(promoService);

        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
        assertTrue(e.getCause().getMessage().contains("Validation type=SourceTypeValidationException"));
    }

    @Test
    public void shouldReturn404NotFound() {

        // setup
        doThrow(new PromoNotFoundException()).when(promoService).updatePromo(any(), any(), any());

        // act
        Exception e = assertThrows(Exception.class, () -> updatePromoApiClient.promoUpdateV2(PROMO_REQUEST)
                .schedule()
                .join());

        // verify
        verify(promoService).updatePromo(any(), any(), any());

        assertEquals(HttpStatus.NOT_FOUND.value(), ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }

    @Test
    public void shouldReturn500InternalServerErrorIfExceptionOccurred() {

        // setup
        doThrow(new RuntimeException("Some error")).when(promoService).updatePromo(any(), any(), any());

        // act
        Exception e = assertThrows(Exception.class, () -> updatePromoApiClient.promoUpdateV2(PROMO_REQUEST)
                .schedule()
                .join());

        // verify
        verify(promoService).updatePromo(any(), any(), any());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }
}
