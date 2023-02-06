package ru.yandex.autotests.directintapi.tests.bannersearch;

import java.util.Arrays;
import java.util.List;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 * Пока проверяем старое поведение, исправить после https://st.yandex-team.ru/DIRECT-60630
 */
@Aqua.Test(title = "advanced_search - limit и offset.")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-57459"),
        @Issue("https://st.yandex-team.ru/DIRECT-60630")})
@Features(FeatureNames.NOT_REGRESSION_YET)
public class AdvancedSearchLimitTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;
    private final static String LOGIN_ANOTHER_SHARD = Logins.LOGIN_TRANSPORT_IMG_SMALL;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_SUPER);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    private static Long pid;
    private static Long bidFirst;
    private static Long bidSecond;
    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid, LOGIN);
        bidFirst = api.userSteps.adsSteps().addDefaultTextAd(pid, LOGIN);
        bidSecond = api.userSteps.adsSteps().addDefaultTextAd(pid, LOGIN);
    }

    @Test
    @Title("Поиск баннеров в одной группе с лимитом 1")
    public void bannerSearchLimit1Test() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withLimit(1)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidFirst), jooqSteps
        );
        assertThat("в ответе только один баннер - первый",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннеров в одной группе с лимитом 2")
    public void bannerSearchLimit2Test() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withLimit(2)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidFirst, bidSecond), jooqSteps
        );
        assertThat("в ответе только два баннера",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннеров в одной группе с лимитом 1 и оффсетом 1")
    public void bannerSearchLimitAndOffsetTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withLimit(2)
                        .withOffset(1)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidSecond), jooqSteps
        );
        assertThat("в ответе только один баннер - второй",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннеров в из разных шардов с лимитом 1")
    public void bannerSearchLimitInDifferentShardsTest() {

        assumeThat("клиенты " + LOGIN + " и " + LOGIN_ANOTHER_SHARD + " в разынх шардах",
                api.userSteps.clientFakeSteps().getUserShard(LOGIN),
                not(equalTo(api.userSteps.clientFakeSteps().getUserShard(LOGIN_ANOTHER_SHARD))));

        Long cidOtherShard = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN_ANOTHER_SHARD);
        Long pidOtherShard = api.userSteps.adGroupsSteps().addDefaultGroup(cidOtherShard, LOGIN_ANOTHER_SHARD);
        Long bidOtherShardFirst = api.userSteps.adsSteps().addDefaultTextAd(pidOtherShard, LOGIN_ANOTHER_SHARD);
        api.userSteps.adsSteps().addDefaultTextAd(pidOtherShard, LOGIN_ANOTHER_SHARD);
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pid, pidOtherShard))
        );

        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withLimit(3)
        );
        BannerSearchResponseData expected = new BannerSearchResponseData().withBanners(Arrays.asList(
                new BannerSearchResponseData.Banner().withBid(bidFirst),
                new BannerSearchResponseData.Banner().withBid(bidSecond),
                new BannerSearchResponseData.Banner().withBid(bidOtherShardFirst)
        ));

        assertThat("в ответе только 3 баннера, два от первого клиента, третий от второго",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }


}
