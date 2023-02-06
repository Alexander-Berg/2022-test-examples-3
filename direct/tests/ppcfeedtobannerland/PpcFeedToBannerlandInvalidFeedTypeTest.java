package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
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

/**
 * Created by buhter on 18/08/15.
 * https://st.yandex-team.ru/TESTIRT-9276
 */
@Aqua.Test
@Title("Проверка обработки различных ошибок скриптом ppcFeedToBannerLand.pl")
@Features(FeatureNames.PPC_FEED_TO_BANNER_LAND)
@Issue("https://st.yandex-team.ru/DIRECT-53641")
@RunWith(Parameterized.class)
public class PpcFeedToBannerlandInvalidFeedTypeTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String LOGIN = ru.yandex.autotests.directapi.darkside.Logins.PPC_FEED_TO_BANNER_LAND_4;
    private static final String ELLIPTICS_FEED_NAME = PpcFeedToBannerlandInvalidFeedTypeTest.class.getSimpleName();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.SUPER_LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    @Parameterized.Parameter()
    public String feedType;

    @Parameterized.Parameter(1)
    public int fetchErrorCount;

    @Parameterized.Parameter(2)
    public FeedsUpdateStatus expectedStatus;

    private int shardId;
    private long feedId;
    private String url;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, 1, FeedsUpdateStatus.Error},
                {"", 1, FeedsUpdateStatus.Error},
                {"UnknownFeedType", 1, FeedsUpdateStatus.Error},
                {FeedType.YANDEX_MARKET.getTypedValue(), 0, FeedsUpdateStatus.Done},
                {FeedType.ALIEXPRESS.getTypedValue(), 0, FeedsUpdateStatus.Done},
        });
    }

    @Before
    @Step("Подготовка данных для теста")
    public void init() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();

        String xml = api.userSteps.getDarkSideSteps().getBmapiSteps()
                .ymlCatalogToXmlString(new YmlCatalog().withShop(new Shop()
                        .withCategories(new Categories().withCategories(
                                new Category("12345", "54321", "CategoryName")))
                        .withOffers(new Offers().withOffers(new Offer()
                                .withUrl("http://url.ru")
                                .withName("OfferName")
                                .withVendor("VendorName")
                                .withCategoryId("12345")))));
        url = api.userSteps.getDarkSideSteps().getBmapiSteps().saveFeedToElliptics(xml, ELLIPTICS_FEED_NAME);

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
                        .withFeedType(feedType)
                        .withCategories(Arrays.asList(new CategsBean()
                                .withCategory("CategoryName")
                                .withId(12345L)
                                .withParentId(54321L)
                        ))
                        .withDomain(domainMap)
                        .toString()
        ));
        addJsonAttachment("Ответ BL для фида " + url, logBean.getResponseEntity());

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setRefreshInterval(1L)
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(url);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}
                , null, FakeBsProxyConfig.getPpcFeedToBannerlandUrl());
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord()
                .setFetchErrorsCount(fetchErrorCount)
                .setUpdateStatus(expectedStatus);
        CompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("запись в таблице feeds соответствует ожиданиям", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(strategy));
    }

    @After
    public void after() {
        HELPER.deleteFakeBSProxyLogBeansById(url);
    }

}
