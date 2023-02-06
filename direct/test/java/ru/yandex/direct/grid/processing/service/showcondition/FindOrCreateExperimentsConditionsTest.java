package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditionForExperiments;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditionForExperimentsItem;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditionForExperimentsPayloadItem;
import ru.yandex.direct.grid.processing.service.showcondition.retargeting.RetargetingDataService;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindOrCreateExperimentsConditionsTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private RetargetingDataService retargetingDataService;

    @Autowired
    private Steps steps;

    private List<Goal> goals;
    private ClientInfo defaultClient;
    private List<Long> sectionIds;
    private List<Long> unexistedSectionIds;
    private List<Long> unexistedAbSegmentIds;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        sectionIds = List.of(1L, 2L);
        unexistedSectionIds = List.of(100L);
        unexistedAbSegmentIds = List.of(200L);

        goals = List.of((Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(0)),
                (Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(0)),
                (Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(1)));


        metrikaClientStub.addGoals(defaultClient.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(defaultClient.getUid(), listToSet(goals, GoalBase::getId));
    }

    @Test
    public void findOrCreate_OneCondition() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(null);
        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> result =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRetargetingConditionId())
                .isNull();
    }

    @Test
    public void findOrCreate_TwoDifferentConditions() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(mapList(List.of(goals.get(0), goals.get(2)), GoalBase::getId));
        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> result =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRetargetingConditionId())
                .isNotEqualTo(result.get(0).getStatisticRetargetingConditionId());
    }

    @Test
    public void findOrCreate_TwoEqualConditions() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(mapList(List.of(goals.get(0), goals.get(1), goals.get(2)),
                                GoalBase::getId));

        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> result =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRetargetingConditionId())
                .isEqualTo(result.get(0).getStatisticRetargetingConditionId());
    }

    @Test
    public void findOrCreate_TwoDifferentConditions_WhenConditionsAlreadyExists() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(mapList(List.of(goals.get(0), goals.get(2)), GoalBase::getId));

        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> firstResult =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> result =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(firstResult);
    }

    @Test
    public void findOrCreate_FewItems() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(mapList(List.of(goals.get(0), goals.get(2)), GoalBase::getId));

        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments,
                        gdCreateRetargetingForExperiments));

        List<GdCreateRetargetingConditionForExperimentsPayloadItem> result =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getAddedConditions();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(result.get(1));
    }

    @Test
    public void findOrCreate_sectionIdNotFound() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(unexistedSectionIds)
                        .withAbSegmentGoalIds(null);

        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        GdValidationResult actualValidationResult =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getValidationResult();


        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdCreateRetargetingConditionForExperiments.ITEMS.name()), index(0),
                        field(GdCreateRetargetingConditionForExperimentsItem.SECTION_IDS.name()), index(0)),
                CollectionDefects.inCollection(),
                true)
                .withWarnings(emptyList());

        assertThat(actualValidationResult).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void findOrCreate_abSegmentIdNotFound() {
        GdCreateRetargetingConditionForExperimentsItem gdCreateRetargetingForExperiments =
                new GdCreateRetargetingConditionForExperimentsItem()
                        .withSectionIds(sectionIds)
                        .withAbSegmentGoalIds(unexistedAbSegmentIds);

        GdCreateRetargetingConditionForExperiments input =
                new GdCreateRetargetingConditionForExperiments().withItems(List.of(gdCreateRetargetingForExperiments));

        GdValidationResult actualValidationResult =
                retargetingDataService.findOrCreateExperimentsRetargetingConditions(defaultClient.getClientId(),
                        input).getValidationResult();

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdCreateRetargetingConditionForExperiments.ITEMS.name()), index(0),
                        field(GdCreateRetargetingConditionForExperimentsItem.AB_SEGMENT_GOAL_IDS.name()), index(0)),
                CollectionDefects.inCollection(),
                true)
                .withWarnings(emptyList());

        assertThat(actualValidationResult).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }
}
