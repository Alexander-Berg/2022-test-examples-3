package ru.yandex.autotests.directintapi.tests.bmapi.yml2directinf;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.BmapiRequest;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.*;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.*;
import ru.yandex.autotests.directapi.darkside.model.bmapi.response.Error;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

/**
 * Created by pavryabov on 11.08.15.
 * https://st.yandex-team.ru/TESTIRT-6542
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Проверка параметра max_file_size")
@Issue("https://st.yandex-team.ru/DIRECT-43907")
public class BmapiMaxFileSizeTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String DEFAULT_NAME = "feed";

    public static final String CATEGORY_ID1 = "123";
    public static final String CATEGORY_NAME1 = "category name 1";
    public static final String VENDOR_NAME1 = "vendor name 1";
    public static final String OFFER_NAME = "offer name";

    public static YmlCatalog feed;
    public static String xml;
    public static String url;
    public static int feedSize;

    @BeforeClass
    public static void initTest() {
        feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category(CATEGORY_ID1, null, CATEGORY_NAME1)))
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                        .withName(OFFER_NAME)
                                        .withVendor(VENDOR_NAME1)
                                        .withCategoryId(CATEGORY_ID1)
                                )
                        )
                );
        xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        feedSize = xml.length();
        url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
    }

    @Test
    public void checkMaxSizeError() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .withMaxFileSizeType("bytes")
                        .yml2DirectInf()
                        .withUrl(url)
                        .withMaxFileSize(String.valueOf(feedSize - 1))
        );
        BmapiResponse expectedResponse = new BmapiResponse()
                .withErrors(new Error().withCode(Error.TOO_BIG_FILE))
                .withEmptyWarnings()
                .withCategs(new ItemOfCategs[]{});
        assertThat("yml2directinf вернула правильный ответ",
                response, beanDiffer(expectedResponse).fields(ignore("errors[0]/message", "fileData")));
    }

    @Test
    public void checkRightMaxSize() {
        BmapiResponse response = darkSideSteps.getBmapiSteps().getBmapiResponse(
                new BmapiRequest()
                        .yml2DirectInf()
                        .withUrl(url)
                        .withMaxFileSize(String.valueOf(feedSize))
        );
        BmapiResponse expectedResponse = new BmapiResponse()
                .withCategoryId(new ItemOfCategoryId(CATEGORY_ID1, 1))
                .withVendor(new ItemOfVendor(VENDOR_NAME1, 1))
                .withEmptyErrors()
                .withEmptyWarnings()
                .withCategs(new ItemOfCategs(null, CATEGORY_ID1, CATEGORY_NAME1))
                .withAllElementsAmount(1);
        assertThat("yml2directinf вернула правильный ответ", response, beanDiffer(expectedResponse).fields(ignore("fileData")));
    }
}
