package ru.yandex.market.billing.fulfillment.supplies.matchers;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.core.billing.model.WithdrawItem;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.core.fulfillment.model.StockType;
import ru.yandex.market.core.fulfillment.model.Warehouse;
import ru.yandex.market.mbi.util.MbiMatchers;

public class WithdrawItemMatcher {

    public static Matcher<WithdrawItem> hasWithdrawId(long expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getWithdrawId, expectedValue, "withdrawId")
                .build();
    }

    public static Matcher<WithdrawItem> hasServiceRequestId(String expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getServiceRequestId, expectedValue, "serviceRequestId")
                .build();
    }

    public static Matcher<WithdrawItem> hasWarehouse(Warehouse expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getWarehouseId, expectedValue.getId(), "warehouseId")
                .build();
    }

    public static Matcher<WithdrawItem> hasStockType(StockType expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getStockType, expectedValue, "stockType")
                .build();
    }

    public static Matcher<WithdrawItem> hasSupplierId(long expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<WithdrawItem> betweenFinishedTrantime(LocalDate dateFromInclusive, LocalDate dateToExclusive) {
        return MbiMatchers.satisfy(withdrawItem -> (
                withdrawItem.getFinishedTrantime().equals(dateFromInclusive.atStartOfDay())
                        || withdrawItem.getFinishedTrantime().isAfter(dateFromInclusive.atStartOfDay()))
                && withdrawItem.getFinishedTrantime().isBefore(dateToExclusive.atStartOfDay()));
    }

    public static Matcher<WithdrawItem> hasShopSku(String expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<WithdrawItem> hasCount(int expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getCount, expectedValue, "count")
                .build();
    }

    public static Matcher<WithdrawItem> hasName(String expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<WithdrawItem> hasMarketName(String expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getMarketName, expectedValue, "marketName")
                .build();
    }

    public static Matcher<WithdrawItem> hasSupplyPrice(BigDecimal expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getSupplyPrice, expectedValue, "supplyPrice")
                .build();
    }


    public static Matcher<WithdrawItem> hasWithdrawOperationType(FulfillmentOperationType expectedValue) {
        return MbiMatchers.<WithdrawItem>newAllOfBuilder()
                .add(WithdrawItem::getFulfillmentOperationType, expectedValue, "operationType")
                .build();
    }
}
