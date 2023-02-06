package ru.yandex.market.yml.parser;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.yml.parser.model.Category;
import ru.yandex.market.yml.parser.model.Currency;
import ru.yandex.market.yml.parser.model.DeliveryOption;
import ru.yandex.market.yml.parser.model.Offer;
import ru.yandex.market.yml.parser.model.Price;
import ru.yandex.market.yml.parser.model.enums.CurrencyCode;
import ru.yandex.market.yml.validation.ConstraintViolationsException;
import ru.yandex.market.yml.validation.OfferValidator;

public class OfferValidatorTest {

    OfferValidator offerValidator = new OfferValidator();

    @Test
    void testValidOffer() {
        offerValidator.validate(validOfferBuilder().build());
    }

    @Test
    void testFailValidateOfferWithoutId() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withId(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutType() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withType(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutModel() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withModel(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutVendor() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withVendor(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutUrl() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withUrl(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutPrice() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withPrice(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutCurrencyId() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withCurrency(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithoutCategory() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withCategory(null)
                                    .build());
                });
    }

    @Test
    void testFailValidateOfferWithNotUniqueDeliveryOptions() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () -> {
                    offerValidator.validate(
                            validOfferBuilder()
                                    .withDeliveryOptions(Arrays.asList(
                                            DeliveryOption.newBuilder().build(),
                                            DeliveryOption.newBuilder().build())
                                    ).build());
                });
    }

    private Offer.Builder validOfferBuilder() {
        return Offer.newBuilder()
                .withId("123")
                .withName("Supplier name")
                .withType("supplier type")
                .withVendor("supplier vendor")
                .withVendorCode("suppplier vendor code")
                .withUrl("http://supplier.ru")
                .withPrice(Price.newBuilder().withFrom(true).withValue(BigDecimal.valueOf(10.0d)).build())
                .withCurrency(Currency.newBuilder()
                    .withId(CurrencyCode.RUR)
                        .build())
                .withModel("model 1")
                .withCategory(Category.newBuilder()
                        .withId(1L)
                        .build())
                .withType("mmm type");
    }
}
