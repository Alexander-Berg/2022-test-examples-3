package ru.yandex.autotests.innerpochta.tests.messageslist;


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

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

@Aqua.Test
@Title("Тест на создание кнопки для автоответа")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonAutoReplyTest extends BaseTest {

    private static final String NEW_AUTO_REPLY = "Новый шаблон…";

    private String subject;
    private String templateSbj;
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
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        subject = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        templateSbj = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().shouldSeeMessageWithSubject(templateSbj);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().activeAutoReplyBtn());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoReplyButtonIcon())
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons());
    }

    @Test
    @Title("Создание и использование кнопки «Автоответ»")
    @TestCaseId("1492")
    public void testCreateAutoReplyButton() {
        createAutoReplyButton();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoReplyButton());
        user.messagesSteps().shouldSeeMessageWithSubject("Re: " + subject);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject("Re: " + subject);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessageView().messageTextBlock().text(), text);
    }

    @Test
    @Title("Удаление кнопки «Автоответ»")
    @TestCaseId("3944")
    public void shouldDeleteAutoReplyButton() {
        createAutoReplyButton();
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().activeAutoReplyBtn());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoReplyButtonIcon());
    }

    @Step("Создаем кнопку «Автоответ»")
    private void createAutoReplyButton() {
        user.defaultSteps().clicksOn(onCustomButtons().overview().autoReply())
            .shouldSee(onCustomButtons().autoReplyButtonConfigure().autoReplySelect())
            .selectsOption(onCustomButtons().autoReplyButtonConfigure().autoReplySelect(), NEW_AUTO_REPLY)
            .inputsTextInElement(
                onCustomButtons().autoReplyButtonConfigure().autoReplyTextInput(),
                text
            )
            .clicksOn(onCustomButtons().autoReplyButtonConfigure().saveButton())
            .shouldNotSee(onCustomButtons().autoReplyButtonConfigure())
            .clicksOn(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
    }
}
