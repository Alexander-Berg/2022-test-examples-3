package ru.yandex.market.admin.service.remote.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.admin.ui.model.supplier.UIFfSupplyItemCorrection;
import ru.yandex.market.mbi.util.MbiMatchers;

public class UIFfSupplyItemCorrectionMatchers {
    public static Matcher<UIFfSupplyItemCorrection> hasAmount(Integer expectedValue) {
        return MbiMatchers.<UIFfSupplyItemCorrection>newAllOfBuilder()
                .add(UIFfSupplyItemCorrection::getAmount, expectedValue, "amount")
                .build();
    }

    public static Matcher<UIFfSupplyItemCorrection> hasComment(String expectedValue) {
        return MbiMatchers.<UIFfSupplyItemCorrection>newAllOfBuilder()
                .add(UIFfSupplyItemCorrection::getComment, expectedValue, "comment")
                .build();
    }

    public static Matcher<UIFfSupplyItemCorrection> hasShopSku(String expectedValue) {
        return MbiMatchers.<UIFfSupplyItemCorrection>newAllOfBuilder()
                .add(UIFfSupplyItemCorrection::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<UIFfSupplyItemCorrection> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<UIFfSupplyItemCorrection>newAllOfBuilder()
                .add(UIFfSupplyItemCorrection::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<UIFfSupplyItemCorrection> hasSupplyId(Long expectedValue) {
        return MbiMatchers.<UIFfSupplyItemCorrection>newAllOfBuilder()
                .add(UIFfSupplyItemCorrection::getSupplyId, expectedValue, "supplyId")
                .build();
    }
}
