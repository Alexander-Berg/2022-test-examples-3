package ru.yandex.market.yml.parser;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.yml.parser.model.Category;
import ru.yandex.market.yml.parser.model.Currency;
import ru.yandex.market.yml.parser.model.DeliveryOption;
import ru.yandex.market.yml.parser.model.Shop;
import ru.yandex.market.yml.validation.ConstraintViolationsException;
import ru.yandex.market.yml.validation.ShopValidator;

public class ShopValidatorTest {

    ShopValidator shopValidator = new ShopValidator();

    @Test
    void testValidateFailShop() {
        Assertions.assertThrows(
                ConstraintViolationsException.class,
                () -> shopValidator.validate(Shop.newBuilder().build()));
    }

    @Test
    void testFailValidateShopWithoutName() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withName(null).build()));
    }

    @Test
    void testFailValidateShopWithoutCompany() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withCompany(null).build()));
    }

    @Test
    void testFailValidateShopWithoutUrl() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withUrl(null).build()));
    }

    @Test
    void testFailValidateShopWithoutCurrency() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withCurrencies(null).build()));
    }

    @Test
    void testFailValidateShopWithoutCategories() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withCategories(null).build()));
    }

    @Test
    void testFailOnEmptyDeliveryOptions() {
        Assertions.assertThrows(
                ConstraintViolationsException.class, () ->
                        shopValidator.validate(successShop().withDeliveryOptions(Collections.emptyList()).build()));
    }

    @Test
    void testValidateShopSuccess() {
        shopValidator.validate(successShop().build());
    }

    private Shop.Builder successShop() {
        return Shop.newBuilder()
                .withName("MMM shop")
                .withCompany("MMM 2020")
                .withUrl("https://mmm2020.ru")
                .withDeliveryOptions(Collections.singletonList(DeliveryOption.newBuilder().build()))
                .withCurrencies(Collections.singletonList(Currency.newBuilder().build()))
                .withCategories(Collections.singletonList(Category.newBuilder().build()));

    }
}
