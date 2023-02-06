package ru.yandex.direct.core.entity.campaign.service.validation.type.update.bean;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.CampaignWithBroadMatchValidator;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BROAD_MATCH_LIMIT_MAX;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BROAD_MATCH_LIMIT_MIN;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


public class CampaignWithBroadMatchValidatorTest {

    private static final Set<Long> CLIENT_GOAL_IDS = Set.of(1L, 2L, 3L);
    private CampaignWithBroadMatchValidator validator;

    @Before
    public void before() {
        initValidator(CLIENT_GOAL_IDS);
    }

    private void initValidator(Set<Long> clientGoalIds) {
        validator = CampaignWithBroadMatchValidator.build(clientGoalIds);
    }

    @Test
    public void success_AllFields() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)
                        .withBroadMatchGoalId(1L));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void success_ZeroGoalId() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)
                        .withBroadMatchGoalId(0L));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void success_WithoutGoalId() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)
                        .withBroadMatchGoalId(null));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void error_LimitIsNull() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(null)
                        .withBroadMatchGoalId(123L));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_LIMIT)), notNull()))));
    }

    @Test
    public void error_GoalIdNotFound() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(BROAD_MATCH_LIMIT_MIN)
                        .withBroadMatchGoalId(123L));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_GOAL_ID)), objectNotFound()))));
    }

    @Test
    public void error_GoalIdNotFoundCampaignWithoutGoals() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(BROAD_MATCH_LIMIT_MIN)
                        .withBroadMatchGoalId(123L));

        initValidator(null);

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_GOAL_ID)), objectNotFound()))));
    }

    @Test
    public void error_GoalIdNegative() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(BROAD_MATCH_LIMIT_MAX)
                        .withBroadMatchGoalId(-1L));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_GOAL_ID)), validId()))));
    }

    @Test
    public void error_LimitBelowMin() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(BROAD_MATCH_LIMIT_MIN - 1));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_LIMIT)), inInterval(BROAD_MATCH_LIMIT_MIN, BROAD_MATCH_LIMIT_MAX)))));
    }

    @Test
    public void error_LimitAboveMax() {
        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(BROAD_MATCH_LIMIT_MAX + 1));

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(
                hasDefectWithDefinition(validationError(path(field(CampaignWithBroadMatch.BROAD_MATCH),
                        field(BroadMatch.BROAD_MATCH_LIMIT)), inInterval(BROAD_MATCH_LIMIT_MIN, BROAD_MATCH_LIMIT_MAX)))));
    }

    @Test
    public void success_StrategyBoth() {
        DbStrategy strategy = (DbStrategy) defaultAutobudgetStrategy()
                .withPlatform(CampaignsPlatform.BOTH);

        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)
                        .withBroadMatchGoalId(1L))
                .withStrategy(strategy);

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void success_StrategySearch() {
        DbStrategy strategy = (DbStrategy) defaultAutobudgetStrategy()
                .withPlatform(CampaignsPlatform.SEARCH);

        CampaignWithBroadMatch campaign = new TextCampaign()
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)
                        .withBroadMatchGoalId(1L))
                .withStrategy(strategy);

        ValidationResult<CampaignWithBroadMatch, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

}
