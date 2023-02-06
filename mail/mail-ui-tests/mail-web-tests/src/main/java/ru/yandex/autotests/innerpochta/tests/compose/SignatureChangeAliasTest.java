package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.MailConst.DOMAIN_YANDEXRU;
import static ru.yandex.autotests.innerpochta.util.MailConst.DOMAIN_YARU;


/**
 * @author kurau
 */
@Aqua.Test
@Title("Проверка смены подписи при смене алиаса")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SignatureChangeAliasTest extends BaseTest {

    private static final String SIGN_FOR_YANDEXRU = "Signature for @yandex.ru";
    private static final String SIGN_FOR_YARU = "Signature for @ya.ru";
    private static final String LAST_SIGN_YARU = "Last Signature for @ya.ru";
    private static final String SIGN_FOR_COM = "Signature for @yandex.com";
    private static final String DOMAIN_COM = "@yandex.com";

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
        user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(SIGN_FOR_YANDEXRU).withEmails(singletonList(lock.firstAcc().getLogin() + DOMAIN_YANDEXRU)),
            sign(SIGN_FOR_YARU).withEmails(singletonList(lock.firstAcc().getLogin() + DOMAIN_YARU)),
            sign(LAST_SIGN_YARU).withEmails(singletonList(lock.firstAcc().getLogin() + DOMAIN_YARU)),
            sign(SIGN_FOR_COM).withEmails(singletonList(lock.firstAcc().getLogin() + DOMAIN_COM))
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.defaultSteps()
            .shouldSeeElementsCount(onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(), 4);
    }

    @Test
    @Title("Проверка смены подписи при смене алиаса")
    @TestCaseId("1858")
    public void shouldSeeSignForAlias() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().shouldSeeTextWithSignature(SIGN_FOR_YANDEXRU);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().yabbleFrom())
            .shouldSee(onComposePopup().fromSuggestList().get(0))
            .clicksOnElementWithText(
                onComposePopup().fromSuggestList(),
                lock.firstAcc().getLogin() + DOMAIN_COM
            );
        user.composeSteps().shouldSeeTextWithSignature(SIGN_FOR_COM);
    }

    @Test
    @Title("Если у алиаса несколько подписей - выбираем последнюю")
    @TestCaseId("4095")
    public void shouldSeeLastSignForAlias() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().shouldSeeTextWithSignature(SIGN_FOR_YANDEXRU);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().yabbleFrom())
            .shouldSee(onComposePopup().fromSuggestList().get(0))
            .clicksOnElementWithText(
                onComposePopup().fromSuggestList(),
                lock.firstAcc().getLogin() + DOMAIN_YARU
            );
        user.composeSteps().shouldSeeTextWithSignature(LAST_SIGN_YARU);
    }
}
