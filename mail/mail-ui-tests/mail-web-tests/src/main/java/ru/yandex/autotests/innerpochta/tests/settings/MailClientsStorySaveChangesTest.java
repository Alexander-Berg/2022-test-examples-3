package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients.ImapAdvantagesBlock;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.ENABLE_POP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DISABLE_AUTOEXPUNGE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ENABLE_IMAP;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Сохранение изменений на странице")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.MAIL_APP)
public class MailClientsStorySaveChangesTest extends BaseTest {

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
        user.apiSettingsSteps().callWith(of(
            SETTINGS_PARAM_ENABLE_IMAP, EMPTY_STR,
            ENABLE_POP, EMPTY_STR,
            SETTINGS_PARAM_DISABLE_AUTOEXPUNGE, EMPTY_STR
        ));
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_CLIENT);
    }

    @Test
    @Title("Проставляем все чекбоксы в настройках клиента")
    @TestCaseId("1780")
    public void testTurnCheckBoxesToTrueOnMailClientsPage() {
        user.defaultSteps().turnTrue(
                clients().blockSetupClients().imap().enableImapCheckbox(),
                clients().blockSetupClients().imap().portalPassword(),
                clients().blockSetupClients().imap().disableAutoexpungeCheckbox(),
                clients().blockSetupClients().pop3().enablePopCheckbox(),
                clients().blockSetupClients().markAsReadCheckBox())
            .turnTrue(clients().blockSetupClients().pop3().checkboxesOfFolders());
        user.settingsSteps().savesMailClientsSettings();
        user.defaultSteps().refreshPage()
            .shouldSeeThatEvery(
                clients().blockSetupClients().allCheckboxes(),
                true
            );
    }

    @Test
    @Title("Сниманием выделение с чекбоксов pop и imap")
    @TestCaseId("1781")
    public void testTurnCheckBoxesToFalseOnMailClientsPage() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_ENABLE_IMAP, STATUS_ON, ENABLE_POP, STATUS_ON));
        user.defaultSteps().refreshPage();
        user.defaultSteps().deselects(
            clients().blockSetupClients().imap().enableImapCheckbox(),
            clients().blockSetupClients().pop3().enablePopCheckbox()
        );
        user.settingsSteps().savesMailClientsSettings();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_CLIENT)
            .shouldSeeCheckBoxesInState(
                false,
                clients().blockSetupClients().imap().enableImapCheckbox(),
                clients().blockSetupClients().pop3().enablePopCheckbox()
            );
    }

    @Test
    @Title("Выделяем и снимаем выделение с чекбоксов папок в протоколе pop")
    @TestCaseId("1782")
    public void testPopSettingsOnMailClientsPage() {
        user.defaultSteps().turnTrue(clients().blockSetupClients().pop3().enablePopCheckbox())
            .shouldSeeElementInList(clients().blockSetupClients().pop3().folders(), "Входящие");
        user.defaultSteps().shouldSeeElementInList(clients().blockSetupClients().pop3().folders(), "Отправленные");
        user.defaultSteps().shouldSeeElementInList(clients().blockSetupClients().pop3().folders(), "Спам");
        user.defaultSteps().shouldSeeElementInList(clients().blockSetupClients().pop3().folders(), "Черновики");
        user.defaultSteps().turnTrue(clients().blockSetupClients().pop3().checkboxesOfFolders())
            .clicksOn(clients().blockSetupClients().pop3().enableAllCheckboxes())
            .shouldSeeThatEvery(clients().blockSetupClients().pop3().checkboxesOfFolders(), false)
            .clicksOn(clients().blockSetupClients().pop3().enableAllCheckboxes())
            .shouldSeeThatEvery(clients().blockSetupClients().pop3().checkboxesOfFolders(), true)
            .turnTrue(clients().blockSetupClients().markAsReadCheckBox())
            .clicksOn(clients().blockSetupClients().pop3().enableAllCheckboxes())
            .shouldSeeCheckBoxesInState(true, clients().blockSetupClients().markAsReadCheckBox());
    }

    @Test
    @Title("Тест на появление описания к протоколу imap")
    @TestCaseId("1783")
    public void testCheckInfoAboutImapAdvantages() {
        user.defaultSteps().refreshPage()
            .clicksOn(clients().blockSetupClients().imap().imapAdvantages().showAdvantagesLink())
            .shouldSee(
                clients().blockSetupClients().imap().imapAdvantages(),
                clients().blockSetupClients().imap().imapAdvantages().pictures().get(0),
                clients().blockSetupClients().imap().imapAdvantages().pictures().get(1)
            )
            .shouldSeeThatElementTextEquals(
                clients().blockSetupClients().imap().imapAdvantages().advantages().get(0),
                ImapAdvantagesBlock.FIRST_ADVANTAGE
            )
            .shouldSeeThatElementTextEquals(
                clients().blockSetupClients().imap().imapAdvantages().advantages().get(1),
                ImapAdvantagesBlock.SECOND_ADVANTAGE
            )
            .clicksOn(clients().blockSetupClients().imap().imapAdvantages().selectIMAPLink())
            .shouldSeeCheckBoxesInState(true, clients().blockSetupClients().allCheckboxes().get(0));
    }
}
