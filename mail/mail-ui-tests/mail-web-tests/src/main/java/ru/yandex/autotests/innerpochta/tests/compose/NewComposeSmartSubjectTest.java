package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SMART_SUBJECT;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Smart Subject")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeSmartSubjectTest extends BaseTest {

    private String msg_body = getRandomString();
    private String NO_SUBJECT = "(Без темы)";

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
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем попап Smart Subject",
            of(SMART_SUBJECT, TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Отправляем письмо с выбранным саджестом темы")
    @TestCaseId("5954")
    public void shouldSendLetterWithSubjectSuggest() {
        callSmartSubjectPopup();
        String suggest_subject = onComposePopup().smartSubjectPopup().suggestItem().get(0).getText();
        user.defaultSteps().clicksOn(
                onComposePopup().smartSubjectPopup().suggestItem().get(0),
                onComposePopup().smartSubjectPopup().sendBtn()
            )
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(suggest_subject);
    }

    @Test
    @Title("Возвращаемся к письму из попапа")
    @TestCaseId("5952")
    public void shouldBackToComposeFromSmartSubject() {
        callSmartSubjectPopup();
        user.defaultSteps().clicksOn(onComposePopup().smartSubjectPopup().backToComposeBtn())
            .shouldNotSee(onComposePopup().smartSubjectPopup())
            .shouldSee(onComposePopup().expandedPopup());
    }

    @Test
    @Title("Отправляем без темы")
    @TestCaseId("5953")
    public void shouldSendLetterWithoutSubject() {
        callSmartSubjectPopup();
        user.defaultSteps().clicksOn(onComposePopup().smartSubjectPopup().sendWithoutSubjectBtn());
        user.messagesSteps().shouldSeeMessageWithSubject(NO_SUBJECT);
    }

    @Test
    @Title("Отключаем показ попапа Smart Subject")
    @TestCaseId("5950")
    public void shouldTurnOffSmartSubject() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), msg_body)
            .clicksOn(onComposePopup().expandedPopup().composeMoreBtn())
            .clicksOn(onComposePopup().expandedPopup().composeMoreOptionsPopup().smartsubjectToggle())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldNotSee(onComposePopup().smartSubjectPopup());
    }

    @Test
    @Title("«Больше не предлагать» в попапе Smart Subject")
    @TestCaseId("6295")
    public void doNotOfferSmartSubject() {
        callSmartSubjectPopup();
        user.defaultSteps().clicksOn(onComposePopup().smartSubjectPopup().doNotOfferCheckbox())
            .clicksOn(onComposePopup().smartSubjectPopup().sendWithoutSubjectBtn());
        user.messagesSteps().shouldSeeMessageWithSubject(NO_SUBJECT);
        callSmartSubjectPopup();
        user.defaultSteps().shouldNotSee(onComposePopup().smartSubjectPopup());
        user.messagesSteps().shouldSeeMessageWithSubject(NO_SUBJECT);
    }


    @Step("Вызов попапа Smart Subject")
    private void callSmartSubjectPopup() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), msg_body)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
    }

}
