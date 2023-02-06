package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionVideoAdGroupControllerPricesTest extends ContentPromotionVideoAdGroupControllerTestBase {

    @Autowired
    private ClientService clientService;

    @Test
    public void saveContentPromotionVideoAdGroup_GeneralPriceIsSet_PriceIsSet() {
        double commonPrice = 50.0;
        WebContentPromotionAdGroup requestAdGroup = randomNameWebContentPromotionVideoAdGroup(null,
                campaignInfo.getCampaignId())
                .withGeneralPrice(commonPrice)
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(), true,
                        true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("должна совпадать с ожидаемой", moneyOf(keywords.get(0).getPrice()),
                is(moneyOf(commonPrice)));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_GeneralPriceIsNotSet_PriceMustBeDefault() {
        WebContentPromotionAdGroup requestAdGroup = randomNameWebContentPromotionVideoAdGroup(null,
                campaignInfo.getCampaignId())
                .withGeneralPrice(null)
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(), true,
                        true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));

        Currency workCurrency = clientService.getWorkCurrency(clientId);
        assertThat("должна совпадать с ожидаемой", moneyOf(keywords.get(0).getPrice()),
                is(moneyOf(workCurrency.getDefaultPrice())));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AutobudgetCampaign_PriceIsNotSet() {
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(activeContentPromotionCampaign(clientId, clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        WebContentPromotionAdGroup requestAdGroup = randomNameWebContentPromotionVideoAdGroup(null,
                campaign.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true,
                        false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaign.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords(campaign.getCampaignId());
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("должна совпадать с ожидаемой", keywords.get(0).getPrice(),
                nullValue());
        assertThat("приоритет автобюджета ожидаемый", keywords.get(0).getAutobudgetPriority(),
                is(DEFAULT_AUTOBUDGET_PRIORITY));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddKeywordWithPrice_PriceIsSet() {
        AdGroupInfo contentPromotionVideoAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(
                campaignInfo, ContentPromotionAdgroupType.VIDEO);
        double price = 150;
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(contentPromotionVideoAdGroup.getAdGroupId(),
                        campaignInfo.getCampaignId())
                        .withGeneralPrice(price)
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(singletonList(webContentPromotionVideoAdGroup),
                        campaignInfo.getCampaignId(), false, true, false, null);
        checkResponse(webResponse);

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("должна совпадать с ожидаемой", moneyOf(keywords.get(0).getPrice()),
                is(moneyOf(price)));
    }
}
