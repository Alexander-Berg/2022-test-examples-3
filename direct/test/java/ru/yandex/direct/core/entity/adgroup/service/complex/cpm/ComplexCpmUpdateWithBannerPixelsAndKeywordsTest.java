package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithYandexPlacements;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тест проверяет валидацию пикселей на cpm-баннерах в комплексной операции обновления cpm-групп
 * с criterion_type=KEYWORD
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexCpmUpdateWithBannerPixelsAndKeywordsTest {

    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;

    @Autowired
    private ComplexAdGroupUpdateOperationFactory updateOperationFactory;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;

    private GeoTree geoTree;

    protected static ClientInfo clientInfo;
    protected static Long canvasCreativeId;
    protected static CampaignInfo dealCampaignNonYandexPlacements;
    protected static CampaignInfo dealCampaignYandexPlacements;
    protected List<DealInfo> dealInfosNonYandex;
    protected List<DealInfo> dealInfosYandex;
    private Long operatorUid;

    @Before
    public void before() throws Exception {
        geoTree = geoTreeFactory.getGlobalGeoTree();

        ClientInfo agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);
        operatorUid = agencyClientInfo.getUid();

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        canvasCreativeId = creativeInfo.getCreativeId();
        dealInfosNonYandex = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);
        dealInfosYandex = createDealWithYandexPlacements(steps, placementsRepository, agencyClientInfo);
        dealCampaignNonYandexPlacements = createCpmDealCampaignWithDeals(dealInfosNonYandex, clientInfo);
        dealCampaignYandexPlacements = createCpmDealCampaignWithDeals(dealInfosYandex, clientInfo);
    }

    @After
    public void after() {
        steps.dealSteps()
                .unlinkDeals(clientInfo.getShard(), mapList(dealInfosNonYandex, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfosNonYandex, DealInfo::getDeal), clientInfo);

        steps.dealSteps()
                .unlinkDeals(clientInfo.getShard(), mapList(dealInfosYandex, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfosYandex, DealInfo::getDeal), clientInfo);

        placementsRepository.deletePlacementsBy(ImmutableList.of(BIG_PLACEMENT_PAGE_ID, BIG_PLACEMENT_PAGE_ID + 1));
    }

    //Добавим в группу с размещениями яндекса баннер с пикселем adfox и изменим его на adriver
    @Test
    public void adriverPixel_YandexInventory_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToPixel(adriverPixelUrl(), emptyList(),
                                CampaignType.CPM_DEALS,
                                InventoryType.YANDEX_INVENTORY)));
        testSingleGroupWithSingleBanner(adfoxPixelUrl(), adriverPixelUrl(),
                dealCampaignYandexPlacements.getCampaignId(), errorMatcher);
    }

    //Добавим в группу с размещениями яндекса баннер с пикселем adfox и изменим его на я_аудиторий
    @Test
    public void yaAudiencePixel_YandexInventory_PositiveTest() {
        testSingleGroupWithSingleBanner(adfoxPixelUrl(), yaAudiencePixelUrl(),
                dealCampaignYandexPlacements.getCampaignId(), null);
    }

    //Добавим в группу с размещениями вне яндекса баннер с пикселем adfox и изменим его на я_аудиторий
    @Test
    public void yaAudiencePixel_NonYandexInventory_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToAudiencePixel(yaAudiencePixelUrl())));
        testSingleGroupWithSingleBanner(adfoxPixelUrl(), yaAudiencePixelUrl(),
                dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    //Добавим в группу с размещениями вне яндекса баннер с пикселем adfox и изменим его на adriver
    @Test
    public void adriverPixel_NonYandexInventory_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToPixel(adriverPixelUrl(), emptyList(),
                                CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        testSingleGroupWithSingleBanner(adfoxPixelUrl(), adriverPixelUrl(),
                dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    public void testSingleGroupWithSingleBanner(String pixelForAdd, String pixelForUpdate,
                                                Long campaignId, Matcher expectedErrorMatcher) {
        Integer numErrorsExpected = (expectedErrorMatcher == null ? 0 : 1);
        var bannersForAdd = singletonList(fullCpmBanner(null, null, canvasCreativeId)
                .withPixels(pixelForAdd == null ? emptyList() : singletonList(pixelForAdd)));
        var bannersForUpdate = singletonList(fullCpmBanner(null, null, canvasCreativeId)
                .withPixels(pixelForUpdate == null ? emptyList() : singletonList(pixelForUpdate)));
        testSingleAdGroup(bannersForAdd, bannersForUpdate, campaignId, numErrorsExpected, expectedErrorMatcher);
    }

    public void testSingleAdGroup(List<CpmBanner> bannersForAdd,
                                  List<CpmBanner> bannersForUpdate,
                                  Long campaignId,
                                  Integer numErrorsExpected,
                                  Matcher errorsMatcher) {
        ComplexCpmAdGroup complexCpmAdGroupForAdd = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmBannerAdGroup(campaignId).withCriterionType(CriterionType.KEYWORD))
                .withBanners(new ArrayList<>(bannersForAdd));
        ComplexCpmAdGroupAddOperation addOperation = createAddOperation(singletonList(complexCpmAdGroupForAdd));
        MassResult<Long> addOperationResult = addOperation.prepareAndApply();
        List<Long> adGroupIds = mapList(addOperationResult.getResult(), t -> t.getResult());


        List<ComplexCpmAdGroup> complexCpmAdGroupsForUpdate = constructComplexCpmAdGroupsForUpdate(
                singletonList(bannersForUpdate), adGroupIds);
        ComplexCpmAdGroupUpdateOperation updateOperation = createUpdateOperation(complexCpmAdGroupsForUpdate);
        MassResult<Long> updateOperationResult = updateOperation.prepareAndApply();

        Integer numErrorsActual = updateOperationResult.getValidationResult().flattenErrors().size();
        assertThat(numErrorsActual, comparesEqualTo(numErrorsExpected));
        if (errorsMatcher != null) {
            assertThat(updateOperationResult.getValidationResult().flattenErrors(), errorsMatcher);
        }
    }

    private CampaignInfo createCpmDealCampaignWithDeals(List<DealInfo> dealInfos,
                                                        ClientInfo clientInfo) {
        CampaignInfo cpmDealCampaignInfo = steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);
        mapList(dealInfos, DealInfo::getDealId).forEach(
                dealId -> steps.dealSteps().linkDealWithCampaign(dealId, cpmDealCampaignInfo.getCampaignId()));
        return cpmDealCampaignInfo;
    }

    private ComplexCpmAdGroupAddOperation createAddOperation(List<ComplexCpmAdGroup> complexAdGroups) {
        return addOperationFactory.createCpmAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, operatorUid, clientInfo.getClientId(),
                clientInfo.getUid(), true);
    }

    private List<ComplexCpmAdGroup> constructComplexCpmAdGroupsForUpdate(
            List<List<CpmBanner>> bannersForUpdate,
            List<Long> adGroupIds) {
        Map<Long, List<Long>> bannerIdsByAdGroupIds = StreamEx.of(bannerTypedRepository.getBannersByGroupIds(
                clientInfo.getShard(), adGroupIds))
                .map(b -> (CpmBanner) b)
                .mapToEntry(CpmBanner::getAdGroupId, CpmBanner::getId)
                .collapseKeys()
                .toMap();
        Map<Long, List<CpmBanner>> bannersForUpdateByAdGroupIds = EntryStream.of(adGroupIds)
                .mapValues(bannerIdsByAdGroupIds::get)
                .mapToValue((index, idList) -> StreamEx.zip(bannersForUpdate.get(index), idList,
                        (cpmBanner, id) -> cpmBanner.withId(id)).toList())
                .mapKeys(adGroupIds::get)
                .toMap();

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(clientInfo.getShard(), adGroupIds);
        return StreamEx.of(adGroups)
                .map(singleAdGroup -> new ComplexCpmAdGroup()
                        .withAdGroup(singleAdGroup)
                        .withBanners(new ArrayList<>(bannersForUpdateByAdGroupIds.get(singleAdGroup.getId()))))
                .toList();
    }

    private ComplexCpmAdGroupUpdateOperation createUpdateOperation(List<ComplexCpmAdGroup> adGroups) {
        return updateOperationFactory.createCpmAdGroupUpdateOperation(adGroups, geoTree, false, null,
                operatorUid, clientInfo.getClientId(), clientInfo.getUid(), true);
    }
}
