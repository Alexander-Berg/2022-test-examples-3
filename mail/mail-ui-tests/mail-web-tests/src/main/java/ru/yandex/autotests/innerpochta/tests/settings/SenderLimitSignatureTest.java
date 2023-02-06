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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


@Aqua.Test
@Title("Тест на максимальное количество подписей")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderLimitSignatureTest extends BaseTest {

    private static final String MAX_COUNT_TEXT = "К сожалению, нельзя добавить более 20 подписей.";
    private static final int MAX_SIZE = 20;
    private static final String USER_SIGNATURE_TEXT = Utils.getRandomName();

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
        user.apiSettingsSteps().changeSignsAmountTo(MAX_SIZE);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
    }

    @Test
    @Title("Добавляем 21ю подпись юзеру.")
    @TestCaseId("1855")
    public void maxCountSignatures() {
        user.settingsSteps()
            .clearsSignatureText(onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0))
            .editSignatureValue(
                USER_SIGNATURE_TEXT,
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0)
            );
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().addBtn())
            .shouldSee(onHomePage().notification())
            .shouldContainText(onHomePage().notification(), MAX_COUNT_TEXT)
            .clicksOn(onHomePage().closeNotification());
        user.settingsSteps().shouldNotSeeSignatureWith(USER_SIGNATURE_TEXT);
        user.defaultSteps().shouldSeeElementsCount(
            onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            MAX_SIZE
        );
    }
}
