package ru.yandex.autotests.innerpochta.tests.advertisement;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.AD_DEBUG;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_FREE_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на смену рекламных блоков - ПДД")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
@RunWith(DataProviderRunner.class)
public class AdBlocksTestPddTus extends BaseTest {

    private static final String AD_MSG_LIST_LINE_ID_PDD = "R-I-272659-90";
    private static final String AD_MSG_LIST_LEFT_ID_PDD = "R-I-272659-92";
    private static final String AD_MSG_LIST_LEFT_ID_PDD_3PANE = "R-I-272659-93";
    private static final String AD_MSG_VIEW_LINE_ID_PDD = "R-I-272659-91";
    private static final String AD_MSG_VIEW_LEFT_ID_PDD = "R-I-272659-95";
    private static final String AD_COMPOSE_LEFT_ID_PDD = "R-I-272659-94";
    private static final String AD_DONE_LEFT_ID_PDD = "R-I-272659-96";

    private AccLockRule lock = AccLockRule.use().useTusAccount(PDD_FREE_USER_TAG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));


    @DataProvider
    public static Object[][] userInterfaceLayoutPdd() {
        return new Object[][]{
            {LAYOUT_2PANE, AD_MSG_LIST_LEFT_ID_PDD},
            {LAYOUT_3PANE_VERTICAL, AD_MSG_LIST_LEFT_ID_PDD_3PANE}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу и выключаем треды",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV,
                SETTINGS_FOLDER_THREAD_VIEW, FALSE
            )
        );
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 1);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensDefaultUrlWithPostFix(AD_DEBUG);
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке и над списком писем - пдд пользователь")
    @TestCaseId("4688")
    public void shouldSeeCorrectBlocksIdPdd() {
        shouldSeeCorrectLineAndLeftBlockId(
            AD_MSG_LIST_LINE_ID_PDD, AD_MSG_LIST_LEFT_ID_PDD,
            AD_MSG_VIEW_LINE_ID_PDD, AD_MSG_VIEW_LEFT_ID_PDD
        );
    }

    @Test
    @Title("Отображаем правильные ID блоков рекламы в левой колонке - ПДД пользователь - 3pane")
    @TestCaseId("4688")
    public void shouldSeeCorrectBlocksIdPdd3Pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем вертикальный 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().MessagePage().directLeft())
            .shouldSeeThatElementTextEquals(
                user.pages().MessagePage().directLeftInfo().blockId(), AD_MSG_LIST_LEFT_ID_PDD_3PANE
            );
    }

    @Test
    @Title("Должны менять ID блоков рекламы в ЛК при переходе между страницами - ПДД")
    @TestCaseId("5060")
    public void shouldSeeChangeBlocksIdPdd() {
        user.advertisementSteps()
            .checkChangeBlocksId(AD_MSG_LIST_LEFT_ID_PDD, AD_COMPOSE_LEFT_ID_PDD, AD_DONE_LEFT_ID_PDD, lock.firstAcc().getSelfEmail());
    }

    @Test
    @Title("Показываем рекламу всех видов в PDD")
    @TestCaseId("2090")
    public void shouldSeeAllTypesAdPDD() {
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0))
            .shouldSee(onHomePage().directAd());
        user.advertisementSteps().shouldSeeAllAd();
        user.composeSteps().goToDone(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldSee(user.pages().MessagePage().directDone());
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
