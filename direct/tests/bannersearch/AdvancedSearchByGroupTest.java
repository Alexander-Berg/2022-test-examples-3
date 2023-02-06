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
@Aqua.Test(title = "advanced_search - поиск по id группы(group).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchByGroupTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    @Rule
    public Trashman trashman = new Trashman(api);
    private List<Long> bids = new ArrayList<>();
    private List<Long> pids = new ArrayList<>();
    private Long cid;

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        pids.add(api.userSteps.adGroupsSteps().addDefaultGroup(cid));
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pids.get(0)));
    }

    @Test
    @Title("Поиск баннеров в одной группе(один баннер)")
    @Description("Создаем кампанию с одной группой с одним баннером." +
            "Проверяем, что в ответе ровно один правильный баннер")
    public void bannerSearchByGroupIdTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(pids)
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
    @Title("Поиск баннеров в одной группе(два баннера)")
    @Description("Создаем кампанию с одной группой с двумя баннерами." +
            "Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchByGroupIdTwoBannersInSameGroupTest() {
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pids.get(0)));

        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(pids)
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
    @Title("Поиск по несуществующей группе")
    @Description("Делаем запрос с номером группы, который не существует. Проверяем, что ответ пустой")
    public void bannerSearchByGroupUnexistingIdTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pids.get(0) + 100000))
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
    @Title("Поиск баннеров в двух группах одной кампании")
    @Description("Создаем кампанию с двумя группами, каждая с одним баннером." +
            "Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchByGroupIdTwoBannersInTwoGroupsInOneCampaignTest() {
        pids.add(api.userSteps.adGroupsSteps().addDefaultGroup(cid));
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pids.get(1)));

        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(pids)
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

    @Test
    @Title("Поис баннеров в двух группах разных кампаний")
    @Description("Создаем две кампании, каждая с одной группой, каждая с одним баннером." +
            "Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchByGroupIdTwoBannersInTwoGroupsInTwoCampaignsTest() {
        pids.add(api.userSteps.adGroupsSteps().addDefaultGroup(cid));
        bids.add(api.userSteps.adsSteps().addDefaultTextAd(pids.get(1)));

        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(pids)
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
