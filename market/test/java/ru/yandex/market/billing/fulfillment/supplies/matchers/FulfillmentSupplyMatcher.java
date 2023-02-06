package ru.yandex.market.billing.fulfillment.supplies.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupply;
import ru.yandex.market.core.billing.fulfillment.supplies.FulfillmentSupplyStatus;
import ru.yandex.market.core.fulfillment.FulfillmentSupplyType;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link FulfillmentSupply}.
 *
 * @author vbudnev
 */
public class FulfillmentSupplyMatcher {

    public static Matcher<FulfillmentSupply> hasId(long expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasType(FulfillmentSupplyType expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getType, expectedValue, "type")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasStatus(FulfillmentSupplyStatus expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasRequestId(String expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getServiceRequestId, expectedValue, "serviceRequestId")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasOperationType(FulfillmentOperationType expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getFulfillmentOperationType, expectedValue, "operationType")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasPalletCount(Long expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getPalletCount, expectedValue, "palletCount")
                .build();
    }

    public static Matcher<FulfillmentSupply> haxBoxCount(Long expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getBoxCount, expectedValue, "boxCount")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasXDocServiceId(Long expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getXDocServiceId, expectedValue, "xdocServiceId")
                .build();
    }

    public static Matcher<FulfillmentSupply> hasServiceId(Long expectedValue) {
        return MbiMatchers.<FulfillmentSupply>newAllOfBuilder()
                .add(FulfillmentSupply::getServiceId, expectedValue, "serviceId")
                .build();
    }
}
