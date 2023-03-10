package ru.yandex.direct.web.entity.frontpage.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.region.RegionDesc;
import ru.yandex.direct.core.entity.region.validation.RegionDefectParams;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.currencies.CurrencyChf;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.frontpage.model.CpmYndxFrontpagePriceWarningsResponse;
import ru.yandex.direct.web.entity.frontpage.model.CpmYndxFrontpageWarningsRequest;
import ru.yandex.direct.web.entity.frontpage.model.FrontpageWarningsGetItem;
import ru.yandex.direct.web.validation.model.WebDefect;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.BROWSER_NEW_TAB;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds.Regions.GEO_FRONTPAGE_BROWSER_NEW_TAB_IGNORED_REGIONS_DUE_TO_PRICE;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds.Regions.GEO_FRONTPAGE_DESKTOP_IGNORED_REGIONS_DUE_TO_PRICE;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds.Regions.GEO_FRONTPAGE_MOBILE_IGNORED_REGIONS_DUE_TO_PRICE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmYndxFrontpageWarningControllerAdGroupsTest {
    @Autowired
    private Steps steps;
    @Autowired
    private CpmYndxFrontpageWarningsController controller;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    @Autowired
    private DirectWebAuthenticationSource authenticationSource;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true);
        setAuthData(clientInfo);
    }

    private void setAuthData(ClientInfo clientInfo) {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        UserInfo userInfo = clientInfo.getChiefUserInfo();
        User user = userInfo.getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }

    @Test
    // ?????????????????????? ???????? ?????? ???? ?????? ?????????????? ???? ???????????????? ?? ?????????? 1.8
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoProvidedDesktop() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE), false, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), 1.5,ImmutableList.of(1.5),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ???? ?????? ?????????????? ???? ?????????????????? ?????????????????????? ?? ?????????? 1.8
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ????????????????????, ?????? ???????? ????????, ?????? ??????????????????????. ?????? ??????????????????.
    public void validateAdGroupGeoProvidedMobile() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), false, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), 1.3, ImmutableList.of(1.3),
                emptyList(), emptyList(), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ???? ?????? ?????????????? ???? ?????????? ?????????????? ?? ???????????????? ?? ?????????? 1.8
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoProvidedBrowserNewTab() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(BROWSER_NEW_TAB), false, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), 1.5, ImmutableList.of(1.5),
                emptyList(), emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)));
    }


    @Test
    // ?????????????????????? ???????? ?????? ???????????? ?????? ?????????????? ???? ???????????????? ?? ?????????? 1.8 ?? ???????????????????? (1.4)
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ???????????????????? ???????????? (????) ???????? ????????, ?????? ?????????????????????? (1.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoProvidedAutoBudgetDesktop() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false)
                .withStrategyAutoPrice(BigDecimal.valueOf(1.4));
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(RUSSIA_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE), true, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), 1., ImmutableList.of(1.),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ???????????? ?????? ?????????????? ???? ?????????????????? ?????????????????????? ?? ?????????? 1.8 ?? ???????????????????? (1.2)
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ???????????????????? ???????????? (????) ???????? ????????, ?????? ?????????????????????? (1.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoProvidedAutoBudgetMobile() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false)
                .withStrategyAutoPrice(BigDecimal.valueOf(1.2));
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(RUSSIA_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), true, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), .7, ImmutableList.of(.7),
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)),  emptyList(),  emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ???????????? ?????? ?????????????? ???? ?????????? ?????????????? ?? ???????????????? ?? ?????????? 1.8 ?? ???????????????????? (1.4)
    // ?????? ?????????? ???????????? - ??????????????
    // ?????? ?????????????????????? ???????????? (???? ?? ??????) ???????? ????????, ?????? ?????????????????????? (1.5 ?? 4.2) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoProvidedAutoBudgetBrowserNewTab() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false)
                .withStrategyAutoPrice(BigDecimal.valueOf(1.4));
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), String.valueOf(RUSSIA_REGION_ID), null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(BROWSER_NEW_TAB), true, warningItems,
                ImmutableList.of(ImmutableList.of(UKRAINE_REGION_ID)), 1., ImmutableList.of(1.),
                emptyList(),  emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)));
    }


    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ???????????????? ?? ?????????? 1.8 ?????? ??????????????
    // ?????? ?????????? ???????????? - ????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoFromDbDesktop() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), null, null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)), 1.5, ImmutableList.of(1.5),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ?????????????????? ?????????????????????? ?? ?????????? 1.8 ?????? ??????????????
    // ?????? ?????????? ???????????? - ????
    // ?????? ????????????????????, ?????? ???????? ????????, ?????? ??????????????????????. ?????? ??????????????????.
    public void validateAdGroupGeoFromDbMobile() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), null, null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)), 1.3, ImmutableList.of(1.3),
                emptyList(), emptyList(), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ?????????? ?????????????? ?? ???????????????? ?? ?????????? 1.8 ?????? ??????????????
    // ?????? ?????????? ???????????? - ????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateAdGroupGeoFromDbBrowserNewTab() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(new FrontpageWarningsGetItem(
                BigDecimal.valueOf(1.8), null, null
        ));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(BROWSER_NEW_TAB), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)), 1.5, ImmutableList.of(1.5),
                emptyList(), emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)));
    }

    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ???????????????? ?? ???????????? 1.8 ?? 3.0 ?????? ??????????????
    // ?????? ?????????? ?????????????? - ???? ?? ????????????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateTwoAdGroupsGeoFromDbDesktop() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(
                new FrontpageWarningsGetItem(BigDecimal.valueOf(1.8), null, null),
                new FrontpageWarningsGetItem(BigDecimal.valueOf(3.), null, null));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(MOSCOW_REGION_ID)),
                1.5, ImmutableList.of(1.5, 2.5),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ?????????????????? ?????????????????????? ?? ???????????? 1.8 ?? 3.0 ?????? ??????????????
    // ?????? ?????????? ???????????? - ???? ?? ????????????
    // ?????? ????????????????????, ?????? ???????? ????????, ?????? ??????????????????????. ?????? ??????????????????.
    public void validateTwoAdGroupsGeoFromDbMobile() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(
                new FrontpageWarningsGetItem(BigDecimal.valueOf(1.8), null, null),
                new FrontpageWarningsGetItem(BigDecimal.valueOf(3.), null, null));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(MOSCOW_REGION_ID)),0.08, ImmutableList.of(1.3, 0.08),
                emptyList(), emptyList(), emptyList());
    }

    @Test
    // ?????????????????????? ???????? ?????? ?????????????? ???? ?????????? ?????????????? ?? ???????????????? ?? ???????????? 1.8 ?? 3.0 ?????? ??????????????
    // ?????? ?????????? ?????????????? - ???? ?? ????????????
    // ?????? ???????????????????? ???? (????????????) ???????? ????????, ?????? ?????????????????????? (2.5) - ?????????????? ?????????????? ?????? ????????????.
    public void validateTwoAdGroupsGeoFromDbBrowserNewTab() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(false)
                .withUseDbAdGroups(false);
        List<FrontpageWarningsGetItem> warningItems = ImmutableList.of(
                new FrontpageWarningsGetItem(BigDecimal.valueOf(1.8), null, null),
                new FrontpageWarningsGetItem(BigDecimal.valueOf(3.), null, null));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(BROWSER_NEW_TAB), false, warningItems,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(MOSCOW_REGION_ID)),1.5, ImmutableList.of(1.5, 2.5),
                emptyList(), emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)));
    }

    private void getFrontpagePriceWarningsTest(CpmYndxFrontpageWarningsRequest request,
                                               Set<FrontpageCampaignShowType> campaignShowTypes,
                                               Boolean isAutoBudget,
                                               List<FrontpageWarningsGetItem> warningItems,
                                               List<List<Long>> adGroupGeo,
                                               Double expectedMinPrice,
                                               List<Double> expectedAdGroupMinPrices,
                                               List<List<Long>> expectedMobileWarningRegions,
                                               List<List<Long>> expectedDesktopWarningRegions,
                                               List<List<Long>> expectedBrowserNewTabWarningRegions) {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(isAutoBudget ? averageCpaStrategy() : manualStrategy()), clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(), campaignInfo.getCampaignId(), campaignShowTypes);
        request.setCampaignId(campaignInfo.getCampaignId());
        EntryStream.of(warningItems).forKeyValue((index, item) -> item.setAdGroupId(addAdGroup(campaignInfo,
                adGroupGeo.get(index),
                CurrencyChf.getInstance().getMinCpmPrice().doubleValue())));
        request.withFrontpageWarningsGetItems(warningItems);

        CpmYndxFrontpagePriceWarningsResponse response = controller.getFrontpagePriceWarnings(request, null);
        BigDecimal actualMinPrice = response.getMinPrice();
        assertThat("?????????????????????? ???????? ???????????? ???????????????? ?? ??????????????????", actualMinPrice.doubleValue(),
                equalTo(expectedMinPrice));

        List<Double> actualAdGroupMinPrices = response.responseItems().stream()
                .map(t -> t.adGroupMinPrice())
                .map(t -> t.doubleValue())
                .collect(Collectors.toList());
        assertThat("?????????????????????? ???????? ???? ?????????????? ???????????? ???????????????? ?? ????????????????????",
                actualAdGroupMinPrices, contains(expectedAdGroupMinPrices.toArray()));

        List<List<WebDefect>> responseWarnings = response.responseItems().stream()
                .map(t -> t.validationResult())
                .map(t -> t.getWarnings())
                .collect(Collectors.toList());
        List<List<Long>> actualMobileWarningRegionIds = responseWarnings.stream()
                .map(t -> getWarningRegions(t, GEO_FRONTPAGE_MOBILE_IGNORED_REGIONS_DUE_TO_PRICE.getCode()))
                .filter(Objects::nonNull)
                .map(t -> t.stream().map(s -> s.getId()).collect(Collectors.toList()))
                .collect(Collectors.toList());
        List<List<Long>> actualDesktopWarningRegionIds = responseWarnings.stream()
                .map(t -> getWarningRegions(t, GEO_FRONTPAGE_DESKTOP_IGNORED_REGIONS_DUE_TO_PRICE.getCode()))
                .filter(Objects::nonNull)
                .map(t -> t.stream().map(s -> s.getId()).collect(Collectors.toList()))
                .collect(Collectors.toList());
        List<List<Long>> actualBrowserNewTabWarningRegionIds = responseWarnings.stream()
                .map(t -> getWarningRegions(t, GEO_FRONTPAGE_BROWSER_NEW_TAB_IGNORED_REGIONS_DUE_TO_PRICE.getCode()))
                .filter(Objects::nonNull)
                .map(t -> t.stream().map(s -> s.getId()).collect(Collectors.toList()))
                .collect(Collectors.toList());

        assertThat("???????????? ???????????????? ?? ?????????????????? ?????????? ???????????????????????????? ???? ?????????????????? ??????????????",
                actualMobileWarningRegionIds.size() == expectedMobileWarningRegions.size(), is(true));
        EntryStream.of(expectedMobileWarningRegions)
                .forKeyValue((index, expectedWarningRegions) -> assertThat(
                        "?????????? ???????????????? ?? ???????????????????? ???? ?????????????????? ?????????????? ???????????? ???????????????? ?? ??????????????????",
                        actualMobileWarningRegionIds.get(index), equalTo(expectedWarningRegions)));

        assertThat("???????????? ???????????????? ?? ?????????????????? ?????????? ???????????????????????????? ???? ???????????????????? ??????????????",
                actualDesktopWarningRegionIds.size() == expectedDesktopWarningRegions.size(), is(true));
        EntryStream.of(expectedDesktopWarningRegions)
                .forKeyValue((index, expectedWarningRegions) -> assertThat(
                        "?????????? ???????????????? ?? ???????????????????? ???? ???????????????????? ?????????????? ???????????? ???????????????? ?? ??????????????????",
                        actualDesktopWarningRegionIds.get(index), equalTo(expectedWarningRegions)));

        assertThat("???????????? ???????????????? ?? ?????????????????? ?????????? ???????????????????????????? ???? ?????????????? ???? ?????????? ????????????????",
                actualBrowserNewTabWarningRegionIds.size() == expectedBrowserNewTabWarningRegions.size(), is(true));
        EntryStream.of(expectedBrowserNewTabWarningRegions)
                .forKeyValue((index, expectedWarningRegions) -> assertThat(
                        "?????????? ???????????????? ?? ???????????????????? ???? ?????????????? ???? ?????????? ???????????????? ???????????? ???????????????? ?? ??????????????????",
                        actualBrowserNewTabWarningRegionIds.get(index), equalTo(expectedWarningRegions)));
    }

    private static List<RegionDesc> getWarningRegions(List<WebDefect> warningsList, String requiredWarningCode) {
        return warningsList.stream()
                .filter(s -> s.getCode().equals(requiredWarningCode))
                .map(s -> s.getParams())
                .map(s -> (RegionDefectParams) s)
                .map(s -> s.getRegions())
                .findFirst()
                .orElse(null);
    }

    private Long addAdGroup(CampaignInfo campaignInfo, List<Long> adGroupGeo, Double minPrice) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId())
                        .withGeo(adGroupGeo))
                .withCampaignInfo(campaignInfo));
        RetConditionInfo retargetingCondition =
                steps.retConditionSteps().createCpmRetCondition(adGroupInfo.getClientInfo());
        steps.retargetingSteps().createRetargeting(defaultRetargeting()
                .withRetargetingConditionId(retargetingCondition.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(minPrice)), adGroupInfo);
        return adGroupInfo.getAdGroupId();
    }
}
