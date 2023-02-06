package ru.yandex.autotests.directintapi.tests.bmapi.yml2filtered;

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
@Description("Проверка фильтрации по CategoryId и фильтрации фида без категорий")
@Issue("https://st.yandex-team.ru/IRT-263")
public class BmapiFilteredCategoriesTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    DarkSideSteps darkSideSteps = new DarkSideSteps();

    public static final String DEFAULT_NAME = "feed";

    public static final String CATEGORY_ID1 = "123";
    public static final String CATEGORY_ID2 = "321";
    public static final String PARENT_ID1 = "456";
    public static final String PARENT_ID2 = "654";
    public static final String CATEGORY_NAME1 = "category name 1";
    public static final String CATEGORY_NAME2 = "category name 2";
    public static final String VENDOR_NAME1 = "vendor name 1";
    public static final String OFFER_NAME = "offer name";

    @Test
    public void feedWithoutCategories() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                        .withCategoryId(CATEGORY_ID1)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                1
        );
    }

    @Test
    public void feedWithCategoryAnOfferWithOtherCategory() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category(CATEGORY_ID1, PARENT_ID1, CATEGORY_NAME1)))
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                        .withCategoryId(CATEGORY_ID2)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                0
        );
    }

    @Test
    public void twoOffersWithTheSameCategory() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category(CATEGORY_ID1, PARENT_ID1, CATEGORY_NAME1)))
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                                .withCategoryId(CATEGORY_ID1),
                                        new Offer()
                                                .withCategoryId(CATEGORY_ID1)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                2
        );
    }

    @Test
    public void twoOffersWithDiferentCategoriesAndOneInFilter() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category(CATEGORY_ID1, PARENT_ID1, CATEGORY_NAME1)))
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                                .withCategoryId(CATEGORY_ID1),
                                        new Offer()
                                                .withCategoryId(CATEGORY_ID2)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        Filters filters = new Filters().add("name", filter);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                1
        );
    }

    @Test
    public void twoOffersWithDiferentCategoriesAndBothInFilters() {
        YmlCatalog feed = new YmlCatalog()
                .withShop(new Shop()
                        .withCategories(new Categories()
                                .withCategories(new Category(CATEGORY_ID1, PARENT_ID1, CATEGORY_NAME1)))
                        .withOffers(new Offers()
                                .withOffers(new Offer()
                                                .withCategoryId(CATEGORY_ID1),
                                        new Offer()
                                                .withCategoryId(CATEGORY_ID2)
                                )
                        )
                );
        String xml = darkSideSteps.getBmapiSteps().ymlCatalogToXmlString(feed);
        String url = darkSideSteps.getBmapiSteps().saveFeedToElliptics(xml, DEFAULT_NAME);
        HashMap<String, Object> filter = new HashMap<String, Object>();
        filter.put("categoryId", CATEGORY_ID1);
        HashMap<String, Object> filterElse = new HashMap<String, Object>();
        filterElse.put("categoryId", CATEGORY_ID2);
        Filters filters = new Filters().add("name", filter).add("nameElse", filterElse);
        darkSideSteps.getBmapiSteps().checkCountInYml2Filtered(
                new BmapiRequest()
                        .yml2Filtered()
                        .withUrl(url)
                        .withFilters(filters),
                2
        );
    }
}
