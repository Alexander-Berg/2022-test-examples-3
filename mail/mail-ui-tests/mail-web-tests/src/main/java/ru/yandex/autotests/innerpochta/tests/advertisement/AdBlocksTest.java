package ru.yandex.autotests.innerpochta.tests.advertisement;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.AD_CRYPROX;
import static ru.yandex.autotests.innerpochta.util.MailConst.AD_DEBUG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на смену рекламных блоков")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
public class AdBlocksTest extends BaseTest {

    private static final String AD_MSG_LIST_LINE_ID = "R-I-92550-159";
    private static final String AD_MSG_LIST_LEFT_ID = "R-I-92550-164";
    private static final String AD_MSG_LIST_LEFT_ID_3PANE = "R-I-92550-167";
    private static final String AD_MSG_VIEW_LINE_ID = "R-I-92550-160";
    private static final String AD_MSG_VIEW_LEFT_ID = "R-I-92550-195";
    private static final String AD_COMPOSE_LEFT_ID = "R-I-92550-168";
    private static final String AD_DONE_LEFT_ID = "R-I-92550-217";
    private static final String AD_MSG_LIST_LEFT_ID_AB = "R-I-92550-164";
    private static final String AD_COMPOSE_LEFT_ID_AB = "R-I-92550-168";
    private static final String AD_MSG_VIEW_LEFT_ID_AB = "R-I-92550-195";
    private static final String AD_MSG_VIEW_LINE_ID_AB = "R-I-92550-160";

    private static final String AD_LEFT_REFRESH = "1 раз(а)";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV
            )
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensDefaultUrlWithPostFix(AD_DEBUG);
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке и над списком писем - обычный пользователь")
    @TestCaseId("4688")
    public void shouldSeeCorrectBlocksId() {
        user.advertisementSteps().shouldSeeAdLineAndLeft()
            .checkLineAndLeftBlockId(AD_MSG_LIST_LINE_ID, AD_MSG_LIST_LEFT_ID);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.advertisementSteps().shouldSeeAdLineAndLeft()
            .checkLineAndLeftBlockId(AD_MSG_VIEW_LINE_ID, AD_MSG_VIEW_LEFT_ID);
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке - обычный пользователь - 3pane")
    @TestCaseId("4688")
    public void shouldSeeCorrectBlocksId3Pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем вертикальный 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().MessagePage().directLeft())
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfo().blockId(),
                AD_MSG_LIST_LEFT_ID_3PANE
            );
    }

    @Test
    @Title("При автосохранении черновика реклама не перерисовывается")
    @TestCaseId("5136")
    public void shouldNotRefreshAdInCompose() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsSubject(Utils.getRandomString());
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().savedAt())
            .waitInSeconds(2)
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfo().blockRefreshCount(),
                AD_LEFT_REFRESH
            );
    }

    @Test
    @Title("При обновлении почты реклама не перерисовывается")
    @TestCaseId("5137")
    public void shouldNotRefreshAdInMsgList() {
        user.defaultSteps().clicksOn(onHomePage().checkMailButton())
            .shouldNotSee(onHomePage().mailLoader())
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfo().blockRefreshCount(),
                AD_LEFT_REFRESH
            );
    }

    @Test
    @Title("Должны менять ID блоков рекламы в ЛК при переходе между страницами - обычный пользователь")
    @TestCaseId("5060")
    public void shouldSeeChangeBlocksId() {
        user.advertisementSteps().checkChangeBlocksId(AD_MSG_LIST_LEFT_ID, AD_COMPOSE_LEFT_ID, AD_DONE_LEFT_ID, lock.firstAcc().getSelfEmail());
    }

    @Test
    @Title("Меняем ID блоков при переходе между страницами под блокировщиками")
    @TestCaseId("5151")
    public void shouldChangeAdBlocksWithCryprox() {
        user.defaultSteps()
            .opensDefaultUrlWithPostFix(AD_CRYPROX)
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfoCryprox().blockId(),
                AD_MSG_LIST_LEFT_ID_AB
            ).opensFragment(QuickFragments.COMPOSE)
            .shouldSeeThatElementHasTextWithCustomWait(
                user.pages().MessagePage().directLeftInfoCryprox().blockId(),
                AD_COMPOSE_LEFT_ID_AB,
                70
            ).opensDefaultUrlWithPostFix(AD_CRYPROX)
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfoCryprox().blockId(),
                AD_MSG_LIST_LEFT_ID_AB
            );
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSeeThatElementHasTextWithCustomWait(
            user.pages().MessagePage().directLeftInfoCryprox().blockId(),
            AD_MSG_VIEW_LEFT_ID_AB,
            70
        ).shouldSeeThatElementTextEquals(
            user.pages().MessagePage().directLineInfoCryprox().blockId(),
            AD_MSG_VIEW_LINE_ID_AB
        );
    }
}
