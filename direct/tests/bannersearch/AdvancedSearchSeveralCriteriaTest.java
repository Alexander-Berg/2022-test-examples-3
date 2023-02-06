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
 */
@Aqua.Test(title = "advanced_search - поиск по нескольким критериям.")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchSeveralCriteriaTest {
    public static final String DOMAIN = "fafjkQfdkjfsvWNDvnks.com";
    private final static String LOGIN = Logins.LOGIN_MAIN;
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static DirectJooqDbSteps jooqSteps;
    private static Long cid;
    private static Long pid;
    private static Long bidFirst;
    private static Long bidSecond;
    private static Long imageId;
    @Rule
    public Trashman trashman = new Trashman(api);

    @BeforeClass
    public static void beforeClass() {
        String hash = api.userSteps.imagesStepsV5().addImagesForUserIfNotExists(LOGIN, 1).get(0);
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bidFirst = api.userSteps.adsSteps().addAd(new AdAddItemMap()
                .withAdGroupId(pid)
                .withTextAd(new TextAdAddMap()
                        .defaultTextAd()
                        .withAdImageHash(hash)
                        .withHref("https://".concat(DOMAIN))
                )
        );
        bidSecond = api.userSteps.adsSteps().addAd(new AdAddItemMap()
                .withAdGroupId(pid)
                .withTextAd(new TextAdAddMap()
                        .defaultTextAd()
                        .withAdImageHash(hash)
                        .withHref("https://".concat(DOMAIN))
                )
        );

        imageId = jooqSteps.imagesSteps().getBannerImagesRecordsByHash(hash)
                .stream()
                .filter(bannerImagesRecord -> bannerImagesRecord.getBid().equals(bidFirst))
                .findFirst()
                .get()
                .getImageId();
    }

    @Test
    @Title("Поиск баннера по многим полям, уникальный ключ - bid")
    @Description("Создаем кампанию с одной группой, с двумя почти одинаковыми баннерами, один из них с картинкой. " +
            "Ищем по cid, pid, login, domain и bid. Проверяем, что в ответе ровно один правильный баннер")
    public void bannerSearchBannerDistinctBidTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                        .withKey(Criteria.CriteriaKey.NUM.getKey())
                        .withValues(Arrays.asList(bidFirst)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.GROUP.getKey())
                        .withValues(Arrays.asList(pid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                        .withValues(Arrays.asList(cid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.LOGIN.getKey())
                        .withValues(Arrays.asList(LOGIN)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.DOMAIN.getKey())
                        .withValues(Arrays.asList(DOMAIN))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidFirst), jooqSteps
        );
        assertThat("в ответе только один созданный баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннера по многим полям, уникальный ключ - imageId")
    @Description("Создаем кампанию с одной группой, с двумя почти одинаковыми баннерами, один из них с картинкой. " +
            "Ищем по cid, pid, login, domain и imageId. Проверяем, что в ответе ровно один правильный баннер")
    public void bannerSearchBannerDistinctImageIdTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                        .withKey(Criteria.CriteriaKey.GROUP.getKey())
                        .withValues(Arrays.asList(pid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                        .withValues(Arrays.asList(cid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.LOGIN.getKey())
                        .withValues(Arrays.asList(LOGIN)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.DOMAIN.getKey())
                        .withValues(Arrays.asList(DOMAIN)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.IMAGE_ID.getKey())
                        .withValues(Arrays.asList(imageId))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidFirst), jooqSteps
        );
        assertThat("в ответе только один созданный баннер",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Title("Поиск баннеров по многим полям")
    @Description("Создаем кампанию с одной группой, с двумя почти одинаковыми баннерами, один из них с картинкой. " +
            "Ищем по cid, pid, login, domain. Проверяем, что в ответе ровно два правильных баннера")
    public void bannerSearchTwoBannersTest() {
        List<Criteria> criteria = Arrays.asList(
                new Criteria()
                        .withKey(Criteria.CriteriaKey.GROUP.getKey())
                        .withValues(Arrays.asList(pid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.CAMPAIGN.getKey())
                        .withValues(Arrays.asList(cid)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.LOGIN.getKey())
                        .withValues(Arrays.asList(LOGIN)),
                new Criteria()
                        .withKey(Criteria.CriteriaKey.DOMAIN.getKey())
                        .withValues(Arrays.asList(DOMAIN))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        BannerSearchResponseData expected = BannerSearchSteps.getExpectedBannerSearchResponse(
                Arrays.asList(bidFirst, bidSecond), jooqSteps
        );
        assertThat("в ответе оба созданных баннера",
                response,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }
}
