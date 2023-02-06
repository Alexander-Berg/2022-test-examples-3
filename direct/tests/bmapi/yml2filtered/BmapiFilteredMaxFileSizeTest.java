package ru.yandex.autotests.directintapi.tests.bmapi.yml2filtered;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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

import java.util.HashMap;

/**
 * Created by pavryabov on 22.09.15.
 * https://st.yandex-team.ru/TESTIRT-6810
 */
@Aqua.Test
@Features(FeatureNames.BMAPI)
@Description("Проверка параметра max_file_size")
@Issue("https://st.yandex-team.ru/IRT-263")
public class BmapiFilteredMaxFileSizeTest {

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
    public static Filters filters;

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
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        filters = new Filters().add("name", filter);
    }

    @Test
    public void checkMaxSizeError() {
        //DIRECT-46633
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters)
                        .withMaxFileSize(String.valueOf(feedSize - 1)),
                0
        );
    }

    @Test
    public void checkRightMaxSize() {
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters)
                        .withMaxFileSize(String.valueOf(feedSize)),
                1
        );
    }
}
