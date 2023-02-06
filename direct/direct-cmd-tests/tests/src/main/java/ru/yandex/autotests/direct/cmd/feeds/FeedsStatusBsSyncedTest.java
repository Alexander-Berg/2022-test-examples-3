package ru.yandex.autotests.direct.cmd.feeds;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * TESTIRT-8297
 */
@Aqua.Test
@Description("Сброс statusBsSynced для баннеров групп с данным фидом")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class FeedsStatusBsSyncedTest {
    private static final String CLIENT = "at-direct-bssynced-feed-1";
    private static final String NEW_FEED_URL = "https://4tochki.ru/external_upload/yandex/5037.xml";
    private static final String LOGIN_FOR_FEED = "loginForFeed";
    private static final String NEW_LOGIN_FOR_FEED = "newLoginForFeed";
    private static final String PASSWORD_FOR_FEED = "passwordForFeed";
    private static final String NEW_PASSWORD_FOR_FEED = "newPasswordForFeed";
    private static final String DELETE_LINKS_CHECK_BOX_ON = "1";
    private static final String DELETE_LINKS_CHECK_BOX_OFF = "0";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private FeedSaveRequest feedSaveRequest;
    private Long bannerId;

    @Before
    public void before() {

        Long feedId = bannersRule.getFeedId();
        bannerId = bannersRule.getBannerId();

        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class)
                .withFeedId(feedId.toString())
                .withLogin(LOGIN_FOR_FEED)
                .withPassword(PASSWORD_FOR_FEED)
                .withRemoveUtm(DELETE_LINKS_CHECK_BOX_OFF)
                .withUlogin(CLIENT);
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);

        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        BannersRecord firstBanner = TestEnvironment.newDbSteps().bannersSteps().getBanner(bannerId);
        assumeThat("баннер синхронизирован с БК", firstBanner.getStatusbssynced().toString(),
                equalTo(StatusBsSynced.YES.toString()));
    }

    @Test
    @Description("Изменение ссылки на фид")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9756")
    public void changeFeedsUrl() {
        feedSaveRequest.withUrl(NEW_FEED_URL);
        saveFeedAndCheckBannersStatusBsSynced();
    }

    @Test
    @Description("Изменение логина доступа к фиду")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9757")
    public void changeFeedsLogin() {
        feedSaveRequest.withLogin(NEW_LOGIN_FOR_FEED);
        saveFeedAndCheckBannersStatusBsSynced();
    }

    @Test
    @Description("Изменение пароля доступа к фиду")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9758")
    public void changeFeedsPassword() {
        feedSaveRequest.withPassword(NEW_PASSWORD_FOR_FEED);
        saveFeedAndCheckBannersStatusBsSynced();
    }

    @Test
    @Description("Включение галки удаления меток у ссылок фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9759")
    public void setDeleteLinksCheckBoxOn() {
        feedSaveRequest.withRemoveUtm(DELETE_LINKS_CHECK_BOX_ON);
        saveFeedAndCheckBannersStatusBsSynced();
    }

    @Test
    @Description("Выключение галки удаления меток у ссылок фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9760")
    public void setDeleteLinksCheckBoxOff() {
        feedSaveRequest.withRemoveUtm(DELETE_LINKS_CHECK_BOX_ON);
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        BannersRecord firstBanner = TestEnvironment.newDbSteps().bannersSteps().getBanner(bannerId);
        assumeThat("баннер синхронизирован с БК", firstBanner.getStatusbssynced().toString(),
                equalTo(StatusBsSynced.YES.toString()));

        feedSaveRequest.withRemoveUtm(DELETE_LINKS_CHECK_BOX_OFF);
        saveFeedAndCheckBannersStatusBsSynced();
    }

    private void saveFeedAndCheckBannersStatusBsSynced() {
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        BannersRecord firstBanner = TestEnvironment.newDbSteps().bannersSteps().getBanner(bannerId);
        assertThat("баннер НЕ синхронизирован с БК", firstBanner.getStatusbssynced().toString(),
                equalTo(StatusBsSynced.NO.toString()));
    }
}
