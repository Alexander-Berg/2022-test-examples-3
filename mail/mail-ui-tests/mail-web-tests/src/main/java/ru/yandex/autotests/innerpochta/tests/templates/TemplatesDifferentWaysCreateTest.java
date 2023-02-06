package ru.yandex.autotests.innerpochta.tests.templates;

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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * Created by kurau on 06.01.14.
 */
@Aqua.Test
@Title("Тесты на разные способы создания шаблонов")
@Features(FeaturesConst.TEMPLATES)
@Tag(FeaturesConst.TEMPLATES)
@Stories(FeaturesConst.CREATE_TEMPLATE)
public class TemplatesDifferentWaysCreateTest extends BaseTest {

    private String subject = Utils.getRandomName();
    private String msgText = Utils.getRandomName();

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
        user.apiFoldersSteps().createTemplateFolder();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание шаблона со страницы написания письма")
    @TestCaseId("5915")
    public void shouldCreateTemplateFromCompose() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subject)
            .inputsSendText(msgText);
        user.defaultSteps().clicksOn(
            onComposePopup().expandedPopup().templatesBtn(),
            onComposePopup().expandedPopup().templatePopup().saveBtn()
        );
        checkTemplate(subject);
    }

    @Test
    @Title("Создание шаблона из папки «Черновики»")
    @TestCaseId("5914")
    public void shouldCreateTemplateFromDraftsFolder() {
        user.defaultSteps().opensFragment(QuickFragments.DRAFT)
            .clicksOn(onMessagePage().toolbar().createTemplateButton());
        shouldSeeCompose();
        createTemplate();
        checkTemplate(subject);
    }

    @Test
    @Title("Создание шаблона из папки «Шаблоны» кнопкой на тулбаре")
    @TestCaseId("5913")
    public void shouldCreateTemplateFromTemplatesFolderToolbarBtn() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE)
            .clicksOn(onMessagePage().toolbar().createTemplateButton());
        shouldSeeCompose();
        createTemplate();
        checkTemplate(subject);
    }

    @Test
    @Title("Создание шаблона из папки «Шаблоны» кнопкой под тулбаром “Создать шаблон“")
    @TestCaseId("3291")
    public void testCreateTemplateFromTemplatesFolder() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE)
            .clicksOn(onMessagePage().notificationBlock().createTemplateBtn());
        shouldSeeCompose();
        createTemplate();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().closeBtn())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.TEMPLATE);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.composeSteps().shouldSeeSendToAreaHas(lock.firstAcc().getSelfEmail())
            .shouldSeeTextAreaContains(msgText)
            .shouldSeeSubject(subject);
    }

    @Test
    @Title("Проверяем отправку письма из шаблона")
    @TestCaseId("3292")
    public void shouldSendMsgFromTemplate() {
        String templateSbj = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.composeSteps().clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.messagesSteps().shouldSeeMessageWithSubject(templateSbj);
        checkTemplate(templateSbj);
    }

    @Test
    @Title("Создание шаблона при заполненном композе")
    @TestCaseId("4577")
    public void testCreateTemplateFromFilledCompose() {
        String templateSbj = getRandomString();
        String templateText = getRandomString();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsSubject(templateSbj)
            .inputsSendText(templateText);
        user.defaultSteps()
            .clicksOn(
                onComposePopup().expandedPopup().templatesBtn(),
                onComposePopup().expandedPopup().templatePopup().saveBtn()
            );
        shouldSeeCompose();
        user.composeSteps().shouldSeeSubject(templateSbj)
            .shouldSeeTextAreaContains(templateText);
    }

    @Step("Должны перейти в композ")
    private void shouldSeeCompose() {
        user.defaultSteps().shouldSee(
            onComposePopup().expandedPopup(),
            onComposePopup().expandedPopup().templatesBtn(),
            onComposePopup().expandedPopup().sendBtn()
        );
    }

    @Step("Создание шаблона")
    private void createTemplate() {
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subject)
            .inputsSendText(msgText);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().templatesNotif())
            .clicksOn(
                onComposePopup().expandedPopup().templatesBtn(),
                onComposePopup().expandedPopup().templatePopup().saveBtn()
            );
    }

    @Step("Проверка созданного шаблона")
    private void checkTemplate(String sbj) {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().shouldSeeMessageWithSubject(sbj);
    }
}
