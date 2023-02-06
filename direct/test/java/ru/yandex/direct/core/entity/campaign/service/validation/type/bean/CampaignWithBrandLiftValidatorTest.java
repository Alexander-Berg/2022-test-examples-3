package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.brandLiftExperimentSegmentsCantBeChanged;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithBrandLiftValidatorTest {
    private CampaignWithBrandLiftValidator validator;
    private long sectionId;
    private long abSegmentGoalId;
    private long campaignId;
    private String brandSurveyId;
    private long metrikaCounterId;
    private long abSegmentRetargetingConditionId;
    private CampaignWithBrandLift campaign;
    private Map<Long, BrandSurveyStatus> brandSurveyStatus;
    private List<RetargetingCondition> retargetingConditions;
    private Map<Long, Boolean> isBrandLiftHiddenByCampaignId;
    private Map<Long, String> brandSurveyIdByCampaignId;

    @Before
    public void before() {
        sectionId = nextPositiveLong();
        abSegmentGoalId = nextPositiveLong();
        campaignId = nextPositiveLong();
        brandSurveyId = "brand_survey_id";
        metrikaCounterId = nextPositiveLong();
        abSegmentRetargetingConditionId = nextPositiveLong();

        retargetingConditions = generateRetargetingCondition(List.of(sectionId),
                List.of(abSegmentGoalId));

        campaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withBrandSurveyId(brandSurveyId)
                .withMetrikaCounters(List.of(metrikaCounterId))
                .withAbSegmentRetargetingConditionId(abSegmentRetargetingConditionId)
                .withAbSegmentGoalIds(new ArrayList<>(List.of(abSegmentGoalId)))
                .withSectionIds(new ArrayList<>(List.of(sectionId)))
                .withIsBrandLiftHidden(false);

        brandSurveyStatus = Map.of(campaignId,
                new BrandSurveyStatus().withSurveyStatusDaily(SurveyStatus.ACTIVE));

        isBrandLiftHiddenByCampaignId = new HashMap<>();
        brandSurveyIdByCampaignId = new HashMap<>();

        validator = new CampaignWithBrandLiftValidator(
                Map.of(abSegmentRetargetingConditionId, List.of(abSegmentGoalId)),
                brandSurveyStatus,
                retargetingConditions,
                isBrandLiftHiddenByCampaignId,
                brandSurveyIdByCampaignId);
    }

    @Test
    public void success_AllFieldCorrect() {
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_IncorrectBrandSurveyId() {
        campaign.withBrandSurveyId("short");

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.BRAND_SURVEY_ID)),
                validId())));
    }

    @Test
    public void success_RemoveDraftBrandSurvey() {
        brandSurveyStatus = Map.of(campaignId,
                new BrandSurveyStatus().withSurveyStatusDaily(SurveyStatus.DRAFT));
        campaign.setBrandSurveyId(null);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_RemoveActiveBrandSurvey() {
        campaign.setBrandSurveyId(null);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void success_RemoveActiveBrandSurveyIfBlBecomesNormal() {
        campaign.withBrandSurveyId(null);
        isBrandLiftHiddenByCampaignId.put(campaignId, true);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void success_RemoveActiveBrandSurveyIfItWasAlreadyDeleted() {
        campaign.withBrandSurveyId(null);
        brandSurveyId = null;

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_MetrikaCountersNull() {
        campaign.setMetrikaCounters(null);
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_MetrikaCountersEmpty() {
        campaign.setMetrikaCounters(Collections.emptyList());
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_AbGoalsIdsIsUnknown() {
        campaign.getAbSegmentGoalIds().add(nextPositiveLong());
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS),
                index(1)), inCollection())));
    }

    @Test
    public void error_SectionIdsIsUnknown() {
        campaign.getSectionIds().add(nextPositiveLong());
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.SECTION_IDS), index(1)),
                inCollection())));
    }

    @Test
    public void success_AbGoalsIdsIsUnknownIfBrandLiftIsHidden() {
        campaign.getAbSegmentGoalIds().add(nextPositiveLong());
        campaign.withIsBrandLiftHidden(true);
        isBrandLiftHiddenByCampaignId.put(campaignId, true);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void success_SectionIdsIsUnknownIfBrandLiftIsHidden() {
        campaign.getSectionIds().add(nextPositiveLong());
        campaign.withIsBrandLiftHidden(true);
        isBrandLiftHiddenByCampaignId.put(campaignId, true);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_AbGoalsIdsMustBeEmptyIfCreating() {
        campaign.withIsBrandLiftHidden(true);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    @Test
    public void error_SectionIdsMustBeEmptyIfBrandLiftIsHidden() {
        campaign.withIsBrandLiftHidden(true);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.SECTION_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    @Test
    public void error_CantTurnOffSegments_WhenBrandSurveySet() {
        campaign.setAbSegmentGoalIds(List.of(nextPositiveLong()));
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    @Test
    public void success_CanTurnOffSegments_WhenBrandSurveySetAndBrandLiftChangedToNormal() {
        campaign.setAbSegmentGoalIds(null);
        isBrandLiftHiddenByCampaignId.put(campaignId, true);
        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void success_CanTurnOffSegments_WhenBrandSurveyUnset() {
        List<Long> sectionIds = LongStreamEx.range(CampaignConstants.MAX_EXPERIMENTS_COUNT)
                .boxed()
                .map(id -> nextPositiveLong())
                .toList();
        List<Long> abGoalIds = List.of(nextPositiveLong());

        retargetingConditions = generateRetargetingCondition(sectionIds, abGoalIds);

        campaign.setBrandSurveyId(null);
        campaign.setSectionIds(sectionIds);
        campaign.setAbSegmentGoalIds(abGoalIds);

        brandSurveyStatus = Map.of(campaignId,
                new BrandSurveyStatus().withSurveyStatusDaily(SurveyStatus.DRAFT));

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void error_ExperimentCountExceeded() {
        List<Long> sectionIds = LongStreamEx.range(CampaignConstants.MAX_EXPERIMENTS_COUNT + 1)
                .boxed()
                .map(id -> nextPositiveLong())
                .toList();

        retargetingConditions = generateRetargetingCondition(sectionIds, List.of(abSegmentGoalId));
        campaign.setSectionIds(sectionIds);

        ValidationResult<CampaignWithBrandLift, Defect> vr = createValidatorAndApply();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithBrandLift.SECTION_IDS)),
                maxCollectionSize(CampaignConstants.MAX_EXPERIMENTS_COUNT))));
    }

    private ValidationResult<CampaignWithBrandLift, Defect> createValidatorAndApply() {
        validator = new CampaignWithBrandLiftValidator(
                Map.of(abSegmentRetargetingConditionId, List.of(abSegmentGoalId)),
                brandSurveyStatus,
                retargetingConditions,
                isBrandLiftHiddenByCampaignId,
                brandSurveyId != null ? Map.of(campaignId, brandSurveyId) : emptyMap());

        return validator.apply(campaign);
    }

    private static List<RetargetingCondition> generateRetargetingCondition(List<Long> sectionIds,
                                                                           List<Long> abSegmentIds) {
        List<Long> sectionsIdsNew = new ArrayList<>(sectionIds);
        List<Long> abSegmentIdsNew = new ArrayList<>(abSegmentIds);
        while (sectionsIdsNew.size() < abSegmentIdsNew.size()) {
            sectionsIdsNew.add(null);
        }
        while (abSegmentIdsNew.size() < sectionsIdsNew.size()) {
            abSegmentIdsNew.add(nextPositiveLong());
        }

        return EntryStream.of(abSegmentIdsNew)
                .mapKeyValue((index, abSegmentId) -> new RetargetingCondition()
                        .withId(abSegmentId)
                        .withSectionId(sectionsIdsNew.get(index)))
                .toList();
    }
}
