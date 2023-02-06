package ru.yandex.autotests.directintapi.tests.bmapi.yml2filtered;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
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

/**
 * Created by pavryabov on 23.09.15.
 * https://st.yandex-team.ru/TESTIRT-6810
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Проверка фильтрации по Id")
@Issue("https://st.yandex-team.ru/IRT-263")
@RunWith(Parameterized.class)
public class FilterByIdTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String DEFAULT_NAME = "feed";

    public static final String ID1 = "1";
    public static final String ID2 = "2";
    public static final String ID3 = "3";

    @Parameterized.Parameter(value = 0)
    public String operator;

    @Parameterized.Parameter(value = 1)
    public Object value;

    @Parameterized.Parameter(value = 2)
    public Integer expectedCount;

    @Parameterized.Parameters(name = "Filter = {0}:{1}")
    public static Collection date() {
        Object[][] data = new Object[][]{
                {"id >", ID3, 0},
                {"id >=", ID3, 1},
                {"id >", "0", 3},
                {"id <", ID1, 0},
                {"id <=", ID1, 1},
                {"id <", "4", 3},
                {"id", ID1, 1},
                {"id ==", ID1, 1},
                {"id", "0", 0},
                {"id ==", "0", 0},
                {"id", new String[]{"1", "2"}, 2},
                {"id", new String[]{"1", "4"}, 1},
                {"id", new String[]{"4", "5"}, 0},
        };
        return Arrays.asList(data);
    }

    private static String url;

    @BeforeClass
    public static void initTest() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withOffers(new Offers()
                                .withOffers(
                                        new Offer().withId(ID1),
                                        new Offer().withId(ID2),
                                        new Offer().withId(ID3)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
    }

    @Test
    public void feedWithoutCategories() {
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put(operator, value);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                expectedCount
        );
    }
}
