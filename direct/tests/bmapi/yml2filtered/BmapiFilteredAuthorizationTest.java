package ru.yandex.autotests.directintapi.tests.bmapi.yml2filtered;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.BmapiRequest;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Filters;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.HashMap;

import static org.hamcrest.Matchers.greaterThan;

/**
 * Created by pavryabov on 22.09.15.
 * https://st.yandex-team.ru/TESTIRT-6810
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Работа с фидами, требующими авторизацию")
@Issue("https://st.yandex-team.ru/IRT-263")
public class BmapiFilteredAuthorizationTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String HOLODILNIK_URL = "http://holodilnik.ru/yandex/create_xml.php";
    public static final String LOGIN = "yandex";
    public static final String PASS = "skAMuyf";

    @Test
    public void checkAuthorizationError() {
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("price > ", 1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(HOLODILNIK_URL)
                        .withFilters(filters),
                0
        );
    }

    @Test
    public void checkRightAuthorization() {
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("price > ", 1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(HOLODILNIK_URL)
                        .withLogin(LOGIN)
                        .withPass(PASS)
                        .withFilters(filters),
                greaterThan(0)
        );
    }
}
