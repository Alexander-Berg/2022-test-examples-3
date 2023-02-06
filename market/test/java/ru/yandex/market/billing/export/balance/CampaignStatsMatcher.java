package ru.yandex.market.billing.export.balance;

import org.hamcrest.Matcher;

import ru.yandex.market.core.order.payment.BankOrderInfo;
import ru.yandex.market.mbi.util.MbiMatchers;

public class CampaignStatsMatcher {

    public static Matcher<CampaignStatsDto> hasCampaignId(Long expectedValue) {
        return MbiMatchers.<CampaignStatsDto>newAllOfBuilder()
                .add(CampaignStatsDto::getCampaignId, expectedValue, "campaignId")
                .build();
    }

    public static Matcher<CampaignStatsDto> hasSum(Long expectedValue) {
        return MbiMatchers.<CampaignStatsDto>newAllOfBuilder()
                .add(CampaignStatsDto::getSum, expectedValue, "sum")
                .build();
    }

    public static Matcher<CampaignStatsDto> hasTestShop(Boolean expectedValue) {
        return MbiMatchers.<CampaignStatsDto>newAllOfBuilder()
                .add(CampaignStatsDto::getTestShop, expectedValue, "testShop")
                .build();
    }
}
