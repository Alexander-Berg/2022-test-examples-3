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
import ru.yandex.autotests.directapi.model.api5.ads.AdAddItemMap;
import ru.yandex.autotests.directapi.model.api5.ads.TextAdAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.emptyIterable;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 */
@Aqua.Test(title = "advanced_search - поиск по домену(domain).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchByDomainTest {
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
    }

    @Test
    @Title("Поиск баннера по домену")
    @Description("Создаем кампанию, с одной группой, с одним баннером с уникальным доменом." +
            "Проверяем, что в ответе ровно один правильный баннер")
    public void bannerSearchByBannerDomainTest() {
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        String domain = RandomUtils.getString(26).concat(".ru");

        Long bid = api.userSteps.adsSteps().addAd(new AdAddItemMap()
                .withAdGroupId(pid)
                .withTextAd(new TextAdAddMap()
                        .defaultTextAd()
                        .withHref("https://".concat(domain)))
        );
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.DOMAIN.getKey())
                .withValues(Arrays.asList(domain))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bid), jooqSteps
        );
        assertThat("в ответе есть созданный баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннера по несуществующему домену")
    @Description("Делаем запрос с несуществующим доменом. Проверяем, что ответ пустой")
    public void bannerSearchByBannerUnexistingDomainTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.DOMAIN.getKey())
                .withValues(Arrays.asList(RandomUtils.getString(30).concat(".com")))
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
}
