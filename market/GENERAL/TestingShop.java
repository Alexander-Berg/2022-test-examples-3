package ru.yandex.market.core.moderation;

import java.util.Objects;

/**
 * Магазин, находящийся на проверке.
 *
 * @author Vadim Lyalin
 */
public class TestingShop {
    /**
     * <code>DATASOURCES_IN_TESTING.ID</code>.
     */
    private final long testingId;

    /**
     * Идентификатор магазина.
     */
    private final long shopId;

    public TestingShop(long testingId, long shopId) {
        this.testingId = testingId;
        this.shopId = shopId;
    }

    public long getTestingId() {
        return testingId;
    }

    public long getShopId() {
        return shopId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestingShop that = (TestingShop) o;
        return testingId == that.testingId &&
                shopId == that.shopId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testingId, shopId);
    }

    @Override
    public String toString() {
        return "TestingShop{" +
                "testingId=" + testingId +
                ", shopId=" + shopId +
                '}';
    }
}
