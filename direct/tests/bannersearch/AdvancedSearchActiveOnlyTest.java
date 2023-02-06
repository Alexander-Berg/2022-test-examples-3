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
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.darkside.steps.BannerSearchSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 * $sql_activeonly = {'b.statusModerate' => 'Yes',
 * 'p.statusModerate' => 'Yes',
 * 'c.statusShow' => 'Yes',
 * 'b.statusShow' => 'Yes',
 * 'c.OrderID__gt' => 0,
 * 'b.BannerID__gt' => 0,
 * 'c.archived' => 'No',
 * '_AND' => {_TEXT => 'c.sum - c.sum_spent + IF(wc.cid, wc.sum - wc.sum_spent, 0) > 0'},
 * };
 */
@Aqua.Test(title = "advanced_search - поиск по только активных баннеров(active_only).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchActiveOnlyTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    private static Long pid;
    private static Long bidActive;
    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bidActive = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.bannersFakeSteps().setBannerRandomFakeBannerID(bidActive);
        api.userSteps.bannersFakeSteps().setStatusModerate(bidActive, Status.YES);
        api.userSteps.bannersFakeSteps().setStatusShow(bidActive, Status.YES);
        api.userSteps.groupFakeSteps().setStatusModerate(pid, Status.YES);
        api.userSteps.campaignFakeSteps().setCampaignSum(cid, 1000f);
        api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
        api.userSteps.campaignFakeSteps().setStatusShow(cid, Status.YES);
    }

    @Test
    @Title("Поиск только активных баннеров в группе")
    @Description("Создаем кампанию с одной группой, в которой два баннера, один активный, второй – нет." +
            "Проверяем, что в ответе ровно один правильный активный баннер")
    public void bannerSearchActiveOnlyTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.GROUP.getKey())
                .withValues(Arrays.asList(pid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withActiveonly(1)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidActive), jooqSteps
        );
        assertThat("в ответе только один баннер - активный",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }
}
