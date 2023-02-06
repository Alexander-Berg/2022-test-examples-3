package ru.yandex.direct.internaltools.tools.moderationdiags;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.moderationdiags.model.UpdateModerationDiagsBanIsProhibitedParams;
import ru.yandex.direct.internaltools.tools.moderationdiags.model.UpdateModerationDiagsBanIsProhibitedResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiag1;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiag2;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiagPerformance;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateModerationDiagsUnbanIsProhibitedToolTest {

    private static final CompareStrategy MODERATE_DIAGS_COMPARE_STRATEGY = allFieldsExcept(newPath("diagText"));

    @Autowired
    private Steps steps;

    @Autowired
    private UpdateModerationDiagsUnbanIsProhibitedTool updateModerationDiagsUnbanIsProhibitedTool;

    private ModerationDiag criticalModerationDiag = createModerationDiag1();
    private ModerationDiag nonCriticalModerationDiag = createModerationDiag2();
    private ModerationDiag performanceNonCriticalModerationDiag = createModerationDiagPerformance();

    @Before
    public void before() {
        steps.moderationDiagSteps().insertStandartDiags();

        criticalModerationDiag = createModerationDiag1();
        nonCriticalModerationDiag = createModerationDiag2();
        performanceNonCriticalModerationDiag = createModerationDiagPerformance();
    }

    @Test
    public void getMassData_UpdateOneNonCriticalToCritical() {
         List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(String.valueOf(nonCriticalModerationDiag.getId()))
                        .withSetBanIsProhibited(true));

         assertThat(results,
                contains(new UpdateModerationDiagsBanIsProhibitedResult(nonCriticalModerationDiag.getId(), true)));
         checkModerationDiag(nonCriticalModerationDiag.getId(), nonCriticalModerationDiag.withUnbanIsProhibited(true));
    }

    @Test
    public void getMassData_UpdateOneCriticalToNonCritical() {
        List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(String.valueOf(criticalModerationDiag.getId()))
                        .withSetBanIsProhibited(false));

        assertThat(results,
                contains(new UpdateModerationDiagsBanIsProhibitedResult(criticalModerationDiag.getId(), false)));
        checkModerationDiag(criticalModerationDiag.getId(), criticalModerationDiag.withUnbanIsProhibited(false));
    }

    @Test
    public void getMassData_UpdateOnePerformanceNonCriticalToCritical() {
        List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(String.valueOf(performanceNonCriticalModerationDiag.getId()))
                        .withSetBanIsProhibited(true));

        assertThat(results,
                contains(new UpdateModerationDiagsBanIsProhibitedResult(performanceNonCriticalModerationDiag.getId(),
                        true)));
        checkModerationDiag(performanceNonCriticalModerationDiag.getId(),
                performanceNonCriticalModerationDiag.withUnbanIsProhibited(true));
    }

    @Test
    public void getMassData_UpdateMultiple() {
        List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(
                                nonCriticalModerationDiag.getId() + "," + performanceNonCriticalModerationDiag.getId())
                        .withSetBanIsProhibited(true));

        assertThat(results, containsInAnyOrder(
                new UpdateModerationDiagsBanIsProhibitedResult(nonCriticalModerationDiag.getId(), true),
                new UpdateModerationDiagsBanIsProhibitedResult(performanceNonCriticalModerationDiag.getId(), true)));
        checkModerationDiag(nonCriticalModerationDiag.getId(),
                nonCriticalModerationDiag.withUnbanIsProhibited(true));
        checkModerationDiag(performanceNonCriticalModerationDiag.getId(),
                performanceNonCriticalModerationDiag.withUnbanIsProhibited(true));
    }

    @Test
    public void getMassData_UpdateNonCriticalToNonCritical_AnotherModerationDiagNotChanged() {
        List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(String.valueOf(nonCriticalModerationDiag.getId()))
                        .withSetBanIsProhibited(false));

        assertThat(results, contains(new UpdateModerationDiagsBanIsProhibitedResult(nonCriticalModerationDiag.getId(), false)));
        checkModerationDiag(criticalModerationDiag.getId(), criticalModerationDiag);
    }

    @Test
    public void getMassData_UpdateCriticalToCritical_AnotherModerationDiagNotChanged() {
        List<UpdateModerationDiagsBanIsProhibitedResult> results = updateModerationDiagsUnbanIsProhibitedTool.getMassData(
                new UpdateModerationDiagsBanIsProhibitedParams()
                        .withModerationDiagsIds(String.valueOf(criticalModerationDiag.getId()))
                        .withSetBanIsProhibited(true));

        assertThat(results, contains(new UpdateModerationDiagsBanIsProhibitedResult(criticalModerationDiag.getId(), true)));
        checkModerationDiag(nonCriticalModerationDiag.getId(), nonCriticalModerationDiag);
    }

    private void checkModerationDiag(Long moderationDiagId, ModerationDiag expected) {
        List<ModerationDiag> actual =
                steps.moderationDiagSteps().getModerationDiags(List.of(moderationDiagId));

        assertThat(actual, hasSize(1));
        assertThat(actual.get(0), beanDiffer(expected).useCompareStrategy(MODERATE_DIAGS_COMPARE_STRATEGY));
    }
}
