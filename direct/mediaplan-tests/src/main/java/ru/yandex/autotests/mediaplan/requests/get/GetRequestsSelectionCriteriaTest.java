package ru.yandex.autotests.mediaplan.requests.get;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.TestFeatures;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_requests.Api5GetRequestsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_requests.Request;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_get_requests.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.steps.UserStepsMediaplan;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

/**
 * Changes by ginger on 16.02.16.
 * https://st.yandex-team.ru/TESTIRT-8573
 */
@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(TestFeatures.REQUESTS_GET)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Проверка работы SelectionCriteria метода Requests.Get")
public class GetRequestsSelectionCriteriaTest {
    private static Long uid = getClient();

    public static final UserStepsMediaplan userSteps = new UserStepsMediaplan();

    @Rule
    public MediaplanRule mediaplanRule = new MediaplanRule();


    @Test
    public void getRequestByUID(){
        //баг
        Api5GetRequestsResult requests = userSteps.requestsSteps().api5RequestsGetByUID(uid);
        List<Long> ids = requests.getRequests().stream()
                .map(Request::getId)
                .collect(Collectors.toList());
        assertThat("вернулась добавленная заявка", ids.toArray(),
                arrayContainingInAnyOrder(mediaplanRule.getRequestIds().toArray()));
    }

    @Test
    public void getRequestByIds(){
        Api5GetRequestsResult requests = userSteps.requestsSteps().api5RequestsGetByIds(mediaplanRule.getRequestIds());
        List<Long> ids = requests.getRequests().stream()
                .map(Request::getId)
                .collect(Collectors.toList());
        assertThat("вернулась добавленная заявка", ids.toArray(),
                arrayContainingInAnyOrder(mediaplanRule.getRequestIds().toArray()));
    }

    @Test
    public void getRequestWithEmptySelectionCriteria(){
        //баг
        Api5GetRequestsResult requests = userSteps.requestsSteps().api5RequestsGet(new SelectionCriteria());
        List<Long> ids = requests.getRequests().stream()
                .map(Request::getId)
                .collect(Collectors.toList());
        assertThat("вернулась добавленная заявка", ids.toArray(),
                arrayContainingInAnyOrder(mediaplanRule.getRequestIds().toArray()));
    }

    @Test
    public void getRequestByAllFields(){
        Api5GetRequestsResult requests = userSteps.requestsSteps().api5RequestsGet(
                new SelectionCriteria()
                    .withMediaplannerUID(uid)
                    .withIds(mediaplanRule.getRequestIds())
        );
        List<Long> ids = requests.getRequests().stream()
                .map(Request::getId)
                .collect(Collectors.toList());
        assertThat("вернулась добавленная заявка", ids.toArray(),
                arrayContainingInAnyOrder(mediaplanRule.getRequestIds().toArray()));
    }
}
