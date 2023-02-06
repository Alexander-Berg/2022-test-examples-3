package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import jdk.jfr.Description;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.dbschema.ppc.enums.ModObjectVersionObjType;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.adgroup.model.CriterionType.KEYWORD;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notFound;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.unableToDelete;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_OBJECT_VERSION;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardIncPid.SHARD_INC_PID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService service;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    // Американские Виргинские острова, отсутствуют в нашем гео-дереве
    private static final long UNKNOWN_REGION_ID = 21553L;

    @Test
    public void getSimpleAdGroups_OneGroupBelongsToClientAndOneDoesNot() {
        ClientInfo clientInfo1 = steps.clientSteps().createDefaultClient();
        ClientInfo clientInfo2 = steps.clientSteps().createDefaultClient();
        CampaignInfo campaign1 = steps.campaignSteps().createActiveCampaign(clientInfo1);
        CampaignInfo campaign2 = steps.campaignSteps().createActiveCampaign(clientInfo2);
        AdGroup adGroup1 = defaultTextAdGroup(campaign1.getCampaignId());
        Long adGroupId1 = steps.adGroupSteps().createAdGroup(adGroup1, campaign1).getAdGroupId();
        AdGroup adGroup2 = defaultTextAdGroup(campaign2.getCampaignId());
        Long adGroupId2 = steps.adGroupSteps().createAdGroup(adGroup2, campaign2).getAdGroupId();

        Map<Long, AdGroupSimple> adGroups = service.getSimpleAdGroups(clientInfo1.getClientId(),
                asList(adGroupId1, adGroupId2));
        assertThat("Только одна группа должна принадлежать первому клиенту", adGroups.values(), hasSize(1));
        assertThat("Первая группа должна принадлежать первому клиенту", adGroups.get(adGroupId1).getId(),
                is(adGroupId1));
    }

    @Test
    public void getMobileContentPublisherDomains() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        int shard = clientInfo.getShard();
        DomainInfo domainInfo = steps.domainSteps().createDomain(shard);

        CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        MobileContent mobileContent = steps.mobileContentSteps().createDefaultMobileContent()
                .getMobileContent()
                .withPublisherDomainId(domainInfo.getDomainId());

        MobileContentAdGroup mobileAdGroup = createMobileAppAdGroup(campaign.getCampaignId(), mobileContent);
        steps.adGroupSteps().createAdGroup(mobileAdGroup, campaign);

        mobileContent.withId(mobileAdGroup.getMobileContentId());

        dslContextProvider.ppc(shard)
                .update(MOBILE_CONTENT)
                .set(MOBILE_CONTENT.PUBLISHER_DOMAIN_ID, domainInfo.getDomainId())
                .where(MOBILE_CONTENT.MOBILE_CONTENT_ID.eq(mobileContent.getId()))
                .execute();

        Map<Long, Domain> domains = service.getMobileContentPublisherDomains(clientInfo.getClientId(),
                singletonList(mobileAdGroup.getId()));
        assertThat("У группы должен быть правильный домен", domainInfo.getDomain(),
                is(domains.get(mobileAdGroup.getId())));
    }

    @Test
    public void getMobileContentAppIds() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        MobileContent mobileContent = steps.mobileContentSteps().createDefaultMobileContent()
                .getMobileContent();

        MobileContentAdGroup mobileAdGroup = createMobileAppAdGroup(campaign.getCampaignId(), mobileContent);
        steps.adGroupSteps().createAdGroup(mobileAdGroup, campaign);

        mobileContent.withId(mobileAdGroup.getMobileContentId());
        Map<Long, String> mobileAppIds = service.getMobileContentAppIds(clientInfo.getClientId(),
                singletonList(mobileAdGroup.getId()));

        assertThat("У группы должен быть установлен правильный store content id",
                mobileAppIds.get(mobileAdGroup.getId()), is(mobileContent.getStoreContentId()));
    }

    @Test
    public void getStrategiesByAdGroupIds() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        MobileContent mobileContent = steps.mobileContentSteps().createDefaultMobileContent()
                .getMobileContent();

        MobileContentAdGroup adGroup = createMobileAppAdGroup(campaignInfo.getCampaignId(), mobileContent);
        steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);

        Map<Long, DbStrategy> strategiesByIds = service.getStrategiesByAdGroupIds(clientInfo.getClientId(),
                singletonList(adGroup.getId()));

        assumeThat(campaignInfo.getCampaign().getStrategy(), is(instanceOf(ManualStrategy.class)));

        ManualStrategy strategy = (ManualStrategy) campaignInfo.getCampaign().getStrategy();
        DbStrategy dbStrategy = strategiesByIds.get(adGroup.getId());

        assertThat("Поле автобюджет должно быть установлено правильно", strategy.isAutobudget(),
                is(dbStrategy.isAutoBudget()));
        assertThat("Бюджеты стратегий должны совпадать", strategy.getDayBudget().getDayBudget(),
                is(dbStrategy.getDayBudget()));
    }

    @Test
    public void deleteAdGroups_MetabaseDataDeletedToo() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();

        service.deleteAdGroups(campaignInfo.getUid(), campaignInfo.getClientId(), singletonList(adGroupId));
        assertThat("Группа должна быть удалена из метабазы",
                dslContextProvider.ppcdict()
                        .select(SHARD_INC_PID.PID)
                        .from(SHARD_INC_PID)
                        .where(SHARD_INC_PID.PID.eq(adGroupId))
                        .fetch(),
                empty());
    }

    @Test
    public void deleteAdGroups_WithModObjectVersion_ModObjectVersionIsDeletedToo() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();

        dslContextProvider.ppc(campaignInfo.getShard())
                .insertInto(MOD_OBJECT_VERSION)
                .set(MOD_OBJECT_VERSION.OBJ_ID, adGroupId)
                .set(MOD_OBJECT_VERSION.OBJ_TYPE, ModObjectVersionObjType.phrases)
                .set(MOD_OBJECT_VERSION.EXPORT_VERSION, "1a94230c-cbb4-11e3-a9f9-001851247f16")
                .execute();

        service.deleteAdGroups(campaignInfo.getUid(), campaignInfo.getClientId(), singletonList(adGroupId));

        assertThat("Объект модерации должен быть удален вместе с группой",
                dslContextProvider.ppc(campaignInfo.getShard())
                        .select(MOD_OBJECT_VERSION.OBJ_ID).from(MOD_OBJECT_VERSION)
                        .where(MOD_OBJECT_VERSION.OBJ_ID.eq(adGroupId))
                        .and(MOD_OBJECT_VERSION.OBJ_TYPE.eq(ModObjectVersionObjType.phrases))
                        .fetch(), empty());
    }

    @Test
    public void deleteAdGroups_WithModReason_ModReasonIsDeletedToo() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();

        dslContextProvider.ppc(campaignInfo.getShard())
                .insertInto(MOD_REASONS)
                .set(MOD_REASONS.ID, adGroupId)
                .set(MOD_REASONS.TYPE, ModReasonsType.phrases)
                .execute();

        service.deleteAdGroups(campaignInfo.getUid(), campaignInfo.getClientId(), singletonList(adGroupId));

        assertThat("Объект модерации должен быть удален вместе с группой",
                dslContextProvider.ppc(campaignInfo.getShard())
                        .select(MOD_REASONS.ID).from(MOD_REASONS)
                        .where(MOD_REASONS.ID.eq(adGroupId))
                        .and(MOD_REASONS.TYPE.eq(ModReasonsType.phrases))
                        .fetch(), empty());
    }

    @Test
    public void deleteAdGroups_WithValidAndInvalidAGroupIds_NoExceptionsOccurValidationResultContainsNotFoundDefect() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();

        MassResult<Long> result = service.deleteAdGroups(campaignInfo.getUid(), campaignInfo.getClientId(),
                asList(adGroupId, 1111L));
        assertThat("Запрос должен быть успешным", result, isSuccessful());
        assertThat("Первая группа должна быть удалена",
                result.get(0).getValidationResult(),
                hasNoDefectsDefinitions());
        assertThat("Результат валидации должен содержать нужный дефект",
                result.get(1).getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), notFound())));
    }

    @Test
    @Description("В прайсовой кампании нельзя удалять дефолтную группу")
    public void deleteAdGroups_SpecificAdgroupInCpmPriceCampaign_Ok() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()).getPricePackage();
        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign,
                clientInfo);

        MassResult<Long> result = service.deleteAdGroups(clientInfo.getUid(), clientInfo.getClientId(),
                List.of(adGroup.getId()));

        assertThat("Запрос должен быть успешным", result, isSuccessful());
        assertThat("Результат валидации должен содержать нужный дефект",
                result.get(0).getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), unableToDelete())));
    }

    @Test
    @Description("В прайсовой кампании можно удалять специфическую группу")
    public void deleteAdGroups_DefaultAdgroupInCpmPriceCampaign_Error() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()).getPricePackage();
        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(cpmPriceCampaign,
                clientInfo);

        MassResult<Long> result = service.deleteAdGroups(clientInfo.getUid(), clientInfo.getClientId(),
                List.of(adGroup.getId()));

        assertThat("Запрос должен быть успешным", result, isSuccessful());
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void getAdGroupTypesByBannerIds_OneBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        Long bannerId = steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), null), adGroupInfo).getBannerId();

        Map<Long, AdGroupType> adGroupTypesByBannerIds = service.getAdGroupTypesByBannerIds(adGroupInfo.getClientId(),
                singletonList(bannerId));

        assertThat("Должен вернуться правильный тип группы", adGroupTypesByBannerIds.get(bannerId),
                is(AdGroupType.CPM_BANNER));
    }

    @Test
    public void getAdGroupTypesByBannerIds_TwoBannersFromDifferentAdGroups() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        Long bannerId = steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), null), adGroupInfo).getBannerId();

        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveMobileContentAdGroup();
        Long bannerId2 = steps.bannerSteps().createActiveDynamicBanner(adGroupInfo2).getBannerId();

        Map<Long, AdGroupType> adGroupTypesByBannerIds = service.getAdGroupTypesByBannerIds(adGroupInfo.getClientId(),
                asList(bannerId, bannerId2));

        assertThat("Должен вернуться правильный тип группы", adGroupTypesByBannerIds.get(bannerId),
                is(AdGroupType.CPM_BANNER));
        assertThat("Должен вернуться правильный тип группы", adGroupTypesByBannerIds.get(bannerId2),
                is(AdGroupType.MOBILE_CONTENT));
    }

    @Test
    public void getAdGroupTypesByBannerIds_TwoBannersFromOneAdGroup() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        Long bannerId = steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), null), adGroupInfo).getBannerId();
        Long bannerId2 = steps.bannerSteps().createActiveDynamicBanner(adGroupInfo).getBannerId();

        Map<Long, AdGroupType> adGroupTypesByBannerIds = service.getAdGroupTypesByBannerIds(adGroupInfo.getClientId(),
                asList(bannerId, bannerId2));

        assertThat("Должен вернуться правильный тип группы", adGroupTypesByBannerIds.get(bannerId),
                is(AdGroupType.CPM_BANNER));
        assertThat("Должен вернуться правильный тип группы", adGroupTypesByBannerIds.get(bannerId2),
                is(AdGroupType.CPM_BANNER));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoNotSetAndNoGroupsExists() {
        Long clientGeo = Region.BY_REGION_ID;
        Set<Integer> campaignGeo = null;
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItem(clientGeo), is(iterableWithSize(1))));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoNotSetAndNoGroupsExists_UnknownRegion() {
        Long clientGeo = UNKNOWN_REGION_ID;
        Set<Integer> campaignGeo = null;
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItem(Region.RUSSIA_REGION_ID), is(iterableWithSize(1))));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoIsSetAndNoGroupsExists() {
        Long clientGeo = Region.BY_REGION_ID;
        Long campaignGeo = Region.KAZAKHSTAN_REGION_ID;
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(Set.of(campaignGeo.intValue()));
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItem(campaignGeo), is(iterableWithSize(1))));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoIsSetAndThereIsTwoGroupsWithSameGeo() {
        Long clientGeo = Region.BY_REGION_ID;
        Long campaignGeo = Region.KAZAKHSTAN_REGION_ID;
        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(Set.of(campaignGeo.intValue()));
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        FeedInfo feed = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroup firstAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(adGroupGeo);
        AdGroup secondAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, campaignInfo);
        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItems(adGroupGeo.get(0), adGroupGeo.get(1)), is(iterableWithSize(2))));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoIsSetAndThereIsTwoGroupsWithDifferentGeo() {
        Long clientGeo = Region.BY_REGION_ID;
        Long campaignGeo = Region.KAZAKHSTAN_REGION_ID;
        List<Long> firstAdGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        List<Long> secondAdGroupGeo = List.of(Region.SAINT_PETERSBURG_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(Set.of(campaignGeo.intValue()));
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        FeedInfo feed = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroup firstAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(firstAdGroupGeo);
        AdGroup secondAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(secondAdGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, campaignInfo);

        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItem(campaignGeo), is(iterableWithSize(1))));
    }

    @Test
    public void getDefaultGeoCalculator_success_whenCampaignGeoNotSetAndThereIsTwoGroupsWithDifferentGeo() {
        Long clientGeo = Region.BY_REGION_ID;
        Set<Integer> campaignGeo = null;
        List<Long> firstAdGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        List<Long> secondAdGroupGeo = List.of(Region.SAINT_PETERSBURG_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient().withCountryRegionId(clientGeo)));
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        FeedInfo feed = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroup firstAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(firstAdGroupGeo);
        AdGroup secondAdGroup = activePerformanceAdGroup(campaignId, feed.getFeedId()).withGeo(secondAdGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);
        steps.adGroupSteps().createAdGroup(secondAdGroup, campaignInfo);

        List<Long> defaultGeo = service.getDefaultGeoByCampaignId(clientInfo.getClientId(), Set.of(campaignId))
                .get(campaignId);
        assertThat(defaultGeo, allOf(hasItem(clientGeo), is(iterableWithSize(1))));
    }

    @Test
    public void getProductRestrictionIds() {

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        int shard = adGroupInfo.getShard();
        Long campaignId = adGroupInfo.getCampaignId();
        AdGroup adGroup = new CpmBannerAdGroup()
                .withId(adGroupInfo.getAdGroupId())
                .withCampaignId(campaignId)
                .withType(adGroupInfo.getAdGroupType())
                .withCriterionType(KEYWORD);

        Long productId = campaignRepository.getProductIds(shard, singleton(campaignId)).get(campaignId);
        ProductRestriction productRestriction = new ProductRestriction()
                .withProductId(productId)
                .withGroupType(adGroupInfo.getAdGroupType())
                .withPublicNameKey("banner_text")
                .withPublicDescriptionKey("111");
        productRepository.addProductRestrictions(dslContextProvider.ppcdict(), singleton(productRestriction));
        var productRestrictionId = StreamEx.of(productRepository.getAllProductRestrictions())
                .filter(r -> Objects.equals(r.getProductId(), productId))
                .findFirst().get().getId();
        Map<Long, Long> productRestrictionIds = service.getProductRestrictionIdsByAdgroupIds(shard, singletonList(adGroup));
        Map<Long, Long> expected = ImmutableMap.of(adGroupInfo.getAdGroupId(), productRestrictionId);
        assertThat(productRestrictionIds, beanDiffer(expected));
    }
}
