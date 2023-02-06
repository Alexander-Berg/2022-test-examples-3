package ru.yandex.autotests.mediaplan.requests.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_requests.Api5AddRequestsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_requests.ParamsApi5AddRequests;
import ru.yandex.autotests.mediaplan.steps.UserStepsMediaplan;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.MEDIAPLANS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddRequestsFactory.defaultAddRequest;
import static ru.yandex.autotests.mediaplan.datafactories.AddRequestsFactory.twoDefaultRequest;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(MEDIAPLANS_ADD)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Создание запроса")
@RunWith(Parameterized.class)
public class RequestPositiveTest {
    protected UserStepsMediaplan userSteps = new UserStepsMediaplan();

    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddRequests requests;
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Один запрос", defaultAddRequest()},
                {"Два запроса", twoDefaultRequest()},
        });
    }
    @Test
    public void createMediaPlan() {
        Api5AddRequestsResult requestOutputData = userSteps.requestsSteps().api5RequestsAdd(requests);
        assumeThat("ошибок при создании заявки не получено", requestOutputData.getAddResults().get(0).getErrors(), hasSize(0));
        assertThat("вернулась одна заявка", requestOutputData.getAddResults().size(), equalTo(requests.getRequests().size()));
    }
}
