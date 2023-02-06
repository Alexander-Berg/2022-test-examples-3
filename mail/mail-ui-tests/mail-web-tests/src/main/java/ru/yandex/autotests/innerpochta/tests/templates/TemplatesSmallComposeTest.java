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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.COMPOSE_SMALL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAST_USED_COMPOSE_SIZE;

/**
 * Created by eremin-n-s
 */
@Aqua.Test
@Title("Маленький композ - Шаблоны")
@Features(FeaturesConst.TEMPLATES)
@Tag(FeaturesConst.TEMPLATES)
@Stories(FeaturesConst.CREATE_TEMPLATE)
public class TemplatesSmallComposeTest extends BaseTest {

    private String subject = Utils.getRandomName();
    private String msgText = Utils.getRandomName();
    private String subjectTemplate = Utils.getRandomName();
    private String msgTextTemplate = Utils.getRandomName();

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
            "Включаем маленький композ",
            of(LAST_USED_COMPOSE_SIZE, COMPOSE_SMALL)
        );
        user.apiMessagesSteps().createTemplateMessage(lock.firstAcc(), subjectTemplate, msgTextTemplate);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание шаблона из композа")
    @TestCaseId("6020")
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
    @TestCaseId("6019")
    public void shouldCreateTemplateFromDraftsFolder() {
        user.defaultSteps().opensFragment(QuickFragments.DRAFT)
            .clicksOn(onMessagePage().toolbar().createTemplateButton())
            .shouldSee(onComposePopup().expandedPopup());
        createTemplate();
        checkTemplate(subject);
    }

    @Test
    @Title("Создание шаблона из папки «Шаблоны»")
    @TestCaseId("6018")
    public void shouldCreateTemplateFromTemplatesFolder() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE)
            .clicksOn(onMessagePage().toolbar().createTemplateButton());
        createTemplate();
        checkTemplate(subject);
    }

    @Test
    @Title("Открываем шаблон в пустом маленьком композе")
    @TestCaseId("6021")
    public void shouldOpenTemplatesFromEmptySmallCompose() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .shouldSee(onComposePopup().expandedPopup().templatePopup())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), msgTextTemplate)
            .shouldHasValue(onComposePopup().expandedPopup().sbjInput(), subjectTemplate)
            .shouldContainText(onComposePopup().yabbleTo().yabbleText(), lock.firstAcc().getSelfEmail());
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
