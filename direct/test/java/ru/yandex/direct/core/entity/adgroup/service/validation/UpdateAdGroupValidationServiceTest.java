package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.bstags.AdGroupBsTagsSettingsProvider;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProviderFactory;
import ru.yandex.direct.core.entity.adgroup.service.validation.types.AdGroupTypeSpecificValidationProvider;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.TagCampaignSteps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameCantBeEmpty;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.bsTagNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.duplicatedObject;
import static ru.yandex.direct.core.entity.adgroup.service.validation.UpdateAdGroupValidationService.MAX_ELEMENTS_PER_OPERATION;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectRegions;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.minCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupValidationServiceTest {
    private static final String NEW_NAME = "новое имя " + randomNumeric(5);
    private static final String NEW_VALID_KEYWORD = "новая минус-фраза " + randomNumeric(5);
    private static final List<Long> NEW_VALID_GEO = singletonList(213L);
    private static final List<Long> NEW_INVALID_GEO = singletonList(12345678L);

    @Autowired
    private ClientSteps clientSteps;
    @Autowired
    private AdGroupSteps adGroupSteps;
    @Autowired
    private TagCampaignSteps tagCampaignSteps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;
    @Autowired
    private AdGroupValidationService adGroupValidationService;
    @Autowired
    private AdGroupTypeSpecificValidationProvider typeSpecificValidationProvider;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private AdGroupBsTagsSettingsProvider adGroupBsTagsSettingsProvider;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private MinusKeywordsPackRepository packRepository;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private AdGroupGeoTreeProviderFactory geoTreeProviderFactory;
    @Autowired
    private AdGroupLanguageGeoValidator languageGeoValidator;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private ClientRepository clientRepository;

    // will be mocked
    private UpdateAdGroupValidationService validationService;
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    // валидация ModelChanges

    @Before
    public void before() {
        typeSpecificValidationProvider = spy(typeSpecificValidationProvider);

        validationService = new UpdateAdGroupValidationService(
                campaignSubObjectAccessCheckerFactory,
                adGroupValidationService,
                typeSpecificValidationProvider,
                adGroupRepository,
                tagRepository,
                packRepository, languageGeoValidator, featureService, clientRepository);

        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(null), adGroupInfo1.getClientInfo());

        operatorUid = adGroupInfo1.getUid();
        clientId = adGroupInfo1.getClientId();
        shard = adGroupInfo1.getShard();
    }

    @Test
    public void preValidate_EmptyModelChanges_ResultHasOperationError() {
        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult = preValidateAccess(emptyList());

        assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), minCollectionSize(1))));
    }

    @Test
    public void preValidate_MaximumOfModelChangesExceeded_ResultHasOperationError() {
        List<ModelChanges<AdGroup>> modelChangesList = nCopies(
                MAX_ELEMENTS_PER_OPERATION + 1,
                modelChangesWithNewMinusKeywords(adGroupInfo1.getAdGroupId(), singletonList(NEW_VALID_KEYWORD)));

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidateAccess(modelChangesList);

        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(), maxCollectionSize(MAX_ELEMENTS_PER_OPERATION))));
    }

    @Test
    public void preValidate_DuplicatedAdGroups_ResultHasOperationError() {
        List<ModelChanges<AdGroup>> modelChangesList = nCopies(
                2,
                modelChangesWithNewMinusKeywords(adGroupInfo1.getAdGroupId(), singletonList(NEW_VALID_KEYWORD)));

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> vr = preValidateAccess(modelChangesList);

        assertThat(vr.getSubResults().get(index(0)),
                hasDefectDefinitionWith(validationError(path(), duplicatedObject())));
        assertThat(vr.getSubResults().get(index(1)),
                hasDefectDefinitionWith(validationError(path(), duplicatedObject())));
    }

    @Test
    public void preValidate_NameIsValid_ResultIsSuccessful() {
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithNewName(adGroupInfo1.getAdGroupId(), NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> vr = preValidate(singletonList(modelChanges));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_MinusKeywordsAreValid_ResultIsSuccessful() {
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithNewMinusKeywords(adGroupInfo1.getAdGroupId(), singletonList(NEW_VALID_KEYWORD));

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> vr = preValidate(singletonList(modelChanges));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_GeoIsValid_ResultIsSuccessful() {
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithNewGeo(adGroupInfo1.getAdGroupId(), NEW_VALID_GEO);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> vr = preValidate(singletonList(modelChanges));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_IdIsNull_ResultHasElementError() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(null, NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidateAccess(singletonList(modelChanges));

        checkElementError(validationResult, path(index(0), field("id")), notNull());
    }

    @Test
    public void preValidate_IdDoesNotExists_ResultHasElementError() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(Long.MAX_VALUE, NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidateAccess(singletonList(modelChanges));

        checkElementError(validationResult, path(index(0), field("id")), objectNotFound());
    }

    @Test
    public void preValidate_IdBelongsToOtherClient_ResultHasElementError() {
        AdGroupInfo otherClientAdGroupInfo = adGroupSteps.createActiveTextAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(otherClientAdGroupInfo.getAdGroupId(), NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidateAccess(singletonList(modelChanges));

        checkElementError(validationResult, path(index(0), field("id")), objectNotFound());
    }

    @Test
    public void preValidate_OperatorHasNoRights_ResultHasElementError() {
        ClientInfo otherClient = clientSteps.createDefaultClient();
        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(adGroupInfo1.getAdGroupId(), NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult = preValidateAccess(
                otherClient.getUid(), singletonList(modelChanges));

        checkElementError(validationResult, path(index(0)), objectNotFound());
    }

    @Test
    public void preValidate_OneValidAndOneInvalidItem_ResultHasElementError() {
        ModelChanges<AdGroup> modelChanges1 =
                modelChangesWithNewName(adGroupInfo1.getAdGroupId(), "");
        ModelChanges<AdGroup> modelChanges2 =
                modelChangesWithNewName(adGroupInfo2.getAdGroupId(), NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(asList(modelChanges1, modelChanges2));

        checkElementError(validationResult, path(index(0), field(AdGroup.NAME)), adGroupNameCantBeEmpty());
        assertThat("второй элемент не должен содержать ошибок",
                validationResult.getSubResults().get(index(1)),
                hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_TagsChanged_NoErrorDueToNullCampaignId() {
        List<Long> tags = tagCampaignSteps.createDefaultTags(shard, clientId, adGroupInfo1.getCampaignId(), 3);

        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupInfo1.getAdGroupId(), AdGroup.class)
                .process(tags, AdGroup.TAGS);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(singletonList(modelChanges));

        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CampaignIsArchived_ResultHasElementError() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(adGroup.getId(), NEW_NAME);

        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(adGroup.getCampaignId()))
                .execute();

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidateAccess(singletonList(modelChanges));

        checkElementError(validationResult, path(index(0)), archivedCampaignModification());
    }

    @Test
    public void validate_NameIsValid_ResultIsSuccessful() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(adGroup.getId(), NEW_NAME);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(singletonList(modelChanges));

        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_NameIsNull_ResultHasElementError() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(adGroup.getId(), null);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(singletonList(modelChanges));

        checkElementError(
                validationResult,
                path(index(0), field(AdGroup.NAME.name())),
                adGroupNameIsNotSet());
    }

    @Test
    public void validate_NameIsEmpty_ResultHasElementError() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ModelChanges<AdGroup> modelChanges = modelChangesWithNewName(adGroup.getId(), "");

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(singletonList(modelChanges));

        checkElementError(
                validationResult,
                path(index(0), field(AdGroup.NAME.name())),
                adGroupNameCantBeEmpty());
    }

    @Test
    public void validate_GeoIsInvalid_ResultHasElementError() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ModelChanges<AdGroup> modelChanges = modelChangesWithNewGeo(
                adGroup.getId(), NEW_INVALID_GEO);

        ValidationResult<List<ModelChanges<AdGroup>>, Defect> validationResult =
                preValidate(singletonList(modelChanges));

        checkElementError(
                validationResult,
                path(index(0), field(AdGroup.GEO.name())),
                geoIncorrectRegions(StringUtils.join(NEW_INVALID_GEO, ",")));
    }

    @Test
    public void validate_UntypeAdGroup_TypeSpecificFields_NoErrors() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        ValidationResult<List<AdGroup>, Defect> validationResult = validate(operatorUid, adGroup);

        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_TypedAdGroup_TypeSpecificFields_NoErrors() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        doAnswer(invocation -> ValidationResult.success(invocation.getArgument(1)))
                .when(typeSpecificValidationProvider)
                .validateAdGroups(any(), anyList());

        ValidationResult<List<AdGroup>, Defect> validationResult = validate(operatorUid, adGroup);

        assertThat(validationResult, hasNoDefectsDefinitions());
        verify(typeSpecificValidationProvider).validateAdGroups(any(), anyList());
    }

    @Test
    public void validate_TypedAdGroup_TypeSpecificFields_ExpectErrors() {
        AdGroup adGroup = adGroupInfo1.getAdGroup();

        doAnswer(invocation -> validationErrorForFirstListElement(invocation.getArgument(1), invalidValue()))
                .when(typeSpecificValidationProvider)
                .validateAdGroups(any(), anyList());

        ValidationResult<List<AdGroup>, Defect> validationResult = validate(operatorUid, adGroup);

        checkElementError(
                validationResult, path(index(0)), invalidValue());

        verify(typeSpecificValidationProvider).validateAdGroups(any(), anyList());
    }

    @Test
    public void validate_TargetTagsAllowedOnlyForOperatorUid_NoErrors() {
        AdGroupInfo anotherAdGroupInfo = adGroupSteps.createActiveTextAdGroup();

        // need a target tag allow feature
        featureSteps.addFeature(FeatureName.TARGET_TAGS_ALLOWED);
        Long featureId = featureSteps.getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(FeatureName.TARGET_TAGS_ALLOWED.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();

        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(anotherAdGroupInfo.getClientId())
                        .withId(featureId)
                        .withState(FeatureState.ENABLED);
        featureSteps.addClientFeature(featureIdToClientId);

        var adGroup = adGroupInfo1.getAdGroup();
        var adGroupChanges = ModelChanges.build(adGroup, AdGroup.PAGE_GROUP_TAGS, singletonList("page_group_tag"));

        ValidationResult<List<AdGroup>, Defect> validationResult = validate(anotherAdGroupInfo.getUid(), adGroup,
                adGroupChanges);

        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_TargetTagsNotAllowed_ExpectErrors() {
        AdGroupInfo anotherAdGroupInfo = adGroupSteps.createActiveTextAdGroup();

        var adGroup = adGroupInfo1.getAdGroup();
        var adGroupChanges = ModelChanges.build(adGroup, AdGroup.PAGE_GROUP_TAGS, singletonList("page_group_tag"));

        ValidationResult<List<AdGroup>, Defect> validationResult = validate(anotherAdGroupInfo.getUid(), adGroup,
                adGroupChanges);

        checkElementError(
                validationResult, path(index(0), field(AdGroup.PAGE_GROUP_TAGS)), bsTagNotAllowed());
    }

    private <T> void checkElementError(ValidationResult<List<T>, Defect> validationResult,
                                       Path path, Defect defect) {
        assertThat("результат валидации не должен содержать ошибок уровня операции",
                validationResult.hasErrors(),
                is(false));
        assertThat("результат валидации должен содержать ошибку уровня элемента",
                validationResult,
                hasDefectDefinitionWith(validationError(path, defect)));
    }

    private ModelChanges<AdGroup> modelChangesWithNewName(Long id, String name) {
        return new ModelChanges<>(id, AdGroup.class)
                .process(name, AdGroup.NAME);
    }

    private ModelChanges<AdGroup> modelChangesWithNewMinusKeywords(Long id, List<String> minusKeywords) {
        return new ModelChanges<>(id, AdGroup.class)
                .process(minusKeywords, AdGroup.MINUS_KEYWORDS);
    }

    private ModelChanges<AdGroup> modelChangesWithNewGeo(Long id, List<Long> geo) {
        return new ModelChanges<>(id, AdGroup.class)
                .process(geo, AdGroup.GEO);
    }

    private <L extends List<?>> ValidationResult<L, Defect> validationErrorForFirstListElement(L list, Defect defect) {
        ValidationResult<L, Defect> vr = new ValidationResult<>(list);
        vr.getOrCreateSubValidationResult(index(0), list.get(0)).addError(defect);
        return vr;
    }

    private ValidationResult<List<ModelChanges<AdGroup>>, Defect> preValidateAccess(
            List<ModelChanges<AdGroup>> modelChangesList) {
        return preValidateAccess(operatorUid, modelChangesList);
    }

    private ValidationResult<List<ModelChanges<AdGroup>>, Defect> preValidateAccess(
            Long operatorUid, List<ModelChanges<AdGroup>> modelChangesList) {
        return validationService.preValidateAccess(
                modelChangesList,
                operatorUid,
                clientId,
                shard);
    }

    private ValidationResult<List<ModelChanges<AdGroup>>, Defect> preValidate(
            List<ModelChanges<AdGroup>> modelChangesList) {
        var adGroupIds = mapList(modelChangesList, ModelChanges::getId);
        var campaigns = campaignRepository.getCampaignsSimpleByAdGroupIds(shard, adGroupIds).values();
        var campaignTypes = StreamEx.of(campaigns)
                .mapToEntry(CampaignSimple::getId, CampaignSimple::getType)
                .toMap();

        var geoTreeProvider = geoTreeProviderFactory.create(geoTreeFactory.getGlobalGeoTree(), campaignTypes);

        return validationService.preValidate(
                ValidationResult.success(modelChangesList),
                emptyMap(),
                emptyMap(),
                geoTreeProvider,
                clientId,
                shard);
    }

    private ValidationResult<List<AdGroup>, Defect> validate(Long operatorUid, AdGroup adGroup) {
        return validate(operatorUid, adGroup, new ModelChanges<>(adGroup.getId(), AdGroup.class));
    }

    private ValidationResult<List<AdGroup>, Defect> validate(Long operatorUid, AdGroup adGroup,
                                                             ModelChanges<AdGroup> adGroupChanges) {
        var adGroupsBsTagsSettings = adGroupBsTagsSettingsProvider.getAdGroupBsTagsSettings(List.of(adGroup),
                clientId);
        var preValidationResult = ValidationResult.<List<AdGroup>, Defect>success(singletonList(adGroup));
        var appliedChanges = Map.of(0, adGroupChanges.applyTo(adGroup));
        return validationService.validate(shard, clientId, operatorUid, preValidationResult, appliedChanges,
                adGroupsBsTagsSettings, emptySet(), true);
    }
}
