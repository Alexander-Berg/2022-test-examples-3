package ru.yandex.autotests.innerpochta.tests.advertisement;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.MailConst.AD_DEBUG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DEFAULT_SIZE_LAYOUT_LEFT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DONE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на смену рекламных блоков - ПДД")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
public class AdBlocksTestPdd extends BaseTest {

    private static final String TUTBY_ACCOUNT = "AdBlocksTestTutby";
    private static final String REGRU_ACCOUNT = "AdBlockTestRegru";

    private static final String AD_MSG_LIST_LINE_ID_TUTBY = "R-A-353372-32";
    private static final String AD_MSG_LIST_LEFT_ID_TUTBY = "R-A-353372-34";
    private static final String AD_MSG_VIEW_LINE_ID_TUTBY = "R-A-353372-33";
    private static final String AD_MSG_VIEW_LEFT_ID_TUTBY = "R-A-353372-37";
    private static final String AD_MSG_LIST_LINE_ID_REGRU = "R-A-354324-32";
    private static final String AD_MSG_LIST_LEFT_ID_REGRU = "R-A-354324-34";
    private static final String AD_MSG_VIEW_LINE_ID_REGRU = "R-A-354324-33";
    private static final String AD_MSG_VIEW_LEFT_ID_REGRU = "R-A-354324-37";

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу, 2pane, переход на страницу промо на Done и выключаем открытие письма в списке писем",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_DONE,
                SETTINGS_OPEN_MSG_LIST, EMPTY_STR,
                SETTINGS_FOLDER_THREAD_VIEW, FALSE
            )
        );
        user.apiSettingsSteps().callWithListAndParams(
            "Разворачиваем ЛК",
            of(
                SIZE_LAYOUT_LEFT, DEFAULT_SIZE_LAYOUT_LEFT
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensDefaultUrlWithPostFix(AD_DEBUG);
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке и над списком писем - tut.by")
    @TestCaseId("4931")
    @UseCreds(TUTBY_ACCOUNT)
    public void shouldSeeCorrectBlocksIdTutby() {
        shouldSeeCorrectLineAndLeftBlockId(
            AD_MSG_LIST_LINE_ID_TUTBY, AD_MSG_LIST_LEFT_ID_TUTBY,
            AD_MSG_VIEW_LINE_ID_TUTBY, AD_MSG_VIEW_LEFT_ID_TUTBY
        );
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке и над списком писем - reg.ru")
    @TestCaseId("4931")
    @UseCreds(REGRU_ACCOUNT)
    public void shouldSeeCorrectBlocksIdRegru() {
        shouldSeeCorrectLineAndLeftBlockId(
            AD_MSG_LIST_LINE_ID_REGRU, AD_MSG_LIST_LEFT_ID_REGRU,
            AD_MSG_VIEW_LINE_ID_REGRU, AD_MSG_VIEW_LEFT_ID_REGRU
        );
    }

    @Step("Проверяем рекламные блоки в левой колонке и полоске над списком писем")
    private void shouldSeeCorrectLineAndLeftBlockId(
        String lineIdMsgList, String leftIdMsgList, String lineIdMsgView, String leftIdMsgView
    ) {
        user.advertisementSteps().shouldSeeAdLineAndLeft()
            .checkLineAndLeftBlockId(lineIdMsgList, leftIdMsgList);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.advertisementSteps().shouldSeeAdLineAndLeft()
            .checkLineAndLeftBlockId(lineIdMsgView, leftIdMsgView);
    }
}
