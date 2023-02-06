package ru.yandex.autotests.directintapi.tests.bmapi.yml2filtered;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static org.hamcrest.Matchers.greaterThan;

/**
 * Created by pavryabov on 22.09.15.
 * https://st.yandex-team.ru/TESTIRT-6810
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Поддерживаемые архивы в ручке yml2filtered")
@Issue("https://st.yandex-team.ru/IRT-263")
@RunWith(Parameterized.class)
public class BmapiFilteredArchivesTest {

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
                {"http://static.ozone.ru/multimedia/zip/marketnew2/yandex_div_music.zip"},
                {"https://www.dropbox.com/s/0hsxuh9xmei0o8v/valid_yml_with_cat.xml.zip?dl=1"},
                {"https://www.dropbox.com/s/z54z7du6f982e1v/valid_yml_with_cat.xml.gz?dl=1"}
        };
        return Arrays.asList(data);
    }

    @Test
    public void checkBmapiResponse() {
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("available", true);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                greaterThan(0)
        );
    }
}
