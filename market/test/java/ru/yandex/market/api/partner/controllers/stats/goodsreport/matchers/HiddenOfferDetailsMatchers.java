package ru.yandex.market.api.partner.controllers.stats.goodsreport.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.hiddenoffers.model.HiddenOfferDetails;
import ru.yandex.market.mbi.util.MbiMatchers;

public class HiddenOfferDetailsMatchers {

    public static Matcher<HiddenOfferDetails> hasDatasourceId(long datasourceId) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getDatasourceId, datasourceId, "datasourceId")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasShopSku(String shopSku) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getShopSku, shopSku, "shopSku")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasOfferName(String offerName) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getOfferName, offerName, "offerName")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasCategoryName(String categoryName) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getCategoryName, categoryName, "categoryName")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasReason(String reason) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getReason, reason, "reason")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasReasonCode(String reasonCode) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getReasonCode, reasonCode, "reasonCode")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasSubreason(String subreason) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getSubreason, subreason, "subreason")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasSubreasonCode(String subreasonCode) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getSubreasonCode, subreasonCode, "subreasonCode")
                .build();
    }

    public static Matcher<HiddenOfferDetails> hasDetails(String details) {
        return MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                .add(HiddenOfferDetails::getDetails, details, "details")
                .build();
    }
}
