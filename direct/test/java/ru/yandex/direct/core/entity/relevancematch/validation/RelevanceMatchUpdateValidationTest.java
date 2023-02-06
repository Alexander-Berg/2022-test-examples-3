package ru.yandex.direct.core.entity.relevancematch.validation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchUpdateContainer;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateValidationTest extends RelevanceMatchModificationBaseTest {

    private static final long NOT_EXISTING_RELEVANCE_MATCH_ID = 9999L;

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private RelevanceMatchValidationService relevanceMatchValidationService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Test
    public void preValidate_Success() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(7, RelevanceMatch.AUTOBUDGET_PRIORITY);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidate_RelevanceMatchCantBeUsedInAutoBudgetCompany_error() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(BigDecimal.ONE, RelevanceMatch.PRICE);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        RelevanceMatchDefects.relevanceMatchCantBeUsedInAutoBudgetCompany()))));
    }

    @Test
    public void preValidate_2SameRelevanceMatchInOneOperation_error() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(BigDecimal.ONE, RelevanceMatch.PRICE);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Arrays.asList(modelChanges, modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        CollectionDefects.duplicatedElement()))));
    }

    @Test
    public void preValidate_RelevanceMatchCantBeUsedWhenSearchIsStopped_error() {
        dslContextProvider.ppc(defaultUser.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.No)
                .set(CAMPAIGNS.PLATFORM, CampaignsPlatform.context)
                .where(CAMPAIGNS.CID.eq(activeCampaign.getCampaignId()))
                .execute();

        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(BigDecimal.ONE, RelevanceMatch.PRICE);
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        RelevanceMatchDefects.relevanceMatchCantBeUsedWhenSearchIsStopped()))));
    }

    @Test
    public void preValidate_NotExistingIds_error() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(NOT_EXISTING_RELEVANCE_MATCH_ID, RelevanceMatch.class);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("id")), CommonDefects.objectNotFound()))));
    }

    @Test
    public void preValidate_SuspendAlreadySuspended_WarningNotSuspendedRelevanceMatch() {
        setSuspended(getSavedRelevanceMatch().getId(), true);

        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(true, RelevanceMatch.IS_SUSPENDED);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> vr = relevanceMatchValidationService.
                preValidateUpdateRelevanceMatches(singletonList(modelChanges), relevanceMatchUpdateOperationContainer
                );

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RelevanceMatchDefects.relevanceMatchAlreadySuspended()))));
    }

    @Test
    public void preValidate_ResumeNotSuspended_WarningNotSuspendedRelevanceMatch() {
        setSuspended(getSavedRelevanceMatch().getId(), false);

        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(false, RelevanceMatch.IS_SUSPENDED);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> vr = relevanceMatchValidationService.
                preValidateUpdateRelevanceMatches(singletonList(modelChanges), relevanceMatchUpdateOperationContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RelevanceMatchDefects.relevanceMatchNotSuspended()))));
    }

    @Test
    public void preValidate_RelevanceMatchCategories_Success() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(7, RelevanceMatch.AUTOBUDGET_PRIORITY);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);
        modelChanges.process(asSet(RelevanceMatchCategory.values()), RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidate_RelevanceMatchCategories_Empty_Success() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(7, RelevanceMatch.AUTOBUDGET_PRIORITY);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);
        modelChanges.process(emptySet(), RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidate_RelevanceMatchCategories_Null_Success() {
        ModelChanges<RelevanceMatch> modelChanges =
                new ModelChanges<>(getSavedRelevanceMatch().getId(), RelevanceMatch.class);
        modelChanges.process(7, RelevanceMatch.AUTOBUDGET_PRIORITY);
        modelChanges.process(activeCampaign.getCampaignId(), RelevanceMatch.CAMPAIGN_ID);
        modelChanges.process(null, RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();
        ValidationResult<List<ModelChanges<RelevanceMatch>>, Defect> actual = relevanceMatchValidationService
                .preValidateUpdateRelevanceMatches(Collections.singletonList(modelChanges),
                        relevanceMatchUpdateOperationContainer);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_Success() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }


    @Test
    public void validate_InvalidAutobudgetPriorityForAutoBudgetCamp_error() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(
                        Collections.singletonList(getSavedRelevanceMatch().withAutobudgetPriority(1111)));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("autobudgetPriority")), CommonDefects.invalidValue()))));
    }

    @Test
    public void validate_NullAutobudgetPriorityForAutoBudgetCamp_error() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(
                        Collections.singletonList(getSavedRelevanceMatch().withAutobudgetPriority(null)));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("autobudgetPriority")), CommonDefects.notNull()))));
    }

    @Test
    public void validate_NullPricePriorityForManualCamp_error() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId())
                .withAutobudget(false)
                .getStrategy().withAutobudget(ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget.NO)
                .withPlatform(ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.BOTH);

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch().withPrice(null)));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("price")),
                        BidsDefects.Ids.SEARCH_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY))));
    }

    @Test
    public void validate_InvalidLettersHrefParam1_error() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId());

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch().withHrefParam1("åß∂")));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("hrefParam1")),
                        DefectIds.MUST_CONTAIN_LETTERS_OR_DIGITS_OR_PUNCTUATIONS))));
    }

    @Test
    public void validate_TooLongHrefParam1_error() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId());

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch().withHrefParam1(
                        Strings.repeat("a", RelevanceMatchValidationService.MAX_HREF_PARAM_LENGTH + 1))));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("hrefParam1")),
                        StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    public void validate_InvalidLettersHrefParam2_error() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId());

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch().withHrefParam2("åß∂")));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("hrefParam2")),
                        DefectIds.MUST_CONTAIN_LETTERS_OR_DIGITS_OR_PUNCTUATIONS))));
    }

    @Test
    public void validate_TooLongHrefParam2_error() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId());

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch().withHrefParam2(
                        Strings.repeat("a", RelevanceMatchValidationService.MAX_HREF_PARAM_LENGTH + 1))));

        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("hrefParam2")),
                        StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    public void validate_NotVisibleCampaignAccessChecker_ObjectNotFound() {
        defaultUser = steps.clientSteps().createDefaultClient().getChiefUserInfo();

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), objectNotFound()))));
    }

    @Test
    public void validate_NotWritableCampaignAccessChecker_NoRights() {
        defaultUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).getChiefUserInfo();

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), noRights()))));
    }

    @Test
    public void validate_ArchivedCampaignAccessChecker_ArchivedCampaignModification() {
        campaignRepository.archiveCampaign(defaultAdGroup.getShard(), defaultAdGroup.getCampaignId());

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), archivedCampaignModification()))));
    }

    @Test
    public void validate_RelevanceMatchCategories_Success() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()
                        .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()))));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_RelevanceMatchCategories_Empty_Success() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()
                        .withRelevanceMatchCategories(emptySet())));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_RelevanceMatchCategories_Null_Success() {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()
                        .withRelevanceMatchCategories(null)));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_CampaignTypeNotSupportedForRelevanceMatchCategories() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId())
                .withType(CampaignType.MOBILE_CONTENT);

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()
                        .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()))));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("relevanceMatchCategories")),
                        CampaignDefects.inconsistentCampaignType()))));
    }

    @Test
    public void validate_RelevanceMatchCategories_Empty_CampaignTypeNotSupported_Success() {
        campaignsByIds.get(getSavedRelevanceMatch().getCampaignId())
                .withType(CampaignType.MOBILE_CONTENT);

        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(Collections.singletonList(getSavedRelevanceMatch()
                        .withRelevanceMatchCategories(emptySet())));
        RelevanceMatchUpdateContainer relevanceMatchUpdateOperationContainer =
                createRelevanceMatchUpdateOperationContainer();

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                relevanceMatchValidationService.validateUpdateRelevanceMatches(preValidationResult,
                        relevanceMatchUpdateOperationContainer, true);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    private RelevanceMatchUpdateContainer createRelevanceMatchUpdateOperationContainer() {
        return RelevanceMatchUpdateContainer.createRelevanceMatchUpdateOperationContainer(
                getOperatorUid(), getClientId(), campaignsByIds, campaignIdsByAdGroupIds,
                adGroupIdsByRelevanceMatchIds,
                relevanceMatchByIds
        );
    }
}
