package ru.yandex.autotests.directintapi.tests.bannersearch;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchRequestData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchResponseData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.Criteria;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.ads.AdAddItemMap;
import ru.yandex.autotests.directapi.model.api5.ads.TextAdAddMap;
import ru.yandex.autotests.directapi.model.api5.sitelinks.AddRequestMap;
import ru.yandex.autotests.directapi.model.api5.sitelinks.SitelinkMap;
import ru.yandex.autotests.directapi.model.api5.sitelinks.SitelinksSetAddItemMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 */
@Aqua.Test(title = "advanced_search - короткий ответ(short)")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchShortTest {
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
    private Long bannerID;

    @BeforeClass
    public static void beforeClass() {
        jooqSteps = api.userSteps.getDarkSideSteps().getDirectJooqDbSteps().useShardForLogin(LOGIN);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        jooqSteps.bannersCalloutsSteps().clearCalloutsForClient(Long.valueOf(User.get(LOGIN).getClientID()));
        Long calloutIds = api.userSteps.adExtensionsSteps().addCalloutWithText("aa");
        jooqSteps.bannersCalloutsSteps().setAdditionsItemCalloutsStatusModerated(calloutIds,
                AdditionsItemCalloutsStatusmoderate.Yes);

        List<Long> sitelinksSetIds = api.userSteps.sitelinksSteps().add(new AddRequestMap().withSitelinksSets(
                new SitelinksSetAddItemMap().withSitelinks(
                        new SitelinkMap().randomSitelinkWithDescription().withDescription("aa"))));
        assumeThat("добавлен набор сайтлинков", sitelinksSetIds, hasSize(1));

        bid = api.userSteps.adsSteps().addAd(new AdAddItemMap()
                .withAdGroupId(pid)
                .withTextAd(new TextAdAddMap()
                        .defaultTextAd()
                        .withText("asdasdasasdas")
                        .withTitle("Adfzvdafczvadf")
                        .withDisplayUrlPath("Adfadavewadf")
                        .withAdExtensionIds(calloutIds)
                        .withSitelinkSetId(sitelinksSetIds.get(0)))
        );
        bannerID = api.userSteps.bannersFakeSteps().setBannerRandomFakeBannerID(bid);

    }

    @Test
    @Title("Поиск баннера, короткий ответ")
    @Description("Создаем кампанию с одной группой, с одним баннером. " +
            "Ищем с запросом короткого ответа." +
            "Проверяем, что в ответе ровно один правильный баннер и только поля соответствующие короткому ответу")
    public void bannerSearchShortTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.NUM.getKey())
                .withValues(Arrays.asList(bid))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
                        .withShortResponse(1)
        );
        BannerSearchResponseData expected = new BannerSearchResponseData().withBanners(
                Arrays.asList(
                        new BannerSearchResponseData.Banner()
                                .withBannerID(bannerID)
                                .withBid(bid)
                                .withCid(cid)
                )
        );
        assertThat("в ответе есть созданный баннер",
                response,
                beanDiffer(expected)
        );
    }

}
