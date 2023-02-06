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

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

/**
 * Created by pavryabov on 10.08.15.
 * https://st.yandex-team.ru/TESTIRT-6542
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Неправильные урлы")
@Issue("https://st.yandex-team.ru/DIRECT-43907")
@RunWith(Parameterized.class)
public class BmapiUrlsNegativeTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String url;

    @Parameterized.Parameter(value = 2)
    public BmapiResponse expectedResponse;

    @Parameterized.Parameters(name = "{0}")
    public static Collection feeds() {
        Object[][] data = new Object[][]{
                {"invalid url", "xxx",
                        new BmapiResponse()
                                .withErrors(new Error().withCode(Error.DOWNLOAD_ERROR))
                                .withEmptyWarnings()
                                .withCategs(new ItemOfCategs[]{})},
                {"url with 404", "yandex.ru/404",
                        new BmapiResponse()
                                .withErrors(new Error().withCode(Error.DOWNLOAD_ERROR))
                                .withEmptyWarnings()},
                {"invalid xml", "https://www.dropbox.com/s/mcarugkwkqkzjin/xml_with_error.xml?dl=1",
                        new BmapiResponse()
                                .withErrors(new Error().withCode(Error.XML_FATAL))//ToDo генерить самому
                                .withEmptyWarnings()},
                {"not xml", "ya.ru",
                        new BmapiResponse()
                                .withErrors(new Error().withCode(Error.FEED_TYPE_MISMATCH))//ToDo генерить самому
                                .withEmptyWarnings()}
        };
        return Arrays.asList(data);
    }

    @Test
    public void checkBmapiResponse() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(url)
        );
        assertThat("yml2directinf вернула правильный ответ",
                response, beanDiffer(expectedResponse).fields(ignore("errors[0]/message", "categs", "fileData")));
    }
}
