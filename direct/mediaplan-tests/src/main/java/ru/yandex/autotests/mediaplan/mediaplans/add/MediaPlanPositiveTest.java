package ru.yandex.autotests.mediaplan.mediaplans.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_mediaplans.Api5AddMediaplansResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_mediaplans.Mediaplan;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_requests.Api5AddRequestsResult;
import ru.yandex.autotests.mediaplan.steps.UserStepsMediaplan;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.MEDIAPLANS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddMediplanFactory.oneMediaplan;
import static ru.yandex.autotests.mediaplan.datafactories.AddRequestsFactory.defaultAddRequestWithAuthorId;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(MEDIAPLANS_ADD)
@Description("Создание медиаплана")
@RunWith(Parameterized.class)
public class MediaPlanPositiveTest {
    protected UserStepsMediaplan userSteps = new UserStepsMediaplan();

    @Parameterized.Parameter(value = 1)
    public  List<Mediaplan> mediaplans;
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Один медиаплан", oneMediaplan()},
        });
    }
    @Test
    public void createMediaPlan() {
        Api5AddRequestsResult requestOutputData = userSteps.requestsSteps().api5RequestsAdd(defaultAddRequestWithAuthorId(getClient()));
        assumeThat("вернулась одна заявка", requestOutputData.getAddResults().size(), equalTo(1));
        assumeThat("ошибок при создании заявки не получено", requestOutputData.getAddResults().get(0).getErrors(), hasSize(0));
        Long requestId = requestOutputData.getAddResults().get(0).getId();
        mediaplans.stream().map(x->x.withRequestId(requestId)).collect(Collectors.toList());
        Api5AddMediaplansResult addMediaplansResult = userSteps.mediaplansSteps().api5MediaplansAdd(
                mediaplans
        );
        assertThat("сохраненные медипланы соответсвуют ожиданиям", addMediaplansResult.getAddResults(),
                hasSize(mediaplans.size()));
    }
}
