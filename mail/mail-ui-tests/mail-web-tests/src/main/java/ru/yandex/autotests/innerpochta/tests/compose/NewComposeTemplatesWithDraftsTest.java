package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Применение шаблонов в черновиках")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeTemplatesWithDraftsTest extends BaseTest {

    private String template_body = getRandomString();
    private String template_subject = getRandomString();
    private String draft_body = getRandomString();
    private String draft_subject = getRandomString();
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
    @Title("Применяем шаблон в композе с не сохраненным черновиком")
    @TestCaseId("5918")
    public void shouldApplyTemplateToUnsavedDraft() {
        createDraft();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0));
        checkFieldsAfterTemplate();
    }

    @Test
    @Title("Применяем шаблон в композе с сохраненным черновиком")
    @TestCaseId("5919")
    public void shouldApplyTemplateToSavedDraft() {
        createDraft();
        user.hotkeySteps().pressHotKeysWithDestination(
                onComposePopup().expandedPopup().bodyInput(),
                Keys.chord(Keys.CONTROL, "s")
        );
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0));
        checkFieldsAfterTemplate();
    }

    @Step("Создаем черновик")
    private void createDraft(){
        user.defaultSteps().clicksOn(onHomePage().composeButton())
                .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), OTHER_EMAIL)
                .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), draft_subject)
                .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), draft_body);
    }

    @Step("Проверяем поля после применения шаблона")
    private void checkFieldsAfterTemplate (){
        user.defaultSteps().shouldHasValue(onComposePopup().expandedPopup().sbjInput(), draft_subject)
                .shouldContainText(onComposePopup().expandedPopup().bodyInput(), template_body)
                .shouldContainText(onComposePopup().yabbleToList().get(0).yabbleText(), OTHER_EMAIL)
                .shouldContainText(onComposePopup().yabbleToList().get(1).yabbleText(), lock.firstAcc().getSelfEmail());
    }

}
