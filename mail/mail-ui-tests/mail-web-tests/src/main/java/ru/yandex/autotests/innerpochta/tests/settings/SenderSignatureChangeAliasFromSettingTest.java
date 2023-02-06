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
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;

/**
 * @author kurau
 */
@Aqua.Test
@Title("Изменение алиаса подписи в настройках и проверка изменений")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderSignatureChangeAliasFromSettingTest extends BaseTest {

    private String signText = Utils.getRandomName();
    private String tmpSignature = Utils.getRandomName();

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(tmpSignature).withEmails(singletonList(lock.firstAcc().getLogin() + MailConst.DOMAIN_YARU))
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.defaultSteps().shouldSeeElementsCount(
            onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(), 1);
        user.settingsSteps().shouldSeeSignatureWith(tmpSignature);
    }

    @Test
    @Title("Изменение алиаса подписи в настройках и проверка изменений")
    @TestCaseId("1857")
    public void setAliasFromSetting() {
        user.settingsSteps()
            .clearsSignatureText(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0))
            .editSignatureValue(
                signText,
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0)
            );
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().aliasesCheckBox())
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().addBtn());
        user.settingsSteps().shouldSeeSignaturesCountInJsResponse(2);
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), signText);
    }
}
