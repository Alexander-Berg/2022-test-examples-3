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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Применение шаблонов с подписями")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeTemplatesWithSignaturesTest extends BaseTest {

    private String template_body = getRandomString();
    private String template_subject = getRandomString();
    private String messsage_subject = getRandomString();
    private String signature_1 = "-- \nAAA" + getRandomString();
    private String signature_2 = "-- \nZZZ" + getRandomString();
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
        user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(signature_1),
            sign(signature_2)
        );
        user.apiMessagesSteps().sendMail(lock.firstAcc().getSelfEmail(), messsage_subject, "");
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Применяем шаблон с подписью")
    @TestCaseId("5931")
    public void shouldApplyTemplateWithSign() {
        createTemplateWithSign();
        user.defaultSteps().clicksOn(onHomePage().composeButton());
        checkAndChangeSign();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
            .shouldContainText(onComposePopup().signatureBlock(), signature_2);
        checkFieldsAfterTemplate(template_subject);
    }

    @Test
    @Title("Применяем шаблон с подписью в ответе")
    @TestCaseId("4475")
    public void shouldApplyTemplateWithSignInAnswer() {
        createTemplateWithSign();
        user.messagesSteps().clicksOnMessageWithSubject(messsage_subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().replyButton());
        checkAndChangeSign();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
            .shouldContainText(onComposePopup().signatureBlock(), signature_1);
        checkFieldsAfterTemplate("Re: " + messsage_subject);
    }

    @Step("Создаем шаблон с подписью")
    private void createTemplateWithSign() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), OTHER_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), template_subject)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), template_body)
            .clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().saveBtn())
            .clicksOn(onComposePopup().expandedPopup().closeBtn());
    }

    @Step("Проверяем поля после применения шаблона")
    private void checkFieldsAfterTemplate(String subject) {
        user.defaultSteps().shouldHasValue(onComposePopup().expandedPopup().sbjInput(), subject)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), template_body);
    }

    @Step("Проверяем и меняем подпись")
    private void checkAndChangeSign() {
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().signatureBlock(), signature_2)
            .onMouseHover(onComposePopup().signatureBlock())
            .clicksOn(onComposePopup().signatureChooser())
            .clicksOnElementWithText(onComposePopup().signaturesPopup().signaturesList(), "AAA");
    }

}
