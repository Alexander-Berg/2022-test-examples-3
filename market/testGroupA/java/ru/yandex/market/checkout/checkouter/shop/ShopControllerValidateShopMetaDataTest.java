package ru.yandex.market.checkout.checkouter.shop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.controllers.oms.ShopController;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class ShopControllerValidateShopMetaDataTest {
    /////////////// Test ShopMetaData validation ////////////////////////////////////////////////////////////

    /////////////// Test ShopMetaData validation ////////////////////////////////////////////////////////////
    @Test
    public void checkNullInnValidation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ShopController.validateShopMetaData(ShopServiceTestData.NULL_INN);
        });
    }

    @Test
    public void checkNullPhoneValidation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ShopController.validateShopMetaData(ShopServiceTestData.NULL_PHONE);
        });
    }

    @Test
    public void checkWrongInnValidation1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ShopController.validateShopMetaData(ShopServiceTestData.LONG_INN);
        });
    }

    @Test
    public void checkWrongInnValidation2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ShopController.validateShopMetaData(ShopServiceTestData.WRONG_FORMAT_INN);
        });
    }

    @Test
    public void checkCorrectValidation() {
        ShopController.validateShopMetaData(ShopServiceTestData.CORRECT);
    }

    @Test
    public void checkNonMarketPrepayValidation() {
        ShopController.validateShopMetaData(ShopServiceTestData.NON_MARKET_NULL_INN_AND_PHONE);
    }
}
