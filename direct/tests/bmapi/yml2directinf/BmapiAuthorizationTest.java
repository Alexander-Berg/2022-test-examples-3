package ru.yandex.autotests.directintapi.tests.bmapi.yml2directinf;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.BmapiRequest;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.BmapiResponse;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.Error;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.ItemOfCategs;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

/**
 * Created by pavryabov on 11.08.15.
 * https://st.yandex-team.ru/TESTIRT-6542
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Работа с фидами, требующими авторизацию")
@Issue("https://st.yandex-team.ru/DIRECT-43907")
public class BmapiAuthorizationTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String FEED_URL = "https://www.sebevdom.ru/export_feed/ya_prod_direct.xml";
    public static final String LOGIN = "sbd13";
    public static final String PASS = "yaproddirect";
    public static final String AUTHORIZATION_ERROR_CODE = "401";

    @Test
    public void checkAuthorizationError() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(FEED_URL)
        );

        BmapiResponse expectedResponse = new BmapiResponse()
                .withErrors(new Error().withCode(Error.DOWNLOAD_ERROR))
                .withEmptyWarnings()
                .withCategs(new ItemOfCategs[]{});
        assertThat("yml2directinf вернула правильный ответ",
                response, beanDiffer(expectedResponse).fields(ignore("errors[0]/message", "fileData")));
        assertThat("получена ошибка авторизации",
                response.getErrors().get(0).getMessage().contains(AUTHORIZATION_ERROR_CODE), equalTo(true));
    }

    @Test
    public void checkRightAuthorization() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(FEED_URL)
                        .withLogin(LOGIN)
                        .withPass(PASS)
        );
        BmapiResponse expectedResponse = new BmapiResponse()
                .withEmptyErrors()
                .withEmptyWarnings();
        assertThat("yml2directinf вернула вернула ответ без ошибок и предупреждений",
                response, beanEquivalent(expectedResponse));
        assertThat("вернулось ненулевое количество товаров",
                response.getAllElementsAmount(), greaterThan(0));
    }
}
