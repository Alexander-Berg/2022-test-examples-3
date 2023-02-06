package ru.yandex.autotests.direct.cmd.clients;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsResyncQueueRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * TESTIRT-8297
 */
@Aqua.Test
@Description("BsResyncQueuePriority для кампаний пользователя при изменении настройки - показ рейтинга магазина")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SAVE_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class BsResyncQueuePriorityAfterModifiMarketRateTest {
    private static final String CLIENT = "at-direct-bssynced-client-2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public String showMarketRatingFirst;
    @Parameterized.Parameter(1)
    public String expShowMarketRatingFirst;
    @Parameterized.Parameter(2)
    public String showMarketRatingChanged;
    @Parameterized.Parameter(3)
    public String expShowMarketRatingChahged;
    @Parameterized.Parameter(4)
    public Integer expPriority;
    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private SettingsModel userSettings;
    private Long campaignId;

    @Parameterized.Parameters(name = "Меняем настройки показа рейтинга магазина {1} на {3}" +
            " и ожидаем приоритет BsResyncQueue {4}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"", "0", "1", "1", 75},
                {"1", "1", "", "0", 75},
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        setUserSettings(showMarketRatingFirst, expShowMarketRatingFirst);

        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(campaignId);
        TestEnvironment.newDbSteps(CLIENT).bsExportQueueSteps().deleteBsExportQueue(campaignId);
    }

    @Test
    @Description("Изменение BsResyncQueuePriority для кампаний пользователя при изменении настройки - " +
            "\"Скрывать рейтинг магазина в объявлении\"")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9565")
    public void changeMarketRate() {
        setUserSettings(showMarketRatingChanged, expShowMarketRatingChahged);
        checkBsResyncQueuePriority();
    }

    @Test
    @Description("Кампании не отправляются в ленивую очередь синхронизации если настройка " +
            "\"Скрывать рейтинг магазина в объявлении\" осталась прежней")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9566")
    public void notChangeMarketRate() {
        setUserSettings(showMarketRatingFirst, expShowMarketRatingFirst);
        BsResyncQueueRecord bsResyncQueue = TestEnvironment.newDbSteps(CLIENT).bsResyncQueueSteps()
                .getBsResyncQueueRecord(campaignId);
        assertThat("кампании пользователя не отправились в ленивую очередь синхронизации",
                bsResyncQueue, nullValue());
    }

    private void setUserSettings(String showMarketRating, String expShowMarketRating) {
        userSettings.setShowMarketRating(showMarketRating);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
        SettingsModel actual = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assumeThat("настройка пользователя соответствует ожидаемому ",
                actual.getShowMarketRating(), equalTo(expShowMarketRating));
    }

    private void checkBsResyncQueuePriority() {
        BsResyncQueueRecord bsResyncQueue = TestEnvironment.newDbSteps(CLIENT).bsResyncQueueSteps()
                .getBsResyncQueueRecord(campaignId);
        assertThat("bs_resync_queue вернулся с ожидаемым приоритетом",
                bsResyncQueue.getPriority(),
                equalTo(expPriority));
    }
}
