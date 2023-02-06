package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.db.beans.feeds.FeedType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppcfeedtobannerland.CategsBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppcfeedtobannerland.PpcFeedToBannerlandResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Categories;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Category;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Offer;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Offers;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.Shop;
import ru.yandex.autotests.directapi.darkside.model.bmapi.request.YmlCatalog;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Title("Проверка запрета на изменение типа фида скриптом ppcFeedToBannerLand.pl")
@Features(FeatureNames.PPC_FEED_TO_BANNER_LAND)
@Issue("https://st.yandex-team.ru/DIRECT-76588")
public class PpcFeedToBannerlandFeedTypeChangeNegativeTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String LOGIN = ru.yandex.autotests.directapi.darkside.Logins.PPC_FEED_TO_BANNER_LAND_4;
    private static final String OLD_ELLIPTICS_FEED_NAME =
            PpcFeedToBannerlandFeedTypeChangeNegativeTest.class.getSimpleName() + "Old";
    private static final String NEW_ELLIPTICS_FEED_NAME =
            PpcFeedToBannerlandFeedTypeChangeNegativeTest.class.getSimpleName() + "New";
    private static final FeedType OLD_FEED_TYPE = FeedType.YANDEX_CUSTOM;
    private static final FeedType NEW_FEED_TYPE = FeedType.YANDEX_MARKET;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.SUPER_LOGIN);
    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);
    @Rule
    public DirectCmdRule cmdRule;
    private PerformanceBannersRule bannersRule;

    private int shardId;
    private long feedId;
    private String oldUrl;
    private String newUrl;

    public PpcFeedToBannerlandFeedTypeChangeNegativeTest() {
        bannersRule = new PerformanceBannersRule()
                .withUlogin(LOGIN);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    @Step("Подготовка данных для теста")
    public void init() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        oldUrl = uploadNewFeed(OLD_ELLIPTICS_FEED_NAME, OLD_FEED_TYPE);

        feedId = bannersRule.getFeedId();
        api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .updateFeedUrl(feedId, clientId, oldUrl);
        api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .updateFeedType(feedId, clientId, null);
        api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .updateFeedStatus(feedId, FeedsUpdateStatus.New, clientId);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                null, FakeBsProxyConfig.getPpcFeedToBannerlandUrl());

        newUrl = uploadNewFeed(NEW_ELLIPTICS_FEED_NAME, NEW_FEED_TYPE);
        api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .updateFeedUrl(feedId, clientId, newUrl);
        api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps()
                .updateFeedStatus(feedId, FeedsUpdateStatus.New, clientId);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId},
                null, FakeBsProxyConfig.getPpcFeedToBannerlandUrl());
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord()
                .setFetchErrorsCount(1)
                .setUpdateStatus(FeedsUpdateStatus.Error)
                .setFeedType(OLD_FEED_TYPE.getTypedValue());
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("ожидалось, что тип фида не поменяется и будет статус ошибки", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }

    @After
    public void after() {
        HELPER.deleteFakeBSProxyLogBeansById(oldUrl);
        HELPER.deleteFakeBSProxyLogBeansById(newUrl);
    }

    private String uploadNewFeed(String ellipticsFeedName, FeedType feedType) {
        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps()
                .ymlCatalogToXmlString(new YmlCatalog().withShop(new Shop()
                        .withCategories(new Categories().withCategories(
                                new Category("12345", "54321", "CategoryName")))
                        .withOffers(new Offers().withOffers(new Offer()
                                .withUrl("http://url.ru")
                                .withName("OfferName")
                                .withVendor("VendorName")
                                .withCategoryId("12345")))));
        String url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ellipticsFeedName);

        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean().withObjectIds(
                Collections.singletonList(url)
        );

        Map<String, Short> domainMap = new HashMap<>();
        domainMap.put("url.ru", (short) 1);
        HELPER.saveFakeBSProxyLogBean(logBean.withResponseEntity(
                new PpcFeedToBannerlandResponseBean()
                        .withErrors(new ArrayList<>())
                        .withWarnings(new ArrayList<>())
                        .withFileData(xml)
                        .withAllElementsAmount(1)
                        .withFeedType(feedType.getTypedValue())
                        .withCategories(Collections.singletonList(new CategsBean()
                                .withCategory("CategoryName")
                                .withId(12345L)
                                .withParentId(54321L)
                        ))
                        .withDomain(domainMap)
                        .toString()
        ));
        addJsonAttachment("Ответ BL для фида " + url, logBean.getResponseEntity());

        return url;
    }

}
