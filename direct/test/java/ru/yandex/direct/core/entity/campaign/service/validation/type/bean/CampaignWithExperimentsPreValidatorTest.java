package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithExperiments;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithExperimentsPreValidatorTest {

    private static final boolean AB_SEGMENTS_ENABLED = true;
    private static final boolean AB_SEGMENTS_DISABLED = false;
    private static final boolean AUTO_CREATION_ENABLED = true;
    private static final boolean AUTO_CREATION_DISABLED = false;
    private static final boolean VALIDATION_OK = false;
    private static final boolean VALIDATION_ERROR = true;
    private static final long ID = 1L;

    @SuppressWarnings("unused") // See @Parameters annotations usages here
    private static Object[] parametersForAbSegmentRetargetingConditionIds() {
        return new Object[][]{
                // value null: different features => validation ok
                {
                        List.of(),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_DISABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_ENABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_DISABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_ENABLED,
                        null,
                        VALIDATION_OK
                },
                // value not null, different features
                {
                        List.of(ID),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_DISABLED,
                        ID,
                        VALIDATION_ERROR
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_ENABLED,
                        ID,
                        VALIDATION_ERROR
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_DISABLED,
                        ID,
                        VALIDATION_OK
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_ENABLED,
                        ID,
                        VALIDATION_ERROR
                },
        };
    }

    @SuppressWarnings("unused") // See @Parameters annotations usages here
    private static Object[] parametersForSectionAndAbSegmentGoalIds() {
        return new Object[][]{
                // value null: different features => validation ok
                {
                        List.of(),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_DISABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_ENABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_DISABLED,
                        null,
                        VALIDATION_OK
                },
                {
                        List.of(),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_ENABLED,
                        null,
                        VALIDATION_OK
                },
                // value not null, different features
                {
                        List.of(ID),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_DISABLED,
                        ID,
                        VALIDATION_ERROR
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_DISABLED,
                        AUTO_CREATION_ENABLED,
                        ID,
                        VALIDATION_ERROR
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_DISABLED,
                        ID,
                        VALIDATION_ERROR
                },
                {
                        List.of(ID),
                        AB_SEGMENTS_ENABLED,
                        AUTO_CREATION_ENABLED,
                        ID,
                        VALIDATION_OK
                },
        };
    }

    // update prevalidator
    @Test
    @Parameters(method = "parametersForAbSegmentRetargetingConditionIds")
    @TestCaseName("[{index}]")
    public void update_testAbSegmentRetargetingConditionId(List<Long> existedIds,
                                                           boolean abSegmentsEnabledForClientId,
                                                           boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                                           Long value,
                                                           boolean expectErrors) {

        CampaignWithExperimentsUpdatePreValidator validator = new CampaignWithExperimentsUpdatePreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign();
        ModelChanges<CampaignWithExperiments> changes = new ModelChanges<>(campaign.getId(),
                CampaignWithExperiments.class)
                .process(value, TextCampaign.AB_SEGMENT_RETARGETING_CONDITION_ID);

        ValidationResult<ModelChanges<CampaignWithExperiments>, Defect> vr = validator.apply(changes);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForAbSegmentRetargetingConditionIds")
    @TestCaseName("[{index}]")
    public void update_testAbSegmentStatisticRetargetingConditionId(List<Long> existedIds,
                                                                    boolean abSegmentsEnabledForClientId,
                                                                    boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                                                    Long value,
                                                                    boolean expectErrors) {

        CampaignWithExperimentsUpdatePreValidator validator = new CampaignWithExperimentsUpdatePreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign();
        ModelChanges<CampaignWithExperiments> changes = new ModelChanges<>(campaign.getId(),
                CampaignWithExperiments.class)
                .process(value, TextCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID);

        ValidationResult<ModelChanges<CampaignWithExperiments>, Defect> vr = validator.apply(changes);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForSectionAndAbSegmentGoalIds")
    @TestCaseName("[{index}]")
    public void update_testSectionIds(List<Long> existedIds,
                                      boolean abSegmentsEnabledForClientId,
                                      boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                      Long value,
                                      boolean expectErrors) {

        List<RetargetingCondition> retargetingConditions = StreamEx.of(existedIds)
                .map(id -> new RetargetingCondition().withSectionId(id))
                .toList();

        CampaignWithExperimentsUpdatePreValidator validator = new CampaignWithExperimentsUpdatePreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign();

        ModelChanges<CampaignWithExperiments> changes = new ModelChanges<>(campaign.getId(),
                CampaignWithExperiments.class)
                .process(ifNotNull(value, List::of), TextCampaign.SECTION_IDS);

        ValidationResult<ModelChanges<CampaignWithExperiments>, Defect> vr = validator.apply(changes);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForSectionAndAbSegmentGoalIds")
    @TestCaseName("[{index}]")
    public void update_testAbSegmentGoalIds(List<Long> existedIds,
                                            boolean abSegmentsEnabledForClientId,
                                            boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                            Long value,
                                            boolean expectErrors) {

        List<RetargetingCondition> retargetingConditions = StreamEx.of(existedIds)
                .map(id -> new RetargetingCondition().withId(id))
                .toList();

        CampaignWithExperimentsUpdatePreValidator validator = new CampaignWithExperimentsUpdatePreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign();
        ModelChanges<CampaignWithExperiments> changes = new ModelChanges<>(campaign.getId(),
                CampaignWithExperiments.class)
                .process(ifNotNull(value, List::of), TextCampaign.AB_SEGMENT_GOAL_IDS);

        ValidationResult<ModelChanges<CampaignWithExperiments>, Defect> vr = validator.apply(changes);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    // add prevalidator
    @Test
    @Parameters(method = "parametersForAbSegmentRetargetingConditionIds")
    @TestCaseName("[{index}]")
    public void add_testAbSegmentRetargetingConditionId(List<Long> existedIds,
                                                        boolean abSegmentsEnabledForClientId,
                                                        boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                                        Long value,
                                                        boolean expectErrors) {

        CampaignWithExperimentsAddPreValidator validator = new CampaignWithExperimentsAddPreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign()
                .withAbSegmentRetargetingConditionId(value);

        ValidationResult<CampaignWithExperiments, Defect> vr = validator.apply(campaign);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForAbSegmentRetargetingConditionIds")
    @TestCaseName("[{index}]")
    public void add_testAbSegmentStatisticRetargetingConditionId(List<Long> existedIds,
                                                                 boolean abSegmentsEnabledForClientId,
                                                                 boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                                                 Long value,
                                                                 boolean expectErrors) {

        CampaignWithExperimentsAddPreValidator validator = new CampaignWithExperimentsAddPreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign()
                .withAbSegmentStatisticRetargetingConditionId(value);
        ModelChanges<CampaignWithExperiments> changes = new ModelChanges<>(campaign.getId(),
                CampaignWithExperiments.class)
                .process(value, TextCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID);

        ValidationResult<CampaignWithExperiments, Defect> vr = validator.apply(campaign);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForSectionAndAbSegmentGoalIds")
    @TestCaseName("[{index}]")
    public void add_testSectionIds(List<Long> existedIds,
                                   boolean abSegmentsEnabledForClientId,
                                   boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                   Long value,
                                   boolean expectErrors) {

        List<RetargetingCondition> retargetingConditions = StreamEx.of(existedIds)
                .map(id -> new RetargetingCondition().withSectionId(id))
                .toList();

        CampaignWithExperimentsAddPreValidator validator = new CampaignWithExperimentsAddPreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign()
                .withSectionIds(ifNotNull(value, List::of));

        ValidationResult<CampaignWithExperiments, Defect> vr = validator.apply(campaign);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }

    @Test
    @Parameters(method = "parametersForSectionAndAbSegmentGoalIds")
    @TestCaseName("[{index}]")
    public void add_testAbSegmentGoalIds(List<Long> existedIds,
                                         boolean abSegmentsEnabledForClientId,
                                         boolean experimentRetargetingConditionsAutoCreatingEnabledForClientId,
                                         Long value,
                                         boolean expectErrors) {
        List<RetargetingCondition> retargetingConditions = StreamEx.of(existedIds)
                .map(id -> new RetargetingCondition().withId(id))
                .toList();

        CampaignWithExperimentsAddPreValidator validator = new CampaignWithExperimentsAddPreValidator(
                abSegmentsEnabledForClientId,
                experimentRetargetingConditionsAutoCreatingEnabledForClientId
        );

        CampaignWithExperiments campaign = new TextCampaign()
                .withAbSegmentGoalIds(ifNotNull(value, List::of));

        ValidationResult<CampaignWithExperiments, Defect> vr = validator.apply(campaign);

        assertThat(vr.hasAnyErrors()).isEqualTo(expectErrors);
    }
}
