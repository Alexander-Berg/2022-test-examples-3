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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


@Aqua.Test
@Title("Тесты на разные способы сохранения шаблонов")
@Features(FeaturesConst.TEMPLATES)
@Tag(FeaturesConst.TEMPLATES)
@Stories(FeaturesConst.SAVE_TEMPLATE)
public class TemplatesSaveTest extends BaseTest {

    private final static String YANDEX_RU = "@yandex.ru";

    private String mailTo = Utils.getRandomString() + YANDEX_RU;
    private String subject = Utils.getRandomString();
    private String text = Utils.getRandomString();

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
    @Title("Сохранение шаблона через выпадающее меню")
    @TestCaseId("1873")
    public void shouldSaveTemplateFromDropdown() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsMailContents(mailTo, subject, text);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .shouldSee(onComposePopup().expandedPopup().templatePopup())
            .clicksOn(
                onComposePopup().expandedPopup().templatePopup().saveBtn(),
                onComposePopup().expandedPopup().closeBtn()
            )
            .opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Пересохранение существующего шаблона")
    @TestCaseId("1871")
    public void saveExistingTemplates() {
        user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.composeSteps().clearInputsSubjectField()
            .clearInputsSendTextField()
            .inputsSendTextWithFormatting(text)
            .inputsSubject(subject);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(
                onComposePopup().expandedPopup().templatePopup().updateBtn(),
                onComposePopup().expandedPopup().closeBtn()
            );
        user.messagesSteps().clicksOnTemplateWithSubject(subject);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().shouldSeeSubject(subject)
            .shouldSeeTextAreaContains(text);
    }

}
