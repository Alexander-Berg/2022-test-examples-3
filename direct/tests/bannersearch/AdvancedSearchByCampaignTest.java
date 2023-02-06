package ru.yandex.autotests.directintapi.tests.bannersearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchRequestData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchResponseData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.Criteria;
import ru.yandex.autotests.directapi.darkside.steps.BannerSearchSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.emptyIterable;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 */
@Aqua.Test(title = "advanced_search - поиск по id кампании(campaign).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchByCampaignTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    @Rule
    public Trashman trashman = new Trashman(api);
    private List<Long> bids = new ArrayList<>();
    private List<Long> cids = new ArrayList<>();
    private Long pid;

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        cids.add(api.userSteps.campaignSteps().addDefaultTextCampaign());
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cids.get(0));
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pid));
    }

    @Test
    @Title("Поиск баннеров в одной кампании(один баннер)")
    @Description("Создаем кампанию с одной группой, с одним баннером. " +
            "Проверяем, что в ответе ровно один правильный баннер")
    public void bannerSearchByCampaignIdTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                .withValues(cids)
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                bids, jooqSteps
        );
        assertThat("в ответе есть созданный баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннеров в одной кампании(два баннера)")
    @Description("Создаем кампанию с одной группой, с двумя баннерами." +
            "Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchByCampaignIdTwoBannersInSameCampaignTest() {
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pid));

        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                .withValues(cids)
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                bids, jooqSteps
        );
        assertThat("в ответе есть два созданных баннера",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск по несуществующей кампании")
    @Description("Делаем запрос с номером кампании, который не существует. Проверяем, что ответ пустой")
    public void bannerSearchByCampaignUnexistingIdTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                .withValues(Arrays.asList(cids.get(0) + 100000))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        assertThat("ответ пустой",
                response.getBanners(),
                emptyIterable()
        );
    }

    @Test
    @Title("Поиск баннеров в двух кампаниях(по одному баннеру в каждой)")
    @Description("Создаем две кампании, каждая с одной группой, с одним баннером." +
            "Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchByCampaignIdTwoBannersInTwoCampaignsTest() {
        cids.add(api.userSteps.campaignSteps().addDefaultTextCampaign());
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cids.get(1));
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pid));

        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                .withValues(cids)
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                bids, jooqSteps
        );
        assertThat("В ответе есть созданный баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }
}
