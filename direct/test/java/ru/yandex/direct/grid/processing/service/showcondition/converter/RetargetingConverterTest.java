package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingCondition;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionGoalsInfo;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.shortcuts;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_DESCRIPTION;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_DESCRIPTION;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_NAME;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType.SHORTCUTS;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class RetargetingConverterTest {
    private final ClientId clientId = ClientId.fromLong(1L);

    @Test
    public void testToGdRetargetingCondition() {
        // Метод используется для маппинга доступных шорткатов, поэтому и тестируем его на шорткатах

        // Актуальные данные по шорткатам можно получить следующим образом
        /*List<RetargetingCondition> shortcuts = retargetingConditionShortcutService
                .getAvailableTruncatedRetargetingConditionShortcuts(clientId, true, false, false);*/

        List<RetargetingCondition> shortcuts = List.of(
                getRetargetingConditionShortcut(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_SHORTCUT_NAME,
                        NOT_BOUNCE_SHORTCUT_DESCRIPTION),
                getRetargetingConditionShortcut(NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_NAME,
                        NOT_BOUNCE_LAL_SHORTCUT_DESCRIPTION)
        );

        var expected = List.of(
                getGdRetargetingConditionShortcut(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_SHORTCUT_NAME,
                        NOT_BOUNCE_SHORTCUT_DESCRIPTION),
                getGdRetargetingConditionShortcut(NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_NAME,
                        NOT_BOUNCE_LAL_SHORTCUT_DESCRIPTION)
        );
        var gdShortcuts = mapList(shortcuts, RetargetingConverter::toGdRetargetingCondition);

        assertThat(gdShortcuts, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    private RetargetingCondition getRetargetingConditionShortcut(Long id, String name, String description) {
        return (RetargetingCondition) new RetargetingCondition()
                .withId(id)
                .withName(name)
                .withDescription(description)
                .withRules(List.of())
                .withClientId(clientId.asLong())
                .withType(shortcuts)
                .withAvailable(true)
                .withInterest(false)
                .withDeleted(false)
                .withAutoRetargeting(false);
    }

    private GdRetargetingCondition getGdRetargetingConditionShortcut(Long id, String name, String description) {
        return new GdRetargetingCondition()
                .withRetargetingConditionId(id)
                .withType(SHORTCUTS)
                .withName(name)
                .withInterest(false)
                .withDescription(description)
                .withConditionRules(List.of())
                .withHasUnavailableGoals(false)
                .withAvailableForRetargeting(true)
                .withGoalsInfo(new GdRetargetingConditionGoalsInfo()
                        .withHasGeoSegments(false)
                        .withGoalDomains(Set.of())
                        .withGoalIds(Set.of())
                        .withHasUnavailableGoals(false))
                .withAdGroupIds(Collections.emptySet())
                .withCampaigns(Collections.emptyList());
    }
}
