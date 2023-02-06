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

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


/**
 * Created by kurau on 17.02.14.
 */
@Aqua.Test
@Title("Проверка отключения промо о подписи, при >2 подписей")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderSignaturePromoTest extends BaseTest {

    private static int SIGNS_AMOUNT_TO_SWITCHOFF_PROMO = 3;

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
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
    }

    @Test
    @Title("Проверка отключения промо о подписи, при >2 подписей")
    @TestCaseId("1860")
    public void shouldNotSeePromoAfter3Signatures() {
        user.defaultSteps().shouldSee(onSenderInfoSettingsPage().promoSignature().newSignatureButton());
        user.apiSettingsSteps().changeSignsAmountTo(SIGNS_AMOUNT_TO_SWITCHOFF_PROMO);
        user.defaultSteps().refreshPage()
            .shouldNotSee(onSenderInfoSettingsPage().promoSignature().newSignatureButton());
    }

    @Test
    @Title("Добавление подписи через промо")
    @TestCaseId("2488")
    public void shouldAddSignatureAfterClickOnPromo() {
        user.defaultSteps().shouldSee(onSenderInfoSettingsPage().promoSignature().newSignatureButton())
            .clicksOn(onSenderInfoSettingsPage().promoSignature().newSignatureButton())
            .shouldSee(onSenderInfoSettingsPage().blockSetupSender().signatures().activeInput())
            .inputsTextInElement(
                onSenderInfoSettingsPage().blockSetupSender().signatures().input().get(0),
                Utils.getRandomString()
            )
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().signatures().addBtn())
            .shouldSeeElementsCount(onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(), 1);
    }
}
