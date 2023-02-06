package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.retargeting.Constants.INTEREST_LINK_TIME_VALUE;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.metrika_goals;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.audienceTargetNotFound;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingValidationServicePrevalidateTest {

    private static final long TARGETING_CATEGORY_IMPORT_ID = 21672155L;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private UpdateRetargetingValidationService updateRetargetingValidationService;

    @Autowired
    private TestTargetingCategoriesRepository targetingCategoriesRepository;

    private RetargetingInfo retargetingInfo;
    private long operatorUid;
    private int shard;

    @Before
    public void before() {
        retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();
        operatorUid = retargetingInfo.getUid();
        shard = retargetingInfo.getShard();

        var category = new TargetingCategory(51L, null, "Утилиты", "TOOLS",
                BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID), true);
        targetingCategoriesRepository.addTargetingCategory(category);
    }

    @Test
    public void preValidate_ValidChanges_NoErrors() {
        ModelChanges<Retargeting> mc = suspendedModelChange(retargetingInfo.getRetargetingId(), true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr.hasAnyErrors(), is(false));
        assertThat(vr.hasAnyWarnings(), is(false));
    }

    @Test
    public void preValidate_NullId_NotNullIdDefect() {
        ModelChanges<Retargeting> mc = suspendedModelChange(null, true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                CommonDefects.notNull())));
    }

    @Test
    public void preValidate_NotPositiveId_InvalidIdDefectDefinition() {
        ModelChanges<Retargeting> mc = suspendedModelChange(0L, true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                CommonDefects.validId())));
    }

    @Test
    public void preValidate_NotVisibleCompanyForOperatorUid_NotFoundRetargetingDefectDefinition() {
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();

        ModelChanges<Retargeting> mc = suspendedModelChange(retargetingInfo.getRetargetingId(), true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, retargetingInfo.getShard());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RetargetingDefects.notFoundRetargeting())));
    }

    @Test
    public void preValidate_SuspendOnArchivedCompany_badCampaignStatusArchivedOnSuspend() {
        archiveCampaign(retargetingInfo.getCampaignId(), retargetingInfo.getShard());

        ModelChanges<Retargeting> mc = suspendedModelChange(retargetingInfo.getRetargetingId(), true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RetargetingDefects.badCampaignStatusArchivedOnSuspend(retargetingInfo.getCampaignId()))));
    }

    @Test
    public void preValidate_ResumeOnArchivedCompany_BadCampaignStatusArchivedOnResume() {
        archiveCampaign(retargetingInfo.getCampaignId(), retargetingInfo.getShard());

        ModelChanges<Retargeting> mc = suspendedModelChange(retargetingInfo.getRetargetingId(), false);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RetargetingDefects.badCampaignStatusArchivedOnResume(retargetingInfo.getCampaignId()))));
    }

    @Test
    public void preValidate_SuspendAlreadySuspended_WarningNotSuspendedRetargeting() {
        RetargetingInfo suspendedRetargetingInfo = steps.retargetingSteps().createRetargeting(
                retargetingInfo.getRetargeting().withId(null).withIsSuspended(true));

        ModelChanges<Retargeting> mc = suspendedModelChange(suspendedRetargetingInfo.getRetargetingId(), true);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), suspendedRetargetingInfo.getUid(), suspendedRetargetingInfo.getShard());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RetargetingDefects.warningAlreadySuspendedRetargeting())));
    }

    @Test
    public void preValidate_ResumeNotSuspended_WarningNotSuspendedRetargeting() {
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();

        ModelChanges<Retargeting> mc = suspendedModelChange(retargetingInfo.getRetargetingId(), false);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), retargetingInfo.getUid(), retargetingInfo.getShard());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                RetargetingDefects.warningNotSuspendedRetargeting())));
    }

    /**
     * Когда указан retConditionId интереса, но retargetingId не существует
     */
    @Test
    public void preValidate_WrongRetargetingIdOfTargetInterest_AudienceNotFound() {
        RetConditionInfo retConditionInfo = createRetargetingConditionForInterest();

        Long wrongRetargetingId = 555L;
        ModelChanges<Retargeting> mc = new ModelChanges<>(wrongRetargetingId, Retargeting.class)
                .process(retConditionInfo.getRetConditionId(), Retargeting.RETARGETING_CONDITION_ID);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                audienceTargetNotFound())));
    }

    @Test
    public void preValidate_WithValidTargetInterest_NoErrors() {
        RetConditionInfo retConditionInfo = createRetargetingConditionForInterest();

        TargetInterest targetInterest = TestRetargetings.defaultTargetInterest(
                retargetingInfo.getCampaignId(), retargetingInfo.getAdGroupId(), retConditionInfo.getRetConditionId());

        RetargetingInfo retargetingInterestInfo = steps.retargetingSteps()
                .createRetargeting(targetInterest, retargetingInfo.getAdGroupInfo(), retConditionInfo);

        ModelChanges<Retargeting> mc = new ModelChanges<>(retargetingInterestInfo.getRetargetingId(), Retargeting.class)
                .process(retConditionInfo.getRetConditionId(), Retargeting.RETARGETING_CONDITION_ID);

        ValidationResult<List<ModelChanges<Retargeting>>, Defect> vr =
                preValidate(singletonList(mc), operatorUid, shard);

        assertThat(vr.hasAnyErrors(), is(false));
        assertThat(vr.hasAnyWarnings(), is(false));
    }

    private RetConditionInfo createRetargetingConditionForInterest() {
        Goal clientGoal = (Goal) new Goal()
                .withId(TARGETING_CATEGORY_IMPORT_ID)
                .withTime(INTEREST_LINK_TIME_VALUE);
        Rule clientRule = new Rule()
                .withType(RuleType.ALL)
                .withGoals(singletonList(clientGoal));
        RetargetingCondition clientRetargetingCondition = new RetargetingCondition();
        clientRetargetingCondition.withType(metrika_goals)
                .withName("")
                .withClientId(retargetingInfo.getClientId().asLong())
                .withLastChangeTime(LocalDateTime.now())
                .withInterest(true)
                .withDeleted(false)
                .withRules(singletonList(clientRule));
        return steps.retConditionSteps()
                .createRetCondition(clientRetargetingCondition, retargetingInfo.getClientInfo());
    }

    private void archiveCampaign(long campaignId, int shard) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    private ValidationResult<List<ModelChanges<Retargeting>>, Defect> preValidate(
            List<ModelChanges<Retargeting>> modelChanges, long operatorUid, int shard) {
        return updateRetargetingValidationService
                .preValidate(modelChanges, retargetingInfo.getClientId(), operatorUid, shard);
    }

    private ModelChanges<Retargeting> suspendedModelChange(Long retargetingId, boolean isSuspended) {
        return new ModelChanges<>(retargetingId, Retargeting.class).process(isSuspended, Retargeting.IS_SUSPENDED);
    }

    private ModelChanges<Retargeting> priceContextModelChange(Long retargetingId, BigDecimal priceContext) {
        return new ModelChanges<>(retargetingId, Retargeting.class).process(priceContext, Retargeting.PRICE_CONTEXT);
    }

}
