package ru.yandex.direct.core.entity.campaign.service.validation;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.DialogInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DAY_BUDGET_DAILY_CHANGE_COUNT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.inconsistentCampaignLanguageWithAdGroupGeo;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCounterIsUnavailable;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.operatorCannotSetContentLanguage;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.tooManyDayBudgetDailyChanges;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRestrictedCampaignValidationServiceTest {

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private NetAcl netAcl;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignConstantsService campaignConstantsService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private UserInfo defaultUser;
    private UserInfo anotherUser;
    private UserInfo superUser;
    private CampaignInfo textCampaign;

    @Before
    public void before() {
        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
        defaultUser = steps.clientSteps().createDefaultClient().getChiefUserInfo();
        anotherUser = steps.clientSteps().createDefaultClient().getChiefUserInfo();
        superUser = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.SUPER).getChiefUserInfo();
        textCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        steps.featureSteps().addClientFeature(defaultUser.getClientId(),
                FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED, true);
    }

    @After
    public void after() {
        reset(netAcl);
    }

    @Test
    public void preValidate_NoErrors() {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaign.getCampaignId());
        ModelChanges<TextCampaign> textCampaignModelChanges =
                ModelChanges.build(newTextCampaign, CommonCampaign.NAME, "name");

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, singletonList(textCampaignModelChanges),
                        Set.of(textCampaign.getCampaignId()));

        assertThat(defectValidationResult.flattenErrors()).isEmpty();
    }

    @Test
    public void preValidate_AccessErrors() {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaign.getCampaignId());
        ModelChanges<TextCampaign> textCampaignModelChanges =
                ModelChanges.build(newTextCampaign, CommonCampaign.NAME, "name");

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                anotherUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, singletonList(textCampaignModelChanges),
                        Set.of(textCampaign.getCampaignId()));

        assertThat(defectValidationResult.flattenErrors())
                .isNotEmpty()
                .extracting(defectDefectInfo -> defectDefectInfo.getDefect().defectId())
                .first()
                .isEqualTo(CAMPAIGN_NOT_FOUND);
    }

    @Test
    public void preValidate_HasError() {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaign.getCampaignId());
        ModelChanges<TextCampaign> textCampaignModelChangesFirst =
                ModelChanges.build(newTextCampaign, CommonCampaign.NAME, "name");
        ModelChanges<TextCampaign> textCampaignModelChangesSecond =
                ModelChanges.build(newTextCampaign, CommonCampaign.NAME, "name");

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub, List.of(defaultUser.getUid()), Set.of());

        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, List.of(textCampaignModelChangesFirst,
                        textCampaignModelChangesSecond),
                        Set.of(textCampaign.getCampaignId()));

        assertThat(defectValidationResult.flattenErrors()).isNotEmpty();
    }

    @Test
    public void preValidate_FieldModificationIsAllowedForRole() {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaign.getCampaignId());
        ModelChanges<TextCampaign> textCampaignModelChanges =
                ModelChanges.build(newTextCampaign, TextCampaign.CONTENT_LANGUAGE, ContentLanguage.BE);

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                superUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, singletonList(textCampaignModelChanges),
                        Set.of(textCampaign.getCampaignId()));

        assertThat(defectValidationResult.flattenErrors()).isEmpty();
    }

    @Test
    public void preValidate_FieldModificationIsNotAllowedForRole() {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaign.getCampaignId());
        ModelChanges<TextCampaign> textCampaignModelChanges =
                ModelChanges.build(newTextCampaign, TextCampaign.CONTENT_LANGUAGE, ContentLanguage.BE);

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, singletonList(textCampaignModelChanges),
                        Set.of(textCampaign.getCampaignId()));

        assertThat(defectValidationResult).is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                field(TextCampaign.CONTENT_LANGUAGE)),
                operatorCannotSetContentLanguage()))));
    }

    @Test
    public void preValidate_InconsistentCampaignLanguageWithAdGroupGeo() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withClientInfo(defaultUser.getClientInfo())
                .withAdGroup(activeTextAdGroup().withGeo(List.of(Region.RUSSIA_REGION_ID))));
        Campaign campaign = adGroupInfo.getCampaignInfo().getCampaign();
        Long campaignId = campaign.getId();
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(campaignId);
        ModelChanges<TextCampaign> textCampaignModelChanges =
                ModelChanges.build(newTextCampaign, TextCampaign.CONTENT_LANGUAGE, ContentLanguage.TR);

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                superUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(campaignId, campaign.getType());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, List.of(textCampaignModelChanges),
                        Set.of(campaignId));

        assertThat(defectValidationResult)
                .is(matchedBy(hasDefectWithDefinition(anyValidationErrorOnPath(path(index(0),
                        field(TextCampaign.CONTENT_LANGUAGE))))));
        assertThat(defectValidationResult.getSubResults().get(index(0))
                .getSubResults().get(field(TextCampaign.CONTENT_LANGUAGE)).getErrors()).is(matchedBy(
                beanDiffer(List.of(inconsistentCampaignLanguageWithAdGroupGeo(Language.TURKISH,
                        List.of(adGroupInfo.getAdGroupId()))))));
    }

    @Test
    public void validateBeforeApply() {
        long firstCampaignId = textCampaign.getCampaignId();
        TextCampaign newTextCampaignFirst = new TextCampaign()
                .withId(firstCampaignId)
                .withType(CampaignType.TEXT)
                .withStartDate(now())
                .withEndDate(now());

        CampaignInfo secondTextCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        long secondCampaignId = secondTextCampaign.getCampaignId();
        TextCampaign newTextCampaignSecond = new TextCampaign()
                .withId(secondCampaignId)
                .withType(CampaignType.TEXT)
                .withStartDate(now())
                .withEndDate(now());

        ModelChanges<BaseCampaign> validModelChanges =
                getValidModelChanges(newTextCampaignFirst).castModelUp(BaseCampaign.class);
        ModelChanges<BaseCampaign> invalidModelChanges =
                getInvalidModelChanges(newTextCampaignSecond).castModelUp(BaseCampaign.class);
        Map<Long, BaseCampaign> map = Map.of(firstCampaignId, newTextCampaignFirst, secondCampaignId,
                newTextCampaignSecond);

        var vr = ListValidationBuilder.of(List.of(validModelChanges, invalidModelChanges), Defect.class).getResult();
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getCampaignId(), textCampaign.getCampaign().getType());
        container.setCampaignType(secondTextCampaign.getCampaignId(), secondTextCampaign.getCampaign().getType());
        var vrBeforeApply = updateRestrictedCampaignValidationService
                .validateBeforeApply(container, vr, map);

        assertThat(vrBeforeApply.flattenErrors()).isNotEmpty();
    }

    @Test
    public void validateBeforeApply_CampaignTypeDoNotSupportInterfaceOfModelChanges() {
        long campaignId = textCampaign.getCampaignId();

        TextCampaign textCampaign = new TextCampaign()
                .withId(campaignId)
                .withType(CampaignType.TEXT)
                .withStartDate(now())
                .withEndDate(now());

        ModelChanges<BaseCampaign> invalidModelChanges = getInvalidModelChanges(textCampaign)
                .castModelUp(BaseCampaign.class);

        var vr = ListValidationBuilder.of(List.of(invalidModelChanges), Defect.class).getResult();

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getId(), textCampaign.getType());
        var vrBeforeApply = updateRestrictedCampaignValidationService
                .validateBeforeApply(container, vr, Map.of(campaignId, textCampaign));
        assertThat(vrBeforeApply).
                is(matchedBy(not(hasDefectWithDefinition(validationError(path(index(0),
                        field(TextCampaign.CONTENT_LANGUAGE.name())), operatorCannotSetContentLanguage())))));
    }

    @Test
    public void validate_TryToAddNotClientsMetrikaCounter() {
        long campaignId = textCampaign.getCampaignId();
        Long metrikaCounter = 1L;
        List<Long> metrikaCounters = List.of(metrikaCounter);

        TextCampaign textCampaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withId(campaignId)
                .withStartDate(now())
                .withEndDate(now())
                .withWalletId(0L);

        var appliedChanges = new ModelChanges<>(textCampaign.getId(), TextCampaign.class)
                .process(metrikaCounters, TextCampaign.METRIKA_COUNTERS)
                .applyTo(textCampaign);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(textCampaign)), Map.of(0, appliedChanges));
        assertThat(vr).
                is(matchedBy(hasDefectWithDefinition(validationError(
                        path(index(0), field(TextCampaign.METRIKA_COUNTERS.name()), index(0)),
                        metrikaCounterIsUnavailable()))));
    }

    @Test
    public void validate_AddMetrikaCounter() {
        long campaignId = textCampaign.getCampaignId();
        Long metrikaCounter = 1L;
        List<Long> metrikaCounters = List.of(metrikaCounter);
        metrikaClientStub.addUserCounter(defaultUser.getUid(), metrikaCounter.intValue());

        TextCampaign textCampaign = new TextCampaign()
                .withId(campaignId)
                .withType(CampaignType.TEXT)
                .withName("new Name")
                .withStartDate(now())
                .withEndDate(now())
                .withHasTitleSubstitution(false)
                .withWalletId(0L);

        var appliedChanges = new ModelChanges<>(textCampaign.getId(), TextCampaign.class)
                .process(metrikaCounters, TextCampaign.METRIKA_COUNTERS)
                .applyTo(textCampaign);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub, List.of(defaultUser.getUid()), Set.of());
        metrikaClientAdapter.setCampaignsCounterIds(List.of(textCampaign), List.of(metrikaCounter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(textCampaign)), Map.of(0, appliedChanges));
        assertThat(vr).
                is(matchedBy(not(hasDefectWithDefinition(matchesWith(metrikaCounterIsUnavailable())))));
    }

    @Test
    public void validate_AddMetrikaCounterAndStrategy() {
        long campaignId = textCampaign.getCampaignId();
        Long metrikaCounter = 1L;
        List<Long> metrikaCounters = List.of(metrikaCounter);
        metrikaClientStub.addUserCounter(defaultUser.getUid(), metrikaCounter.intValue());
        Long goalId = 123L;
        metrikaClientStub.addCounterGoal(metrikaCounter.intValue(), goalId.intValue());

        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(campaignId)).get(0);
        campaignForUpdate
                .withStartDate(now())
                .withEmail("smt@yandex.ru")
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel());

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(metrikaCounters, TextCampaign.METRIKA_COUNTERS)
                .process(defaultAverageCpaStrategy(goalId), TextCampaign.STRATEGY)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub, List.of(defaultUser.getUid()), Set.of());
        metrikaClientAdapter.setCampaignsCounterIds(List.of(campaignForUpdate), List.of(metrikaCounter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_GoalIsDoesntExist() {
        long campaignId = textCampaign.getCampaignId();
        Long metrikaCounter = 1L;
        List<Long> metrikaCounters = List.of(metrikaCounter);
        metrikaClientStub.addUserCounter(defaultUser.getUid(), metrikaCounter.intValue());
        Long goalId = 124L;
        metrikaClientStub.addCounterGoal(metrikaCounter.intValue(), goalId.intValue());

        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(campaignId)).get(0);
        campaignForUpdate
                .withStartDate(now())
                .withEmail("smt@yandex.ru")
                .withMetrikaCounters(metrikaCounters);

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(defaultAverageCpaStrategy(goalId + 1), TextCampaign.STRATEGY)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(matchesWith(objectNotFound()))));
    }

    @Test
    public void validate_UpdateMeaningfulGoals_InvalidGoalId() {
        long campaignId = textCampaign.getCampaignId();
        Long metrikaCounter = 1L;
        metrikaClientStub.addUserCounter(defaultUser.getUid(), metrikaCounter.intValue());
        TextCampaign textCampaign = new TextCampaign()
                .withId(campaignId)
                .withType(CampaignType.TEXT)
                .withName("new Name")
                .withStartDate(now())
                .withEndDate(now())
                .withHasTitleSubstitution(false)
                .withCurrency(CurrencyCode.RUB)
                .withWalletId(0L);
        var meaningfulGoals = List.of(new MeaningfulGoal()
                .withGoalId(1L)
                .withConversionValue(BigDecimal.ONE));
        ModelChanges<BaseCampaign> modelChanges = ModelChanges.build(textCampaign,
                        CampaignWithMeaningfulGoalsWithRequiredFields.MEANINGFUL_GOALS, meaningfulGoals)
                .castModelUp(BaseCampaign.class);

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                superUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        container.setCampaignType(textCampaign.getId(), textCampaign.getType());

        var vr = updateRestrictedCampaignValidationService.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), Map.of(campaignId, textCampaign));
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(matchesWith(CollectionDefects.inCollection()))));
    }

    @Test
    public void validate_DialogIdDoesntExist() {
        TextCampaign campaign = new TextCampaign()
                .withId(textCampaign.getCampaignId())
                .withType(CampaignType.TEXT)
                .withWalletId(0L);

        var appliedChanges = new ModelChanges<>(campaign.getId(), TextCampaign.class)
                .process(-1L, TextCampaign.CLIENT_DIALOG_ID)
                .applyTo(campaign);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaign)), Map.of(0, appliedChanges));

        assertThat(vr).
                is(matchedBy(hasDefectWithDefinition(validationError(
                        path(index(0), field(TextCampaign.CLIENT_DIALOG_ID.name())), inCollection()))));
    }

    @Test
    public void validate_AddDialogToCampaign() {
        DialogInfo dialog = steps.dialogSteps().createStandaloneDefaultDialog(defaultUser.getClientInfo());

        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(textCampaign.getCampaignId())).get(0);
        campaignForUpdate
                .withStartDate(now())
                .withEmail("smt@yandex.ru")
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel());

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(dialog.getDialog().getId(), TextCampaign.CLIENT_DIALOG_ID)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_AddSomeoneElsesDialogToCampaign() {
        DialogInfo dialog = steps.dialogSteps().createStandaloneDefaultDialog(anotherUser.getClientInfo());

        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(textCampaign.getCampaignId())).get(0);

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(dialog.getDialog().getId(), TextCampaign.CLIENT_DIALOG_ID)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));

        assertThat(vr).
                is(matchedBy(hasDefectWithDefinition(validationError(
                        path(index(0), field(TextCampaign.CLIENT_DIALOG_ID.name())), inCollection()))));
    }

    @Test
    public void validate_DayBudgetMoreThanMax() {
        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(textCampaign.getCampaignId())).get(0);
        Currency currency = defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency();

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(currency.getMaxDailyBudgetAmount().add(BigDecimal.ONE), TextCampaign.DAY_BUDGET)
                .process(DayBudgetShowMode.DEFAULT_, TextCampaign.DAY_BUDGET_SHOW_MODE)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextCampaign.DAY_BUDGET.name())),
                inInterval(currency.getMinDayBudget(), currency.getMaxDailyBudgetAmount())))));
    }

    @Test
    public void validate_DayBudgetLessThanMin() {
        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(textCampaign.getCampaignId())).get(0);
        Currency currency = defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency();

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(currency.getMinDayBudget().subtract(BigDecimal.ONE), TextCampaign.DAY_BUDGET)
                .process(DayBudgetShowMode.DEFAULT_, TextCampaign.DAY_BUDGET_SHOW_MODE)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextCampaign.DAY_BUDGET.name())),
                inInterval(currency.getMinDayBudget(), currency.getMaxDailyBudgetAmount())))));
    }

    @Test
    public void validate_DayBudgetCannotBeSet() {
        TextCampaign campaignForUpdate =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(),
                        singletonList(textCampaign.getCampaignId())).get(0);
        Currency currency = defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency();

        var appliedChanges = new ModelChanges<>(campaignForUpdate.getId(), TextCampaign.class)
                .process(currency.getMaxDailyBudgetAmount().subtract(BigDecimal.ONE), TextCampaign.DAY_BUDGET)
                .process(DayBudgetShowMode.DEFAULT_, TextCampaign.DAY_BUDGET_SHOW_MODE)
                .process(MAX_DAY_BUDGET_DAILY_CHANGE_COUNT + 1, TextCampaign.DAY_BUDGET_DAILY_CHANGE_COUNT)
                .applyTo(campaignForUpdate);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var vr = updateRestrictedCampaignValidationService.validate(container,
                new ValidationResult<>(List.of(campaignForUpdate)), Map.of(0, appliedChanges));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextCampaign.DAY_BUDGET.name())), tooManyDayBudgetDailyChanges()))));
    }

    @Test
    public void validate_WrongCampaignTypeForModelChanges() {
        var campaign = steps.campaignSteps().createActiveCpmBannerCampaign(defaultUser.getClientInfo());
        ModelChanges<BaseCampaign> campaignModelChanges =
                new ModelChanges<>(campaign.getCampaignId(), CampaignWithMeaningfulGoals.class)
                        .process(List.of(), CampaignWithMeaningfulGoals.MEANINGFUL_GOALS)
                        .castModelUp(BaseCampaign.class);

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultUser.getShard(),
                defaultUser.getUid(),
                defaultUser.getClientInfo().getClientId(),
                defaultUser.getUid(),
                defaultUser.getChiefUid(), metrikaClientAdapter,
                new CampaignOptions(), null, emptyMap());
        var defectValidationResult = updateRestrictedCampaignValidationService
                .preValidate(container, List.of(campaignModelChanges),
                        Set.of(campaign.getCampaignId()));

        assertThat(defectValidationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), CampaignDefects.inconsistentCampaignType()))));
    }

    private ModelChanges<TextCampaign> getValidModelChanges(TextCampaign newTextCampaign) {
        ModelChanges<TextCampaign> textCampaignModelChangesFirst = ModelChanges.build(newTextCampaign,
                CommonCampaign.NAME, "name");
        textCampaignModelChangesFirst.process(now(), CommonCampaign.START_DATE);
        textCampaignModelChangesFirst.process(now().plusDays(1), CommonCampaign.END_DATE);
        return textCampaignModelChangesFirst;
    }

    private ModelChanges<TextCampaign> getInvalidModelChanges(TextCampaign newTextCampaign) {
        ModelChanges<TextCampaign> textCampaignModelChangesFirst = ModelChanges.build(newTextCampaign,
                CommonCampaign.NAME, "name");
        textCampaignModelChangesFirst.process(now().plusDays(2), CommonCampaign.START_DATE);
        textCampaignModelChangesFirst.process(now().minusDays(1), CommonCampaign.END_DATE);
        return textCampaignModelChangesFirst;
    }
}
