package ru.yandex.market.promoboss.validator.service.mechanics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.exception.ApiErrorException;
import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.Promocode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ContextConfiguration(classes = {
        CreatePromocodeValidator.class
})
class CreatePromocodeValidatorV2Test {
    @Autowired
    CreatePromocodeValidator createPromocodeValidator;

    @MockBean
    PromocodeReservationService promocodeReservationService;

    private Promocode buildPromocode() {
        return new Promocode()
                .codeType(Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                .value(0)
                .code("code");
    }

    @Test
    void validate_ok() {
        var promocode = buildPromocode();
        doReturn(true).when(promocodeReservationService).isPromocodeAvailable(any(), any(), any());
        assertDoesNotThrow(() -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
    }

    @Test
    void validate_promocodeOccupied() {
        var promocode = buildPromocode();
        doReturn(false).when(promocodeReservationService).isPromocodeAvailable(any(), any(), any());
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Promocode code is occupied by another promo", exception.getMessage());
    }

    @Test
    void validate_promocodeReservationServiceFailed() {
        var promocode = buildPromocode();
        doThrow(new ApiErrorException("Error")).when(promocodeReservationService)
                .isPromocodeAvailable(any(), any(), any());
        ApiErrorException exception =
                assertThrows(ApiErrorException.class,
                        () -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Error", exception.getMessage());
    }

    @Test
    void validate_codeTypeNull_throws() {
        var promocode = buildPromocode();
        promocode.setCodeType(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field codeType is null", exception.getMessage());
    }

    @Test
    void validate_valueNull_throws() {
        var promocode = buildPromocode();
        promocode.setValue(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field value is null", exception.getMessage());
    }

    @Test
    void validate_codeNull_throws() {
        var promocode = buildPromocode();
        promocode.setCode(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> createPromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field code is blank", exception.getMessage());
    }
}
