package ru.yandex.autotests.innerpochta.tests.settings;

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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.LANGS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_RICHEDIT;

/**
 * @author kurau
 */
@Aqua.Test
@Title("Проверка разных стилей разметки")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderChangeFormattingSignatureTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().changeSignsAmountTo(1);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.defaultSteps().shouldSeeElementsCount(onSenderInfoSettingsPage().blockSetupSender()
            .signatures().signaturesList(), 1);
    }

    @Test
    @Title("Вводим подпись курсивом")
    @TestCaseId("1844")
    public void changeFormattingSignature() {
        String text = Utils.getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем форматирование письма",
            of(SETTINGS_ENABLE_RICHEDIT, TRUE)
        );
        user.defaultSteps()
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0));
        user.settingsSteps().inputsSignatureWithFormatting(text);
        user.defaultSteps()
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().saveBtn())
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onMessagePage().composeButton());
        user.composeSteps().shouldSeeSignatureWithFormatting(text);
    }

    @Test
    @Title("Меняем подпись и сохраняем её кликом НЕ на кнопку сохранить, а кликом в поле создания новой подписи.")
    @TestCaseId("1842")
    public void saveChangesByMissClick() {
        String beacon = Utils.getRandomName();
        user.defaultSteps()
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).edit()
            );
        user.settingsSteps()
            .clearsSignatureText(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(1))
            .editSignatureValue(
                beacon,
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(1)
            );
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0));
        user.settingsSteps()
            .editSignatureValue(
                beacon,
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0)
            )
            .shouldSeeSignaturesCountOnPage(1);
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).textSignature(),
            beacon
        );
    }

    @Test
    @Title("Меняем флаг подписи.")
    @TestCaseId("1843")
    public void saveLanguageAlias() {
        user.defaultSteps()
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).edit()
            )
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().editLang())
            .shouldSee(onSenderInfoSettingsPage().languagesDropdown());
        int position = Utils.getRandomNumber(LANGS.length - 1, 1);
        user.defaultSteps()
            .clicksOnElementWithText(
                onSenderInfoSettingsPage().languagesDropdown().langList(),
                LANGS[position]
            )
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().saveBtn())
            .shouldContainText(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).langText(),
                LANGS[position]
            )
            .onMouseHoverAndClick(
                onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList().get(0).edit()
            )
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().editLang())
            .clicksOn(onSenderInfoSettingsPage().languagesDropdown().langList().get(0))
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().editSignatureBlock().saveBtn());
    }
}
