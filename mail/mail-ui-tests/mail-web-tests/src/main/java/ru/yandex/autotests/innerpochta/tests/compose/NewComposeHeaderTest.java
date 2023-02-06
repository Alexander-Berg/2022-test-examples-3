package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.GreetingMessageData.RU_MAIL_EN_LANG_SERVICES;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_FOR_SCROLLTOP;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Шапка")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewComposeHeaderTest extends BaseTest {
    private static final String SCRIPT_FOR_SCROLLDOWN = "$('.composeReact__scrollable-content').scrollTop(5000)";
    private static final String LONG_BODY_TEXT = RU_MAIL_EN_LANG_SERVICES;
    private static final String MANY_RECIPIENTS =
        "testbot1@yandex.ru, testbot2@yandex.ru, testbot3@yandex.ru, testbot4@yandex.ru, testbot5@yandex.ru";
    private static final String TO_EMAIL = "testbot2@yandex.ru";
    private static final String CC_EMAIL = "testbot6@yandex.ru";
    private static final String CC_EMAIL_2 = "testbot8@yandex.ru";
    private static final String CC_EMAIL_3 = "testbot9@yandex.ru";
    private static final String BCC_EMAIL = "testbot7@yandex.ru";
    private static final String BCC_EMAIL_2 = "testbot10@yandex.ru";
    private static final String BCC_EMAIL_3 = "testbot11@yandex.ru";
    String msgSubject;
    String bodyText;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        msgSubject = getRandomString();
        bodyText = getRandomString();
        user.apiSettingsSteps()
            .callWithListAndParams(
                "Включаем просмотр письма в списке писем",
                of(
                    SETTINGS_OPEN_MSG_LIST, TRUE,
                    SETTINGS_HEAD_FULL_EDITION, TRUE
                )
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Скролл залипающей шапки - Клик в поле «Кому» в залипшей шапки")
    @TestCaseId("5699")
    public void shouldExpandHeaderByClickInTo() {
        addLongBodyAndRecipients();
        //мы заполняем всё поле кому и единственное свободное место - справа, поэтому туда и кликаем
        user.defaultSteps().offsetFromRightCornerClick(onComposePopup().expandedPopup().popupToInput(), 5, 5)
            .shouldSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            );
    }

    @Test
    @Title("Скролл залипающей шапки - Скролл вниз и вверх")
    @TestCaseId("5699")
    public void shouldExpandHeaderByScrollOnly() {
        addLongBodyAndRecipients();
        user.defaultSteps().executesJavaScript(SCRIPT_FOR_SCROLLTOP)
            .shouldSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            );
    }

    @Test
    @Title("Скролл залипающей шапки - Клик в кнопку «Развернуть»")
    @TestCaseId("5699")
    public void shouldExpandHeaderByExpandBtn() {
        addLongBodyAndRecipients();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .shouldSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            );
    }

    @Test
    @Title("Скролл залипающей шапки - Клик в тему залипшей шапки")
    @TestCaseId("5699")
    public void shouldNotExpandHeaderAfterClickInSbj() {
        addLongBodyAndRecipients();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sbj())
            .shouldNotSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            );
    }

    @Test
    @Title("Скролл залипающей шапки - Клик в яббл в залипшей шапке")
    @TestCaseId("5699")
    public void shouldNotExpandHeaderAfterClickInYabble() {
        addLongBodyAndRecipients();
        user.defaultSteps().clicksOn(onComposePopup().yabbleTo())
            .shouldSee(onComposePopup().yabbleDropdown())
            .shouldNotSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            );
    }

    @Test
    @Title("Проверяем сохранение введенного значения в поле To после отправки письма")
    @TestCaseId("5528")
    public void shouldFillToCorrect() {
        user.composeSteps().openAndFillComposePopup(TO_EMAIL, msgSubject, bodyText);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(
            onMessageView().messageHead().contactsInTo().get(0),
            TO_EMAIL
        );
    }

    @Test
    @Title("Проверяем сохранение введенного значения в поле Cc после отправки письма")
    @TestCaseId("5528")
    public void shouldFillCcCorrect() {
        user.composeSteps().openAndFillComposePopup(TO_EMAIL, msgSubject, bodyText);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), DEV_NULL_EMAIL);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(
                onMessageView().messageHead().contactsInCC().get(0),
                DEV_NULL_EMAIL
            );
    }

    @Test
    @Title("Проверяем сохранение введенного значения в поле Bcc после отправки письма")
    @TestCaseId("5528")
    public void shouldFillBccCorrect() {
        user.composeSteps().openAndFillComposePopup(TO_EMAIL, msgSubject, bodyText);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), DEV_NULL_EMAIL);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(
                onMessageView().messageHead().contactsInBCC().get(0),
                DEV_NULL_EMAIL
            );
    }

    @Test
    @Title("Проверяем сохранение введенных значений в поля To, Cc и Bcc после отправки письма")
    @TestCaseId("5528")
    public void shouldFillToCcBccCorrect() {
        user.composeSteps().openAndFillComposePopup(TO_EMAIL, msgSubject, bodyText);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), DEV_NULL_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), DEV_NULL_EMAIL_2);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(
                onMessageView().messageHead().contactsInCC().get(0),
                DEV_NULL_EMAIL
            )
            .shouldSeeThatElementHasText(
                onMessageView().messageHead().contactsInBCC().get(0),
                DEV_NULL_EMAIL_2
            )
            .shouldSeeThatElementHasText(onMessageView().messageHead().contactsInTo().get(0), TO_EMAIL);
    }

    @Test
    @Title("Проверяем отображение темы в шапке попапа")
    @TestCaseId("5698")
    public void shouldSeeTitleInHeader() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), getRandomString());
        user.hotkeySteps()
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().bodyInput(), key(Keys.CONTROL), "s");
        user.defaultSteps().shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTitle(), "Без темы")
            .shouldSee(onComposePopup().expandedPopup().savedAt())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject);
        user.hotkeySteps()
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().bodyInput(), key(Keys.CONTROL), "s");
        user.defaultSteps().shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTitle(), msgSubject)
            .shouldSee(onComposePopup().expandedPopup().savedAt());
    }

    @Test
    @Title("Разворачиваем шапку при днд яббла")
    @TestCaseId("5787")
    public void shouldSeeSeeAllHeaderFields() {
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().popupCc())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().bodyInput())
            .dragAndDrop(
                onComposePopup().yabbleTo(),
                onComposePopup().expandedPopup().bodyInput()
            )
            .shouldSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc()
            );
    }

    @Test
    @Title("Разворачиваем шапку попапа и проверяем отображение полей Cc, Bcc и From")
    @TestCaseId("5700")
    public void shouldSeeCcBccFromAfterExpand() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .shouldSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().expandedPopup().popupFrom()
            );
    }

    @Test
    @Title("Сворачиваем шапку попапа и проверяем скрытие полей Cc, Bcc и From")
    @TestCaseId("5700")
    public void shouldNotSeeCcBccFromAfterCollapse() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .shouldNotSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().expandedPopup().popupFrom()
            );
    }

    @Test
    @Title("Заполняем поля Cc, Bcc и проверяем сохранение данных после сворачивания шапки")
    @TestCaseId("5700")
    public void shouldSaveCcBccAfterCollapse() {
        String ccRecipients = CC_EMAIL + ", " + CC_EMAIL_2 + ", " + CC_EMAIL_3;
        String bccRecipients = BCC_EMAIL + ", " + BCC_EMAIL_2 + ", " + BCC_EMAIL_3;
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), ccRecipients)
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), bccRecipients)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksIfCanOn(onComposePopup().yabbleMore())
            .shouldContainText(onComposePopup().yabbleCcList().get(0), CC_EMAIL)
            .shouldContainText(onComposePopup().yabbleCcList().get(1), CC_EMAIL_2)
            .shouldContainText(onComposePopup().yabbleCcList().get(2), CC_EMAIL_3)
            .shouldContainText(onComposePopup().yabbleBccList().get(0), BCC_EMAIL)
            .shouldContainText(onComposePopup().yabbleBccList().get(1), BCC_EMAIL_2)
            .shouldContainText(onComposePopup().yabbleBccList().get(2), BCC_EMAIL_3);
    }

    @Step("Заполняем тело письма длинным текстом и добавляем получателей во все поля")
    private void addLongBodyAndRecipients() {
        user.defaultSteps().setsWindowSize(1200, 800)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), LONG_BODY_TEXT + LONG_BODY_TEXT)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), MANY_RECIPIENTS)
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), CC_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), BCC_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().bodyInput())
            .executesJavaScript(SCRIPT_FOR_SCROLLDOWN)
            .shouldNotSee(
                onComposePopup().expandedPopup().popupCc(),
                onComposePopup().expandedPopup().popupBcc(),
                onComposePopup().yabbleFrom()
            )
            .shouldSee(onComposePopup().expandedPopup().expandCollapseBtn());
    }
}
