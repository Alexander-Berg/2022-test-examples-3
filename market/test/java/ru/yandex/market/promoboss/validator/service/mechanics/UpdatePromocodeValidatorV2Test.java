package ru.yandex.market.promoboss.validator.service.mechanics;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.PromoDao;
import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.market.promoboss.service.mechanics.PromocodeService;
import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.Promocode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@ContextConfiguration(classes = {
        UpdatePromocodeValidator.class,
})
public class UpdatePromocodeValidatorV2Test {
    @Autowired
    UpdatePromocodeValidator updatePromocodeValidator;

    @MockBean
    PromocodeReservationService promocodeReservationService;

    @MockBean
    PromoDao promoDao;

    @MockBean
    PromocodeService promocodeService;

    private Promocode buildPromocode() {
        return new Promocode()
                .codeType(Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                .value(0)
                .code("code");
    }

    @Test
    void validate_ok() {
        var promocode = buildPromocode();
        doReturn(Optional.of(1L)).when(promoDao).getIdBySourcePromoId("promoId");
        doReturn(Optional.of(
                ru.yandex.market.promoboss.model.mechanics.Promocode.builder().code(promocode.getCode()).build())).when(
                promocodeService).getMechanicsDataByPromoId(1L);

        assertDoesNotThrow(() -> updatePromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
    }

    @Test
    void validate_codeChanged_throws() {
        var promocode = buildPromocode();
        doReturn(Optional.of(1L)).when(promoDao).getIdBySourcePromoId("promoId");
        doReturn(Optional.of(
                ru.yandex.market.promoboss.model.mechanics.Promocode.builder().code("OLDCODE").build())).when(
                promocodeService).getMechanicsDataByPromoId(1L);

        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> updatePromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Promocode code can't be changed", exception.getMessage());
    }

    @Test
    void validate_codeTypeNull_throws() {
        var promocode = buildPromocode();
        promocode.setCodeType(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> updatePromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field codeType is null", exception.getMessage());
    }

    @Test
    void validate_valueNull_throws() {
        var promocode = buildPromocode();
        promocode.setValue(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> updatePromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field value is null", exception.getMessage());
    }

    @Test
    void validate_codeNull_throws() {
        var promocode = buildPromocode();
        promocode.setCode(null);
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class,
                        () -> updatePromocodeValidator.validate(new PromoRequestV2().promoId("promoId"), promocode));
        assertEquals("Field code is blank", exception.getMessage());
    }
}
