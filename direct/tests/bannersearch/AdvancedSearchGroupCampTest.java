package ru.yandex.autotests.directintapi.tests.bannersearch;

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

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 */
@Aqua.Test(title = "advanced_search - поиск с группировкой по кампании(group_camp).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchGroupCampTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    @Rule
    public Trashman trashman = new Trashman(api);
    private Long cid;
    private Long bid;

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
    }

    @Test
    @Title("Поиск баннеров с группировкой по кампании")
    @Description("Создаем кампанию с двумя группами суммарно с тремя баннерами" +
            "Ищем с группировкой по кампании. Проверяем, что в ответе ровно один(первый) баннер")
    public void bannerSearchByGroupCampTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                .withValues(Arrays.asList(cid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withGroupCamp(1)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bid), jooqSteps
        );
        assertThat("в ответе только первый баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }
}
