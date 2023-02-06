package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.LANGS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author kurau
 */
@Aqua.Test
@Title("Добавление подписи")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderAddAndDeleteSignatureTest extends BaseTest {

    private String signatureText = getRandomName();
    private String tmpSignature = getRandomName();

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.apiSettingsSteps().changeSignsWithTextAndAmount(sign(tmpSignature));
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.settingsSteps().shouldSeeSignatureWith(tmpSignature);
    }

    @Test
    @Title("Добавляем подпись из настроек. Проверяем, что появилась.")
    @TestCaseId("1838")
    public void addSignatureFromSettings() {
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0));
        user.settingsSteps()
            .clearsSignatureText(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0))
            .editSignatureValue(
                signatureText,
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0)
            );
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().addBtn());
        user.settingsSteps().shouldSeeSignaturesCountInJsResponse(2);
        user.defaultSteps().shouldSeeElementsCount(
            onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            2
        );
        user.settingsSteps().shouldSeeSignatureWith("-- \n" + signatureText);
    }

    @Test
    @Title("Добавляем подпись из композа.")
    @TestCaseId("1839")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68214")
    public void addSignatureFromCompose() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .onMouseHover(onComposePage().textareaBlock().signatureFirstLine())
            .onMouseHoverAndClick(onComposePage().textareaBlock().signatureFirstLine())
            .shouldSee(onComposePage().signatureChooser())
            .onMouseHoverAndClick(onComposePage().signatureChooser())
            .clicksOn(onComposePage().signaturesDropdownMenu().addSignatureButton())
            .inputsTextInElement(onComposePage().addSignaturePopup().inputSignatureText(), signatureText)
            .clicksOn(onComposePage().addSignaturePopup().saveButton())
            .shouldNotSee(onComposePage().addSignaturePopup())
            .onMouseHoverAndClick(onComposePage().textareaBlock().signatureFirstLine())
            .shouldSee(onComposePage().signatureChooser())
            .onMouseHoverAndClick(onComposePage().signatureChooser());
        user.composeSteps().shouldSeeSignatureInList(signatureText);
    }

    @Test
    @Title("Создаём дупликат подписи из композа.")
    @TestCaseId("1840")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68214")
    public void addDuplicateSignature() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .onMouseHover(onComposePage().textareaBlock().signatureFirstLine())
            .onMouseHoverAndClick(onComposePage().textareaBlock().signatureFirstLine())
            .shouldSee(onComposePage().signatureChooser())
            .onMouseHoverAndClick(onComposePage().signatureChooser())
            .clicksOn(onComposePage().signaturesDropdownMenu().addSignatureButton())
            .clicksOn(onComposePage().addSignaturePopup().saveButton())
            .shouldNotSee(onComposePage().addSignaturePopup())
            .opensFragment(QuickFragments.SETTINGS_SENDER);
        user.settingsSteps().shouldNotSeeDuplicateSignature();
    }

    @Test
    @Title("Удаляем подпись из настроек")
    @TestCaseId("1841")
    public void deleteSignatureFromSettings() {
        user.defaultSteps()
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).delete()
            )
            .shouldSee(onSenderInfoSettingsPage().deleteSignaturePopup())
            .clicksOn(onSenderInfoSettingsPage().deleteSignaturePopup().cancelLink())
            .shouldSeeElementsCount(onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(), 1)
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).delete()
            )
            .clicksOn(onSenderInfoSettingsPage().deleteSignaturePopup().deleteButton())
            .shouldNotSee(onSenderInfoSettingsPage().deleteSignaturePopup())
            .shouldSeeElementsCount(onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(), 0);
    }

    @Test
    @Title("Добавленная подпись отображается в пришедшем письме")
    @TestCaseId("941")
    public void shouldSeeSignInMessage() {
        String subject = getRandomName();
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0));
        user.settingsSteps().editSignatureValue(
            signatureText,
            onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0)
        );
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().aliasesCheckBox())
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().addBtn())
            .waitInSeconds(1);
        user.settingsSteps().shouldSeeSignatureWith("-- \n" + signatureText);
        user.defaultSteps()
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).edit()
            )
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().editLang())
            .clicksOnElementWithText(onSenderInfoSettingsPage().languagesDropdown().langList(), LANGS[0])
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().saveBtn());
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subject)
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps()
            .shouldSeeThatElementHasText(user.pages().MessageViewPage().messageTextBlock(), signatureText);
    }
}
