package ru.yandex.market.admin.service.remote.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.admin.ui.model.supplier.UiOrderItemBillingCorrection;
import ru.yandex.market.mbi.util.MbiMatchers;

public class UiOrderItemBillingCorrectionMatchers {
    public static Matcher<UiOrderItemBillingCorrection> hasAmount(Integer expectedValue) {
        return MbiMatchers.<UiOrderItemBillingCorrection>newAllOfBuilder()
                .add(UiOrderItemBillingCorrection::getAmount, expectedValue, "amount")
                .build();
    }

    public static Matcher<UiOrderItemBillingCorrection> hasOrderId(Long expectedValue) {
        return MbiMatchers.<UiOrderItemBillingCorrection>newAllOfBuilder()
                .add(UiOrderItemBillingCorrection::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<UiOrderItemBillingCorrection> hasShopSku(String expectedValue) {
        return MbiMatchers.<UiOrderItemBillingCorrection>newAllOfBuilder()
                .add(UiOrderItemBillingCorrection::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<UiOrderItemBillingCorrection> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<UiOrderItemBillingCorrection>newAllOfBuilder()
                .add(UiOrderItemBillingCorrection::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<UiOrderItemBillingCorrection> hasComment(String expectedValue) {
        return MbiMatchers.<UiOrderItemBillingCorrection>newAllOfBuilder()
                .add(UiOrderItemBillingCorrection::getComment, expectedValue, "comment")
                .build();
    }


}
