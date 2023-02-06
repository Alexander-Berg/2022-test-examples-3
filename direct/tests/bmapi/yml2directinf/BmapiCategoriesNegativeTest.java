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
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
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
 * Created by pavryabov on 11.08.15.
 * https://st.yandex-team.ru/TESTIRT-6542
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Фид с неправильными категориями")
@Issue("https://st.yandex-team.ru/DIRECT-43907")
@RunWith(Parameterized.class)
public class BmapiCategoriesNegativeTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String DEFAULT_NAME = "feed";

    public static final String CATEGORY_ID1 = "123";
    public static final String PARENT_ID1 = "456";
    public static final String CATEGORY_NAME1 = "category name 1";
    public static final String VENDOR_NAME1 = "vendor name 1";
    public static final String OFFER_NAME = "offer name";

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public Categories categories;

    @Parameterized.Parameter(value = 2)
    public Integer code;

    @Parameterized.Parameters(name = "{0}")
    public static Collection feeds() {
        Object[][] data = new Object[][]{
                {"empty array of categories", new Categories(), Error.NO_CATEGORIES},
                {"without categories", null, Error.NO_CATEGORIES},
                {"category without id",
                        new Categories().withCategories(new Category(null, PARENT_ID1, CATEGORY_NAME1)),
                        Error.NO_CATEGORIES
                },
        };
        return Arrays.asList(data);
    }

    @Test
    public void checkBmapiResponse() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(categories)
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                        .withName(OFFER_NAME)
                                        .withVendor(VENDOR_NAME1)
                                        .withCategoryId(CATEGORY_ID1)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(url)
        );
        BmapiResponse expectedResponse = new BmapiResponse()
                .withErrors(new Error()
                        .withCode(code))
                .withEmptyWarnings()
                .withCategs(new ItemOfCategs[]{});
        assertThat("yml2directinf вернула правильный ответ",
                response, beanDiffer(expectedResponse).fields(ignore("errors[0]/message", "categs", "fileData")));
    }
}
