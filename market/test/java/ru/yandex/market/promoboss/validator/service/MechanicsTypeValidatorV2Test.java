package ru.yandex.market.promoboss.validator.service;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.validator.exception.MechanicsTypeValidationException;
import ru.yandex.mj.generated.server.model.CheapestAsGift;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoMechanicsParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.Promocode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MechanicsTypeValidatorV2Test {

    private static final MechanicsTypeValidator validator = new MechanicsTypeValidator();

    @Test
    public void shouldThrowExceptionIfMechanicsTypeIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(new PromoMainRequestParams());

        // act and verify
        MechanicsTypeValidationException exception = assertThrows(MechanicsTypeValidationException.class,
                () -> validator.validate(request));

        assertEquals("No MechanicsType in the request", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfMechanicsTypeIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(null)
                );

        // act and verify
        MechanicsTypeValidationException exception = assertThrows(MechanicsTypeValidationException.class,
                () -> validator.validate(request));

        assertEquals("No MechanicsType in the request", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfMechanicsTypeDoesntMatchWithPromoType() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(ru.yandex.mj.generated.server.model.MechanicsType.CHEAPEST_AS_GIFT)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .promocode(new Promocode())
                );


        // act and verify
        MechanicsTypeValidationException exception = assertThrows(MechanicsTypeValidationException.class,
                () -> validator.validate(request));

        assertEquals(
                "MechanicsType doesn't match with type of promo mechanic in the request",
                exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfMechanicsDataUnnecessary() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(ru.yandex.mj.generated.server.model.MechanicsType.CHEAPEST_AS_GIFT)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                                .promocode(new Promocode())
                );

        // act and verify
        MechanicsTypeValidationException exception = assertThrows(MechanicsTypeValidationException.class,
                () -> validator.validate(request));

        assertEquals(
                "MechanicsType doesn't match with type of promo mechanic in the request",
                exception.getMessage());
    }

    @Test
    public void shouldNotThrowExceptionIfValid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(ru.yandex.mj.generated.server.model.MechanicsType.CHEAPEST_AS_GIFT)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfNotRequired() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT.getApiValue())
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
