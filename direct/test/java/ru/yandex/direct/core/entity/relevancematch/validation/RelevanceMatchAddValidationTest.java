package ru.yandex.direct.core.entity.relevancematch.validation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchAddContainer;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchAddValidationTest extends RelevanceMatchModificationBaseTest {

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    private RelevanceMatchValidationService relevanceMatchValidationService;

    @Test
    public void validate_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_RelevanceMatchWithPriceAndAutobudgetPriority_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withPrice(BigDecimal.TEN)
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_NullAutobudgetPriorityForAutoBudgetCamp_error() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("autobudgetPriority")), CommonDefects.notNull()))));
    }

    @Test
    public void validate_RelevanceMatchWithoutPriceAndWithoutAutobudgetAndOnContext_error() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(null)
                .withPriceContext(null);

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .getStrategy()
                .withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.CONTEXT);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("priceContext")),
                        BidsDefects.Ids.CONTEXT_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY))));

    }

    @Test
    public void validate_RelevanceMatchWithoutPriceAndWithoutAutobudgetAndOnSearch_error() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(null)
                .withPriceContext(null);

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .getStrategy()
                .withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.SEARCH);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("price")),
                        BidsDefects.Ids.SEARCH_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY))));
    }

    @Test
    public void validate_AutobudgetStrategyWithPrice_Warning() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(5)
                .withPrice(BigDecimal.TEN)
                .withIsSuspended(true);

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .getStrategy()
                //.withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.SEARCH);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        Assert.assertThat(actual, hasNoErrors());
        Assert.assertThat(actual, hasDefectDefinitionWith(
                validationError(path(index(0), field("price")),
                        BidsDefects.Ids.BID_FOR_SEARCH_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY)));
    }

    @Test
    public void validate_NullAdGroupId_error() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(null)
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), CommonDefects.notNull()))));
    }

    @Test
    public void validate_TwoRelevanceMatch_Error() {
        RelevanceMatch relevanceMatch1 = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

        RelevanceMatch relevanceMatch2 = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(asList(relevanceMatch1, relevanceMatch2));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        RelevanceMatchDefects.Number.TOO_MANY_RELEVANCE_MATCH_IN_AD_GROUP))));
    }

    @Test
    public void validate_NotVisibleCampaignAccessChecker_ObjectNotFound() {
        defaultUser = steps.clientSteps().createDefaultClient().getChiefUserInfo();

        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), objectNotFound()))));
    }

    @Test
    public void validate_NotWritableCampaignAccessChecker_NoRights() {
        defaultUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).getChiefUserInfo();

        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), noRights()))));
    }

    @Test
    public void validate_CampaignTypeNotSupported() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .withType(CampaignType.DYNAMIC);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), CampaignDefects.campaignTypeNotSupported()))));
    }

    @Test
    public void validate_RelevanceMatchCategories_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_RelevanceMatchCategories_Null_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true)
                .withRelevanceMatchCategories(null);

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_RelevanceMatchCategories_Empty_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true)
                .withRelevanceMatchCategories(emptySet());

        ValidationResult<List<RelevanceMatch>, Defect> actual = validate(singletonList(relevanceMatch));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_CampaignTypeNotSupportedForRelevanceMatchCategories() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .withType(CampaignType.MOBILE_CONTENT);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("relevanceMatchCategories")),
                        CampaignDefects.inconsistentCampaignType()))));
    }

    @Test
    public void validate_RelevanceMatchCategories_Empty_CampaignTypeNotSupported_Success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true)
                .withRelevanceMatchCategories(emptySet());

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = defaultRelevanceMatchAddOperationContainer();
        relevanceMatchAddOperationContainer.getCampaignByAdGroupId(defaultAdGroup.getAdGroupId())
                .withType(CampaignType.MOBILE_CONTENT);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), relevanceMatchAddOperationContainer);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_StrategyIsNotSet() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId());
        campaignsByIds.get(activeCampaign.getCampaignId()).withStrategy(null);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), defaultRelevanceMatchAddOperationContainer());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        new Defect<>(BidsDefects.Ids.STRATEGY_IS_NOT_SET)))));
    }

    @Test
    @Ignore("Сделать моки вместо @CoreTest")
    public void validate_ValidateContextPriceWhenClientInExperiment() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPriceContext(null)
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

        ValidationResult<List<RelevanceMatch>, Defect> actual =
                validate(singletonList(relevanceMatch), defaultRelevanceMatchAddOperationContainer());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        new Defect<>(BidsDefects.Ids.CONTEXT_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY)))));
    }

    private ValidationResult<List<RelevanceMatch>, Defect> validate(List<RelevanceMatch> relevanceMatches) {
        return validate(relevanceMatches, defaultRelevanceMatchAddOperationContainer());
    }

    private ValidationResult<List<RelevanceMatch>, Defect> validate(List<RelevanceMatch> relevanceMatches,
                                                                    RelevanceMatchAddContainer relevanceMatchAddContainer) {
        ValidationResult<List<RelevanceMatch>, Defect> preValidationResult =
                new ValidationResult<>(relevanceMatches);
        return relevanceMatchValidationService
                .validateAddRelevanceMatches(preValidationResult, relevanceMatchAddContainer, true);
    }

    private RelevanceMatchAddContainer defaultRelevanceMatchAddOperationContainer() {
        return RelevanceMatchAddContainer
                .createRelevanceMatchAddOperationContainer(getOperatorUid(), getClientId(), campaignsByIds,
                        campaignIdsByAdGroupIds);
    }
}
