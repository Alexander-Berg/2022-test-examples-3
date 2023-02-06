package ru.yandex.direct.web.entity.frontpage.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
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
public class CpmYndxFrontpageWarningsControllerTest {
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
    public void createNewCampaignNoGeoTest() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false);
        Double minCpmPrice = CurrencyChf.getInstance().getMinCpmPrice().doubleValue();

        getFrontpagePriceWarningsTest(request, null, false, false, emptyList(), emptyList(),
                minCpmPrice, ImmutableList.of(minCpmPrice), emptyList(), emptyList(), emptyList());
    }

    @Test
    public void createNewCampaignRussiaGeoTest() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, null, false, false, emptyList(), emptyList(),
                .7, ImmutableList.of(.7), emptyList(), emptyList(), emptyList());
    }

    @Test
    public void createNewCampaignRussiaGeoMoscowRegionLowPriceTest() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage")
                .withStrategyAutoPrice(BigDecimal.valueOf(1.1))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, null, true, false, emptyList(), emptyList(),
                1., ImmutableList.of(1.), emptyList(),
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                        SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)), emptyList());
    }

    @Test
    public void validateEmptyCampaignShowTypeChangeRussiaGeoMoscowLowPriceTest() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage")
                .withStrategyAutoPrice(BigDecimal.valueOf(1.6))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE, FRONTPAGE_MOBILE),
                true, false, emptyList(), emptyList(),
                1., ImmutableList.of(1.),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)), emptyList());
    }

    @Test
    public void validateEmptyCampaignShowTypeNonChangeRussiaGeoMoscowStPetersbrurgLowPriceTest() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withStrategyAutoPrice(BigDecimal.valueOf(1.1))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE, FRONTPAGE_MOBILE),
                true, false, emptyList(), emptyList(), .7,
                ImmutableList.of(.7),
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)),
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                        SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)), emptyList());
    }

    @Test
    public void validateCampaignWithTwoAdGroupsCampShowPageCampaignGeoIgnored() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(true)
                .withStrategyAutoPrice(BigDecimal.valueOf(1.6))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE), true, false,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(UKRAINE_REGION_ID)),
                emptyList(), .9, ImmutableList.of(1.5, .9),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID)), emptyList());
    }

    @Test
    public void validateManualCampaignWithAdGroupsCampEditPageCampGeoShowTypeChanged() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage,frontpage_mobile")
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), false, true,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                ImmutableList.of(2., 1.0), 1.1, ImmutableList.of(1.3, 1.1),
                ImmutableList.of(ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)), emptyList());
    }

    @Test
    public void validateManualCampaignWithAdGroupsCampEditPageCampGeoShowTypeChanged2() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage,frontpage_mobile,browser_new_tab")
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), false, true,
                ImmutableList.of(ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                ImmutableList.of(2., 1.0), 1.1, ImmutableList.of(1.3, 1.1),
                ImmutableList.of(ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)));
    }

    @Test
    public void validateAutobudgetCampaignWithAdGroupsCampEditPageCampGeoShowTypeChanged() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage")
                .withStrategyAutoPrice(BigDecimal.valueOf(1.1))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), true, true,
                ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                emptyList(), 1.2, ImmutableList.of(2.5, 1.2),
                emptyList(), ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)), emptyList());
    }
    @Test
    public void validateAutobudgetCampaignWithAdGroupsCampEditPageCampGeoShowTypeChanged2() {
        CpmYndxFrontpageWarningsRequest request = new CpmYndxFrontpageWarningsRequest()
                .withValidateCampaign(true)
                .withUseDbAdGroups(false)
                .withAllowedFrontpageType("frontpage,browser_new_tab")
                .withStrategyAutoPrice(BigDecimal.valueOf(4.1))
                .withCampaignGeo(String.valueOf(RUSSIA_REGION_ID));

        getFrontpagePriceWarningsTest(request, ImmutableSet.of(FRONTPAGE_MOBILE), true, true,
                ImmutableList.of(ImmutableList.of(MOSCOW_REGION_ID),
                        ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)),
                emptyList(), 1.2, ImmutableList.of(2.5, 1.2),
                emptyList(), emptyList(),
                ImmutableList.of(ImmutableList.of(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)));
    }
    private void getFrontpagePriceWarningsTest(CpmYndxFrontpageWarningsRequest request,
                                               Set<FrontpageCampaignShowType> campaignShowTypes,
                                               Boolean isAutoBudget,
                                               Boolean useNewAdGroupsData,
                                               List<List<Long>> adGroupsGeoForDb,
                                               List<Double> dbPrices,
                                               Double expectedMinPrice,
                                               List<Double> expectedAdGroupMinPrices,
                                               List<List<Long>> expectedMobileWarningRegions,
                                               List<List<Long>> expectedDesktopWarningRegions,
                                               List<List<Long>> expectedBrowserNewTabWarningRegions) {
        List<Long> adGroupIds = new ArrayList<>();
        if (campaignShowTypes != null) {
            CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                    activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid())
                            .withStrategy(isAutoBudget ? averageCpaStrategy() : manualStrategy()), clientInfo);
            testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                    clientInfo.getShard(), campaignInfo.getCampaignId(), campaignShowTypes);
            request.setCampaignId(campaignInfo.getCampaignId());
            EntryStream.of(adGroupsGeoForDb).forKeyValue((index, adGroupGeo) ->
                    adGroupIds.add(addAdGroup(campaignInfo, useNewAdGroupsData ? singletonList(0L) : adGroupGeo,
                            dbPrices.size() <= index ? null : dbPrices.get(index))));
        }
        if (useNewAdGroupsData) {
            List<FrontpageWarningsGetItem> warningItems = EntryStream.of(adGroupsGeoForDb)
                    .mapKeyValue((index, geo) -> new FrontpageWarningsGetItem(null, geo.stream()
                            .map(String::valueOf).collect(Collectors.joining(",")), adGroupIds.get(index)))
                    .toList();
            request.withFrontpageWarningsGetItems(warningItems);
        }
        CpmYndxFrontpagePriceWarningsResponse response = controller.getFrontpagePriceWarnings(request, null);
        BigDecimal actualMinPrice = response.getMinPrice();
        assertThat("минимальная цена должна совпасть с ожидаемой", actualMinPrice.doubleValue(),
                equalTo(expectedMinPrice));

        List<Double> actualAdGroupMinPrices = response.responseItems().stream()
                .map(t -> t.adGroupMinPrice())
                .map(t -> t.doubleValue())
                .collect(Collectors.toList());
        assertThat("минимальные цены на группах должны совпасть с ожидаемыми",
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

        assertThat("Должно совпасть с ожидаемым число предупреждений по мобильным показам",
                actualMobileWarningRegionIds.size() == expectedMobileWarningRegions.size(), is(true));
        EntryStream.of(expectedMobileWarningRegions)
                .forKeyValue((index, expectedWarningRegions) -> assertThat(
                        "Набор регионов с ворнингами по мобильным показам должен совпасть с ожидаемым",
                        expectedWarningRegions, equalTo(actualMobileWarningRegionIds.get(index))));

        assertThat("Должно совпасть с ожидаемым число предупреждений по десктопным показам",
                actualDesktopWarningRegionIds.size() == expectedDesktopWarningRegions.size(), is(true));
        EntryStream.of(expectedDesktopWarningRegions)
                .forKeyValue((index, expectedWarningRegions) -> assertThat(
                        "Набор регионов с ворнингами по десктопным показам должен совпасть с ожидаемым",
                        expectedWarningRegions, equalTo(actualDesktopWarningRegionIds.get(index))));
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
                .withPriceContext(minPrice == null ? null : BigDecimal.valueOf(minPrice)), adGroupInfo);
        return adGroupInfo.getAdGroupId();
    }
}
