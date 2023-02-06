package ru.yandex.market.promoboss.service.mechanics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeCheckResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.promoboss.exception.ApiErrorException;
import ru.yandex.market.promoboss.exception.PromocodeReservationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = {PromocodeReservationService.class})
class PromocodeReservationServiceTest {
    private final static String CODE = "CODE";
    private final static Long promoStartAt = 1L;
    private final static Long promoEndAt = 2L;

    @MockBean
    MarketLoyaltyClient marketLoyaltyClient;

    @Autowired
    PromocodeReservationService promocodeService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isPromocodeAvailableTest_ok(Boolean isPromocodeAvailable) {
        when(marketLoyaltyClient.checkPromocode(any())).thenReturn(
                new PromocodeCheckResponse(isPromocodeAvailable, null));

        Assertions.assertEquals(
                isPromocodeAvailable,
                promocodeService.isPromocodeAvailable(CODE, promoStartAt, promoEndAt)
        );
    }

    @Test
    void isPromocodeAvailableTest_apiEmptyResponse_throws() {
        when(marketLoyaltyClient.checkPromocode(any())).thenReturn(null);

        Assertions.assertThrows(ApiErrorException.class,
                () -> promocodeService.isPromocodeAvailable(CODE, promoStartAt, promoEndAt));
    }

    @Test
    void isPromocodeAvailableTest_apiError_throws() {
        when(marketLoyaltyClient.checkPromocode(any())).thenThrow(new MarketLoyaltyException("error"));

        Assertions.assertThrows(ApiErrorException.class,
                () -> promocodeService.isPromocodeAvailable(CODE, promoStartAt, promoEndAt));
    }

    @Test
    void reservePromocode_ok() {
        when(marketLoyaltyClient.reservePromocode(CODE)).thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> promocodeService.reservePromocode(CODE));
    }

    @Test
    void reservePromocode_apiError_throws() {
        when(marketLoyaltyClient.reservePromocode(CODE)).thenThrow(new MarketLoyaltyException("error"));

        Assertions.assertThrows(PromocodeReservationException.class, () -> promocodeService.reservePromocode(CODE));
    }
}
