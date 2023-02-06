package ru.yandex.autotests.mediaplan.mediaplans.get;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.MediaplannerUIDs;
import ru.yandex.autotests.mediaplan.TestFeatures;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_mediaplans.Api5GetMediaplansResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_mediaplans.Mediaplan;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_mediaplans.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.steps.UserStepsMediaplan;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(TestFeatures.REQUESTS_GET)
@Description("Проверка работы SelectionCriteria метода Mediaplans.Get")
public class MediaplanGetSelectionCriteriaTest {
    private static Long uid = MediaplannerUIDs.MEDIAPLANNER_UID1;

    public static final UserStepsMediaplan userSteps = new UserStepsMediaplan();
    @Rule
    public MediaplanRule mediaplanRule = new MediaplanRule();

    @Test
    public void getByUID() {
        Api5GetMediaplansResult getOutputData =
                userSteps.mediaplansSteps().api5MediaplansGetByMediaplannerUID(uid);
        List<Long> ids = getOutputData.getMediaplans().stream()
                .map(mediaplan -> mediaplan.getId())
                .collect(Collectors.toList());
        assertThat("в ответе содержится ожидаемый медиаплан", ids, equalTo(mediaplanRule.getMediaplanIds()));
    }

    @Test
    public void getByIds() {
        Api5GetMediaplansResult getOutputData =
                userSteps.mediaplansSteps().api5MediaplansGetByIds(mediaplanRule.getMediaplanIds());
        List<Long> ids = getOutputData.getMediaplans().stream()
                .map(mediaplan -> mediaplan.getId())
                .collect(Collectors.toList());
        assertThat("в ответе содержится ожидаемый медиаплан", ids, equalTo(mediaplanRule.getMediaplanIds()));
    }

    @Test
    public void getByRequestID() {
        Api5GetMediaplansResult getOutputData =
                userSteps.mediaplansSteps().api5MediaplansGetByRequestID(mediaplanRule.getRequestId());
        List<Long> ids = getOutputData.getMediaplans().stream()
                .map(mediaplan -> mediaplan.getId())
                .collect(Collectors.toList());
        assertThat("в ответе содержится ожидаемый медиаплан", ids, equalTo(mediaplanRule.getMediaplanIds()));
    }

    @Test
    public void getByEmptySelectionCriteria() {
        Api5GetMediaplansResult getOutputData =
                userSteps.mediaplansSteps().api5MediaplansGet(new SelectionCriteria());
        List<Long> ids = getOutputData.getMediaplans().stream()
                .map(Mediaplan::getId)
                .collect(Collectors.toList());
        assertThat("в ответе содержится ожидаемый медиаплан", ids, equalTo(mediaplanRule.getMediaplanIds()));
    }

    @Test
    public void getByAllFields() {
        Api5GetMediaplansResult getOutputData =
                userSteps.mediaplansSteps().api5MediaplansGet(
                        new SelectionCriteria()
                            .withIds(mediaplanRule.getMediaplanIds())
                            .withMediaplannerUID(uid)
                            .withRequestID(mediaplanRule.getRequestId())
                );
        List<Long> ids = getOutputData.getMediaplans().stream()
                .map(mediaplan -> mediaplan.getId())
                .collect(Collectors.toList());
        assertThat("в ответе содержится ожидаемый медиаплан", ids, equalTo(mediaplanRule.getMediaplanIds()));
    }
}
