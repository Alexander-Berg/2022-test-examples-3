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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Изменение шаблонов")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeTemplateEditTest extends BaseTest {

    private String template_body = getRandomString();
    private String template_subject = getRandomString();
    private String template_subject_2 = getRandomString();
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
        user.apiMessagesSteps().createTemplateMessage(lock.firstAcc(), template_subject, template_body);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Добавляем в шаблон новых получателей")
    @TestCaseId("5920")
    public void shouldAddNewRecipientsToTemplate() {
        openTemplate();
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().popupTo(), OTHER_EMAIL)
                .clicksOn(onComposePopup().expandedPopup().bodyInput());
        updateTemplate();
    }

    @Test
    @Title("Изменяем тему и обновляем шаблон")
    @TestCaseId("5925")
    public void shouldEditSubjectAndUpdateTemplate() {
        openTemplate();
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().sbjInput(), getRandomString());
        updateTemplate();
    }

    @Test
    @Title("Изменяем тело письма и обновляем шаблон")
    @TestCaseId("5926")
    public void shouldEditSBodyAndUpdateTemplate() {
        openTemplate();
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), getRandomString());
        updateTemplate();
    }

    @Test
    @Title("Добавляем аттач и обновляем шаблон")
    @TestCaseId("5929")
    public void shouldAddAttachAndUpdateTemplate() {
        openTemplate();
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), IMAGE_ATTACHMENT);
        updateTemplate();
    }

    @Test
    @Title("Изменяем тему и создаем новый шаблон")
    @TestCaseId("5921")
    public void shouldChangeSubjectAndCreateNewTemplate() {
        openTemplate();
        user.defaultSteps().inputsTextInElementClearingThroughHotKeys(
                onComposePopup().expandedPopup().sbjInput(),
                template_subject_2
        )
                .shouldSee(onComposePopup().expandedPopup().templatesNotif())
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().saveAsNewTemplateBtn())
                .shouldSee(onHomePage().notification())
                .opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().shouldSeeMessageWithSubject(template_subject)
                .shouldSeeMessageWithSubject(template_subject_2);
    }

    @Step("Открываем шаблон")
    private void openTemplate() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0));
    }

    @Step("Обновляем шаблон")
    private void updateTemplate() {
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().templatesNotif())
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().updateBtn())
                .shouldSee(onHomePage().notification());
    }

}
