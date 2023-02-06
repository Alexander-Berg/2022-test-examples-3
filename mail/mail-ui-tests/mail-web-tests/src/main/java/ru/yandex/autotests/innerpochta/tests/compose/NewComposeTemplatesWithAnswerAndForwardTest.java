package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Step;

import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Применение шаблонов в ответах и форвардах")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeTemplatesWithAnswerAndForwardTest extends BaseTest {

    private String template_body = getRandomString();
    private String template_subject = getRandomString();
    private String message_body = getRandomString();
    private String message_subject = getRandomString();
    private static final String OTHER_EMAIL = "testbot2@yandex.ru";

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
        user.apiMessagesSteps().createTemplateMessage(OTHER_EMAIL, template_subject, template_body);
        user.apiMessagesSteps().sendMail(lock.firstAcc(), message_subject, message_body);
        user.apiSettingsSteps().callWithListAndParams(
                "Выключаем треды",
                of(
                        SETTINGS_FOLDER_THREAD_VIEW, FALSE
                )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Применяем шаблон в ответ на письмо")
    @TestCaseId("5922")
    public void shouldApplyTemplateToAnswer() {
        user.messagesSteps().clicksOnMessageWithSubject(message_subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().replyButton())
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0));
        checkComposeFields("Re: " + message_subject);
    }

    @Test
    @Title("Применяем шаблон в пересылке письма")
    @TestCaseId("5851")
    public void shouldApplyTemplateToForward() {
        user.messagesSteps().clicksOnMessageWithSubject(message_subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton())
                .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
                .shouldContainText(onComposePopup().yabbleToList().get(0).yabbleText(), lock.firstAcc().getSelfEmail());
        checkComposeFields("Fwd: " + message_subject);
    }

    @Step("Проверяем поля после применения шаблона")
    private void checkComposeFields(String subject) {
        user.defaultSteps().shouldHasValue(onComposePopup().expandedPopup().sbjInput(), subject)
                .shouldContainText(onComposePopup().expandedPopup().bodyInput(), template_body)
                .shouldContainText(onComposePopup().yabbleToList().get(1).yabbleText(), OTHER_EMAIL);
    }

}
