package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.currency.Money;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBannerContentRes;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webContentPromotionVideoBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentDesktopBidModifier;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentMobileBidModifier;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionVideoAdGroupControllerValidationTest extends
        ContentPromotionVideoAdGroupControllerTestBase {

    @Test
    public void addContentPromotionVideoAdGroup_NullAdGroup() {
        addAndExpectError(null, "[0]", CANNOT_BE_NULL.getCode());
    }

    @Test
    public void addContentPromotionVideoAdGroup_WithMinGeneralPrice_Success() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMinPrice(),
                clientInfo.getClient().getWorkCurrency());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        addAndCheckResponse(webContentPromotionVideoAdGroup);
    }

    @Test
    public void addContentPromotionVideoAdGroup_WithMaxGeneralPrice_Success() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMaxPrice(),
                clientInfo.getClient().getWorkCurrency());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        addAndCheckResponse(webContentPromotionVideoAdGroup);
    }

    @Test
    public void addContentPromotionVideoAdGroup_WithSmallerThanMinGeneralPrice_ValidationError() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMinPrice()
                        .subtract(BigDecimal.valueOf(0.01)),
                clientInfo.getClient().getWorkCurrency());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        addAndExpectError(webContentPromotionVideoAdGroup, "[0]." + WebContentPromotionAdGroup.Prop.GENERAL_PRICE,
                SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN.getCode());
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AdGroupWithBanner_NoVideoResource_Error() {
        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(null)
                .withContentResource(null);
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        addAndExpectError(webContentPromotionVideoAdGroup, "[0]." + WebContentPromotionAdGroup.Prop.BANNERS +
                "[0]." + WebContentPromotionBanner.Prop.CONTENT_RESOURCE + "." +
                WebContentPromotionBannerContentRes.Prop.CONTENT_ID, CANNOT_BE_NULL.getCode());
    }

    @Test
    public void addContentPromotionVideoAdGroup_WithGreaterThanMaxGeneralPrice_ValidationError() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMaxPrice()
                        .add(BigDecimal.valueOf(0.01)),
                clientInfo.getClient().getWorkCurrency());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        addAndExpectError(webContentPromotionVideoAdGroup, "[0]." + WebContentPromotionAdGroup.Prop.GENERAL_PRICE,
                SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX.getCode());
    }

    @Test
    public void addContentPromotionVideoAdGroupWithFeature_DesktopBidModifier_Success() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMinPrice(),
                clientInfo.getClient().getWorkCurrency());
        WebAdGroupBidModifiers bidModifiers = new WebAdGroupBidModifiers()
                .withDesktopBidModifier(randomPercentDesktopBidModifier());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue())
                        .withBidModifiers(bidModifiers);

        addAndCheckResponse(webContentPromotionVideoAdGroup);
    }

    @Test
    public void addContentPromotionVideoAdGroupWithFeature_MobileBidModifierLowPercent_Success() {
        Money price = Money.valueOf(clientInfo.getClient().getWorkCurrency().getCurrency().getMinPrice(),
                clientInfo.getClient().getWorkCurrency());
        WebAdGroupBidModifiers bidModifiers = new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentMobileBidModifier().withPercent(10));

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withGeneralPrice(price.bigDecimalValue().doubleValue())
                        .withBidModifiers(bidModifiers);

        addAndCheckResponse(webContentPromotionVideoAdGroup);
    }
}

