package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmGeoproductAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmAdGroupControllerPricesTest extends CpmAdGroupControllerTestBase {

    @Autowired
    private ClientService clientService;

    @Test
    public void addAdGroupWithRetargetingsInManualStrategyCampaign() {
        double commonPrice = 50.0;
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd.withPriceContext(commonPrice)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        Long adGroupId = adGroups.get(0).getId();

        List<Retargeting> retargetings = findRetargetings(adGroupId);
        assertThat("должен быть добавлен один ретаргетинг", retargetings, hasSize(1));
        assertThat("цена у ретаргетинга не совпадает с ожидаемой", moneyOf(retargetings.get(0).getPriceContext()),
                equalTo(moneyOf(commonPrice)));
    }

    @Test
    public void addAdGroupWithKeywordsInManualStrategyCampaign() {
        double commonPrice = 50.0;
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withGeneralPrice(commonPrice)
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("цена у фразы не совпадает с ожидаемой", moneyOf(keywords.get(0).getPriceContext()),
                equalTo(moneyOf(commonPrice)));
    }

    @Test
    public void defaultPriceIsSetWhenAddAdGroupWithKeywordsWithoutPrice() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withGeneralPrice(null)
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));

        Currency workCurrency = clientService.getWorkCurrency(clientId);
        assertThat("цена у фразы не совпадает с ожидаемой", moneyOf(keywords.get(0).getPriceContext()),
                equalTo(moneyOf(workCurrency.getMinCpmPrice())));
    }

    @Test
    public void addAdGroupsWithoutPriceInAutobudgetCampaign() {
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(activeCpmBannerCampaign(clientId, clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaign.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaign.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Keyword> keywords = findKeywords(campaign.getCampaignId());
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("цена у фразы не совпадает с ожидаемой", keywords.get(0).getPriceContext(),
                nullValue());
        assertThat("приоритет автобюджета ожидаемый", keywords.get(0).getAutobudgetPriority(),
                is(DEFAULT_AUTOBUDGET_PRIORITY));
    }

    @Test
    public void addCpmGeoproductAdGroupWithoutPriceInAutobudgetCampaign() {
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(activeCpmBannerCampaign(clientId, clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaign.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd.withPriceContext(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaign.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Retargeting> retargetings = findRetargetings(adGroups.get(0).getId());
        assertThat("должен быть добавлен один ретаргетинг", retargetings, hasSize(1));
        assertThat("цена у ретаргетинга должна совпадать с ожидаемой", moneyOf(retargetings.get(0).getPriceContext()),
                equalTo(moneyOf(CurrencyRub.getInstance().getMinCpmPrice())));
    }

    @Test
    public void updateCpmGeoproductAdGroupWithoutPriceInAutobudgetCampaign() {
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(activeCpmBannerCampaign(clientId, clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultCpmGeoproductAdGroup(clientInfo);
        WebCpmAdGroupRetargeting retargetingForUpdate = createRetargeting(adGroupInfo);

        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaign.getCampaignId())
                .withRetargetings(singletonList(retargetingForUpdate.withPriceContext(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaign.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Retargeting> retargetings = findRetargetings(adGroups.get(0).getId());
        assertThat("должен быть добавлен один ретаргетинг", retargetings, hasSize(1));
        assertThat("цена у ретаргетинга должна совпадать с ожидаемой", moneyOf(retargetings.get(0).getPriceContext()),
                equalTo(moneyOf(CurrencyRub.getInstance().getMinCpmPrice())));
    }

    @Test
    public void addCpmGeoproductAdGroupLowPriceManualStrategySuccess() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaign.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd.withPriceContext(5.)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaign.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        List<Retargeting> retargetings = findRetargetings(adGroups.get(0).getId());
        assertThat("должен быть добавлен один ретаргетинг", retargetings, hasSize(1));
        assertThat("цена у ретаргетинга должна совпадать с ожидаемой", moneyOf(retargetings.get(0).getPriceContext()),
                equalTo(moneyOf(CurrencyRub.getInstance().getMinCpmPrice())));
    }

    @Test
    public void addCpmGeoproductAdGroupLowPriceManualStrategyError() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaign.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd.withPriceContext(3.)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaign.getCampaignId(), true, true, false, null);
        assertThat(webResponse.isSuccessful(), is(false));
    }

    @Test
    public void updateAdGroupUpdateRetargeting() {
        AdGroupInfo cpmBannerAdGroup =
                steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo, CriterionType.KEYWORD);
        WebCpmAdGroupRetargeting retargeting = createRetargeting(cpmBannerAdGroup);

        double priceContext = 150;
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(cpmBannerAdGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargeting.withPriceContext(priceContext)));
        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(webCpmAdGroup), campaignInfo.getCampaignId(), false, true, false, null);
        checkResponse(webResponse);

        List<Retargeting> retargetings = findRetargetings(cpmBannerAdGroup.getAdGroupId());
        assertThat("в группе должен быть один ретаргетинг", retargetings, hasSize(1));
        assertThat("цена у ретаргетинга не совпадает с ожидаемой", moneyOf(retargetings.get(0).getPriceContext()),
                equalTo(moneyOf(priceContext)));
    }

    @Test
    public void updateAdGroupAddKeyword() {
        AdGroupInfo cpmBannerAdGroup =
                steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(campaignInfo);
        double priceContext = 150;
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(cpmBannerAdGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withGeneralPrice(priceContext)
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(webCpmAdGroup), campaignInfo.getCampaignId(), false, true, false, null);
        checkResponse(webResponse);

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));
        assertThat("цена у фразы не совпадает с ожидаемой", moneyOf(keywords.get(0).getPriceContext()),
                equalTo(moneyOf(priceContext)));
    }

    @Test
    public void defaultPriceIsSetWhenUpdateAdGroupModifyKeywordWithoutPrice() {
        AdGroupInfo cpmBannerAdGroup =
                steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo, CriterionType.KEYWORD);
        KeywordInfo keyword = steps.keywordSteps().createKeyword(cpmBannerAdGroup, keywordForCpmBanner());
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(cpmBannerAdGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withGeneralPrice(null)
                        .withKeywords(singletonList(randomPhraseKeyword(keyword.getId())));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(webCpmAdGroup), campaignInfo.getCampaignId(), false, true, false, null);
        checkResponse(webResponse);

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", keywords, hasSize(1));

        Currency workCurrency = clientService.getWorkCurrency(clientId);
        assertThat("цена у фразы не совпадает с ожидаемой", moneyOf(keywords.get(0).getPriceContext()),
                equalTo(moneyOf(workCurrency.getMinCpmPrice())));
    }

}
