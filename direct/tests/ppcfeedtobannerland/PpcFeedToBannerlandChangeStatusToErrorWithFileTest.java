package ru.yandex.autotests.directintapi.tests.ppcfeedtobannerland;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsSource;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.ErrorItemBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppcfeedtobannerland.PpcFeedToBannerlandResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.model.FeedsErrorsEnum;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.autotests.directapi.darkside.Logins.PPC_FEED_TO_BANNER_LAND_4;
import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 18/08/15.
 * https://st.yandex-team.ru/TESTIRT-9276
 */
@Aqua.Test
@Title("Проверка обработки лимита ошибок для фидов по урлу скриптом ppcFeedToBannerLand.pl")
@Features(FeatureNames.PPC_FEED_TO_BANNER_LAND)
@Issue("https://st.yandex-team.ru/DIRECT-53641")
@RunWith(Parameterized.class)
public class PpcFeedToBannerlandChangeStatusToErrorWithFileTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String LOGIN = PPC_FEED_TO_BANNER_LAND_4;
    private static final String FEED_URL = "http://some-url.domain.ru/files/some-error1.xml";
    private static final Integer MAX_ERRORS_CHECK_COUNT = 1;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.SUPER_LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    @Parameterized.Parameter()
    public FeedsErrorsEnum responseError;
    private int shardId;
    private long feedId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Stream.of(FeedsErrorsEnum.values())
                .map(feedsErrorsEnum -> new Object[]{feedsErrorsEnum})
                .collect(Collectors.toList());
    }

    @Before
    @Step("Подготовка данных для теста")
    public void init() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        String clientId = api.userSteps.clientFakeSteps().getClientData(LOGIN).getClientID();
        FakeBSProxyLogBean logBean = new FakeBSProxyLogBean().withObjectIds(
                Collections.singletonList(FEED_URL)
        );
        HELPER.saveFakeBSProxyLogBean(logBean.withResponseEntity(
                new PpcFeedToBannerlandResponseBean()
                        .withErrors(Arrays.asList(
                                new ErrorItemBean()
                                        .withMessage(responseError.getMessage())
                                        .withCode(responseError.getCode()))
                        ).withWarnings(new ArrayList<>())
                        .withFileData("some response")
                        .toString()
        ));
        addJsonAttachment("Ответ BL для фида " + FEED_URL, logBean.getResponseEntity());

        FeedsRecord feeds = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getDefaultFeed(clientId)
                .setCachedFileHash(RandomUtils.getString(22))
                .setRefreshInterval(0L)
                .setSource(FeedsSource.file)
                .setFetchErrorsCount(MAX_ERRORS_CHECK_COUNT)
                .setFilename("filename")
                .setUpdateStatus(FeedsUpdateStatus.New)
                .setUrl(FEED_URL);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().createFeed(feeds);
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{feedId}
                , null, FakeBsProxyConfig.getPpcFeedToBannerlandUrl());
    }

    @Test
    public void checkErrorsCountInFeedTable() {
        FeedsRecord actualFeed = api.userSteps.getDirectJooqDbSteps().useShard(shardId).feedsSteps().getFeed(feedId);
        FeedsRecord expectedFeeds = new FeedsRecord()
                .setFetchErrorsCount(MAX_ERRORS_CHECK_COUNT + 1)
                .setUpdateStatus(FeedsUpdateStatus.Error);
        assertThat("запсись в таблице feeds соответствует ожиданиям", actualFeed.intoMap(),
                beanDiffer(expectedFeeds.intoMap()).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @After
    public void after() {
        HELPER.deleteFakeBSProxyLogBeansById(FEED_URL);
    }

}
