package ru.yandex.autotests.directintapi.tests.bmapi.yml2directinf;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.BmapiRequest;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.BmapiResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by pavryabov on 12.08.15.
 * https://st.yandex-team.ru/TESTIRT-6542
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Поддерживаемые архивы")
@Issue("https://st.yandex-team.ru/DIRECT-43907")
@RunWith(Parameterized.class)
public class BmapiArchivesTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Parameterized.Parameter(value = 0)
    public String url;

    @Parameterized.Parameters(name = "Url = {0}")
    public static Collection feeds() {
        Object[][] data = new Object[][]{
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/yandex_div_music.zip"},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/valid_yml_with_cat.xml.zip"},
                {"http://qa-storage.yandex-team.ru/get/indefinitely/bmapitest/feeds/valid_yml_with_cat.xml.gz"},
        };
        return Arrays.asList(data);
    }

    @Test
    public void checkBmapiResponse() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(url)
                        .withBusinessType("retail")
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
