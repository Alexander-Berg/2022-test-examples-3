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
@Title("Тест на создание кнопки «Архивировать»")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonArchiveTest extends BaseTest {

    private String subject;
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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().archiveActive());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().archiveButton())
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons());
    }

    @Test
    @Title("Создание и использование кнопки «Архивировать»")
    @TestCaseId("5828")
    public void testCreateArchiveButton() {
        createArchiveButton();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().archiveButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.ARCHIVE);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Удаление кнопки «Архивировать»")
    @TestCaseId("3946")
    public void shouldDeleteArchiveButton() {
        createArchiveButton();
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().archiveActive());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().archiveButton());
    }

    @Step("Создаем кнопку «Архивировать»")
    private void createArchiveButton() {
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(
                onCustomButtons().overview().archive(),
                onCustomButtons().overview().saveChangesButton()
            );
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
    }
}
