package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerAddWithGeoTest extends TextAdGroupControllerTestBase {

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

    @Test
    public void addAdgroupWithGeo_RussiaToRussia_RussiaAndCrimea() {
        WebTextAdGroup
                requestAdGroup = addAdGroupWithGeoAndCheckResult(singletonList(RUSSIA_REGION_ID), RUSSIA_REGION_ID);

        addWebAdGroup(requestAdGroup);

        checkResult("Ожидаем Россию и Крым, но значения не совпадают",
                Arrays.asList(RUSSIA_REGION_ID, CRIMEA_REGION_ID));
    }

    @Test
    public void addAdgroupWithGeo_RussiaToUkraine_Russia() {
        WebTextAdGroup
                requestAdGroup = addAdGroupWithGeoAndCheckResult(singletonList(RUSSIA_REGION_ID), UKRAINE_REGION_ID);

        addWebAdGroup(requestAdGroup);

        checkResult("Ожидаем только Россию, но значения не совпадают", singletonList(RUSSIA_REGION_ID));
    }

    @Test
    public void addAdgroupWithGeo_UkraineToUkraine_UkraineAndCrimea() {
        WebTextAdGroup requestAdGroup =
                addAdGroupWithGeoAndCheckResult(singletonList(UKRAINE_REGION_ID), UKRAINE_REGION_ID);

        addWebAdGroup(requestAdGroup);
        checkResult("Ожидаем Украину и Крым, но значения не совпадают",
                Arrays.asList(UKRAINE_REGION_ID, CRIMEA_REGION_ID));
    }

    @Test
    public void addAdgroupWithGeo_UkraineToRussia_Ukraine() {
        WebTextAdGroup
                requestAdGroup = addAdGroupWithGeoAndCheckResult(singletonList(UKRAINE_REGION_ID), RUSSIA_REGION_ID);

        addWebAdGroup(requestAdGroup);

        checkResult("Ожидаем только Украину, но значения не совпадают", singletonList(UKRAINE_REGION_ID));
    }

    private void checkResult(String comment, List<Long> expectedGeoIds) {
        List<AdGroup> addedAdGroups = findAdGroups();
        assertThat(comment, addedAdGroups.get(0).getGeo(), equalTo(expectedGeoIds));
    }

    private String geoToString(List<Long> geoIds) {
        return String.join(",", mapList(geoIds, String::valueOf));
    }

    private WebTextAdGroup addAdGroupWithGeoAndCheckResult(List<Long> geoIds, Long clientCountryRegionId) {
        String geo = geoToString(geoIds);
        clientInfo = steps.clientSteps().createClient(defaultClient().withCountryRegionId(clientCountryRegionId));
        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        clientId = campaignInfo.getClientId();
        shard = campaignInfo.getShard();
        WebTextAdGroup requestAdGroup = new WebTextAdGroup()
                .withCampaignId(campaignInfo.getCampaignId())
                .withName(randomAlphabetic(5))
                .withMinusKeywords(singletonList(randomAlphabetic(5)))
                .withGeo(geo);
        WebBanner requestBanner = randomTitleWebTextBanner(null);
        WebKeyword requestKeyword = randomPhraseKeyword(null);

        requestAdGroup.withKeywords(singletonList(requestKeyword))
                .withBanners(singletonList(requestBanner));

        setAuthData();
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );

        return requestAdGroup;
    }

    private void addWebAdGroup(WebTextAdGroup requestAdGroup) {
        WebResponse response = controller.saveTextAdGroup(singletonList(requestAdGroup),
                campaignInfo.getCampaignId(), true, false, false, null, null);
        checkResponse(response);
    }


}
