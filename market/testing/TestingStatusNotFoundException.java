package ru.yandex.market.core.testing;

/**
 * @author zoom
 */
public class TestingStatusNotFoundException extends IllegalTestingStatus {
    public TestingStatusNotFoundException(long shopId, ShopProgram shopProgram) {
        super("Testing status not found. SHOP_ID: " + shopId + ". SHOP_PROGRAM: " + shopProgram);
    }
}
