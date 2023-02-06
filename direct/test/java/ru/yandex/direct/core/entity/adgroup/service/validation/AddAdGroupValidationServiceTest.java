package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.bstags.AdGroupBsTagsSettingsProvider;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProviderFactory;
import ru.yandex.direct.core.entity.adgroup.service.validation.types.AdGroupTypeSpecificValidationProvider;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.inconsistentAdGroupTypeToCampaignType;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.maxAdGroupsInCampaign;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNoRights;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddAdGroupValidationServiceTest {

    private static final Long WRONG_CID = 1_000_000_000L;
    private static final Long ADGROUP_LIMIT = 2L;

    @Autowired
    protected ClientLimitsService clientLimitsService;

    @Autowired
    protected FeatureService featureService;

    private AddAdGroupValidationService validationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private AdGroupValidationService adGroupValidationService;

    @Autowired
    private AdGroupTypeSpecificValidationProvider typeSpecificValidationServiceProvider;

    @Autowired
    private AdGroupBsTagsSettingsProvider adGroupBsTagsSettingsProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private AdGroupGeoTreeProviderFactory geoTreeProviderFactory;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ClientService clientService;

    private CampaignInfo campaignInfo;
    private AdGroup defaultAdGroup;
    private ClientLimits clientLimits;
    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private GeoTree geoTree;
    private ClientInfo clientInfo;
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        defaultAdGroup = defaultTextAdGroup(campaignInfo.getCampaignId());

        operatorUid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();
        shard = campaignInfo.getShard();
        clientInfo = campaignInfo.getClientInfo();

        rbacService = spy(rbacService);

        campaignSubObjectAccessCheckerFactory = new CampaignSubObjectAccessCheckerFactory(
                shardHelper, rbacService, campaignAccessCheckRepository, new AffectedCampaignIdsContainer(),
                requestAccessibleCampaignTypes, featureService);

        validationService = new AddAdGroupValidationService(
                adGroupValidationService, campaignSubObjectAccessCheckerFactory, adGroupRepository, tagRepository,
                minusKeywordsPackRepository, clientLimitsService, featureService, typeSpecificValidationServiceProvider,
                campaignService, clientService);

        clientLimits = new ClientLimits();
        clientLimits.withClientId(clientId).withBannersCountLimit(ADGROUP_LIMIT);
        ClientInfo clientInfo = new ClientInfo().withClientLimits(clientLimits).withShard(shard);
        steps.clientSteps().updateClientLimits(clientInfo);
    }

    public ValidationResult<List<AdGroup>, Defect> preValidate(List<AdGroup> models, Long operatorUid) {
        return validationService.preValidate(
                models,
                operatorUid,
                clientId,
                shard);
    }

    public ValidationResult<List<AdGroup>, Defect> preValidate(List<AdGroup> models) {
        return preValidate(models, operatorUid);
    }

    //validate
    @Test
    public void positivePreValidationResultWhenNoErrors() {
        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultAdGroup));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidate_CampaignId_Null() {
        ValidationResult<List<AdGroup>, Defect> actual =
                preValidate(singletonList(defaultAdGroup.withCampaignId(null)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), notNull()))));
    }

    @Test
    public void preValidate_CampaignId_Zero() {
        ValidationResult<List<AdGroup>, Defect> actual =
                preValidate(singletonList(defaultAdGroup.withCampaignId(0L)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), validId()))));
    }

    @Test
    public void preValidate_CampaignId_Negative() {
        ValidationResult<List<AdGroup>, Defect> actual =
                preValidate(singletonList(defaultAdGroup.withCampaignId(-1L)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), validId()))));
    }

    @Test
    public void preValidate_CampaignId_CampaignNotExists() {
        ValidationResult<List<AdGroup>, Defect> actual =
                preValidate(singletonList(defaultAdGroup.withCampaignId(WRONG_CID)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    @Test
    public void preValidate_CampaignId_CampaignNotVisible() {
        Long newOperatorUid = steps.userSteps().generateNewUserUid();
        when(rbacService.getVisibleCampaigns(newOperatorUid, singleton(campaignInfo.getCampaignId())))
                .thenReturn(emptySet());

        ValidationResult<List<AdGroup>, Defect> actual =
                preValidate(singletonList(defaultAdGroup), newOperatorUid);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    @Test
    public void preValidate_CampaignId_CampaignNotWritable() {
        Long newOperatorUid = steps.userSteps().generateNewUserUid();
        when(rbacService.getVisibleCampaigns(newOperatorUid, singleton(campaignInfo.getCampaignId())))
                .thenReturn(singleton(campaignInfo.getCampaignId()));
        when(rbacService.getWritableCampaigns(newOperatorUid, singleton(campaignInfo.getCampaignId())))
                .thenReturn(emptySet());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultAdGroup), newOperatorUid);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNoRights()))));
    }

    @Test
    public void preValidate_CampaignId_CampaignIsArchived() {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(defaultAdGroup.getCampaignId()))
                .execute();

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), archivedCampaignModification()))));
    }

    @Test
    public void preValidate_geoCampaignTextAdGroup() {
        requestAccessibleCampaignTypes.setApi5();
        CampaignInfo geoTypeCampaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.newGeoCampaign(clientId, operatorUid), clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(geoTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    @Test
    public void preValidate_Type_Null() {
        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultAdGroup.withType(null)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.TYPE.name())), notNull()))));
    }

    @Test
    public void preValidate_mcbCampaignTextAdGroup() {
        requestAccessibleCampaignTypes.setApi5();
        CampaignInfo mcbTypeCampaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.newMcbCampaign(clientId, operatorUid), clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(mcbTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    @Test
    public void preValidate_mobileCampaignTextAdGroup() {
        CampaignInfo mobileTypeCampaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(mobileTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.TYPE.name())), inconsistentAdGroupTypeToCampaignType()))));
    }

    @Test
    public void preValidate_performanceCampaignTextAdGroup() {
        requestAccessibleCampaignTypes.setApi5();
        CampaignInfo performanceTypeCampaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.activePerformanceCampaign(clientId, operatorUid), clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(performanceTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.TYPE.name())), inconsistentAdGroupTypeToCampaignType()))));
    }

    @Test
    public void preValidate_mcbannerCampaignTextAdGroup() {
        requestAccessibleCampaignTypes.setApi5();
        CampaignInfo mcbannerTypeCampaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.newMcbannerCampaign(clientId, operatorUid), clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(mcbannerTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    @Test
    public void preValidate_dynamicCampaignTextAdGroup() {
        CampaignInfo dynamicTypeCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(dynamicTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.TYPE.name())), inconsistentAdGroupTypeToCampaignType()))));
    }

    @Test
    public void preValidate_walletCampaignTextAdGroup() {
        requestAccessibleCampaignTypes.setApi5();
        CampaignInfo walletTypeCampaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.activeWalletCampaign(clientId, operatorUid), clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(walletTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = preValidate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.CAMPAIGN_ID.name())), campaignNotFound()))));
    }

    //validate

    private ValidationResult<List<AdGroup>, Defect> validate(List<AdGroup> models) {
        var campaignIds = mapList(models, AdGroup::getCampaignId);
        var campaignTypes = campaignRepository.getCampaignsTypeMap(shard, campaignIds);

        var bsTagsSettings = adGroupBsTagsSettingsProvider.getAdGroupBsTagsSettings(models, clientId);
        var geoTreeProvider = geoTreeProviderFactory.create(geoTree, campaignTypes);

        return validationService.validate(models, emptyMap(), emptyMap(), bsTagsSettings, geoTreeProvider, clientId,
                operatorUid, shard);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<AdGroup>, Defect> actual = validate(singletonList(defaultAdGroup));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void positiveValidationTextAdGroupTextCampaign() {
        CampaignInfo textTypeCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroup defaultTextAdGroup = defaultTextAdGroup(textTypeCampaignInfo.getCampaignId());

        ValidationResult<List<AdGroup>, Defect> actual = validate(singletonList(defaultTextAdGroup));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void positiveValidationMobileAdGroupMobileCampaign() {
        CampaignInfo mobileTypeCampaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        AdGroup defaultMobileAdGroup =
                createMobileAppAdGroup(mobileTypeCampaignInfo.getCampaignId(), defaultMobileContent())
                        .withMinimalOperatingSystemVersion("1.0");

        ValidationResult<List<AdGroup>, Defect> actual = validate(singletonList(defaultMobileAdGroup));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void positiveValidationDynamicAdGroupDynamicCampaign() {
        CampaignInfo dynamicTypeCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroup defaultDynamicAdGroup = activeDynamicTextAdGroup(dynamicTypeCampaignInfo.getCampaignId())
                .withDomainUrl("www.yandex.ru");

        ValidationResult<List<AdGroup>, Defect> actual = validate(singletonList(defaultDynamicAdGroup));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_TooManyAdGroupsInCamp_OneAdGroup() {
        steps.adGroupSteps().createAdGroup(activeTextAdGroup(campaignInfo.getCampaignId()), campaignInfo);
        steps.adGroupSteps().createAdGroup(activeTextAdGroup(campaignInfo.getCampaignId()), campaignInfo);

        ValidationResult<List<AdGroup>, Defect> actual = validate(singletonList(defaultAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), maxAdGroupsInCampaign(clientLimits.getBannersCountLimit())))));
    }

    @Test
    public void validate_TooManyAdGroupsInCamp_SecondAdGroup() {
        steps.adGroupSteps().createAdGroup(activeTextAdGroup(campaignInfo.getCampaignId()), campaignInfo);

        ValidationResult<List<AdGroup>, Defect> actual = validate(asList(defaultAdGroup, defaultAdGroup));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(1)), maxAdGroupsInCampaign(clientLimits.getBannersCountLimit())))));
    }

    @Test
    public void validate_Type_typeSpecificValidationIsCalled() {
        ClientLimitsService clientLimitsService = mock(ClientLimitsService.class);

        AdGroup adGroup = defaultAdGroup.withType(AdGroupType.BASE);
        List<AdGroup> adGroups = singletonList(adGroup);

        typeSpecificValidationServiceProvider = mock(AdGroupTypeSpecificValidationProvider.class);
        when(typeSpecificValidationServiceProvider.validateAdGroups(any(), anyList()))
                .thenReturn(ValidationResult.success(adGroups));
        when(typeSpecificValidationServiceProvider.validateAdGroups(any(), anyList()))
                .thenAnswer(args -> ValidationResult.success(args.getArgument(1)));

        validationService = new AddAdGroupValidationService(
                adGroupValidationService,
                campaignSubObjectAccessCheckerFactory,
                adGroupRepository,
                tagRepository,
                minusKeywordsPackRepository, clientLimitsService,
                featureService, typeSpecificValidationServiceProvider, campaignService, clientService);

        ValidationResult<List<AdGroup>, Defect> actual = validate(adGroups);

        verify(typeSpecificValidationServiceProvider)
                .validateAdGroups(any(), eq(adGroups));
    }
}
