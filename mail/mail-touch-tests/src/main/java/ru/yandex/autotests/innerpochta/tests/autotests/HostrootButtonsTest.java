package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_AUTH_URL;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Кнопки на хоструте")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
public class HostrootButtonsTest {

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    private TouchRulesManager rules = touchRulesManager().withLock(null);
    private InitStepsRule steps = rules.getSteps();

    private static final String PASSPORT_URL_REGISTRATION = "passport.yandex.ru/registration";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Кнопка «Регистрация» в хедере перекидывает в паспорт")
    @TestCaseId("129")
    public void shouldGoToPassportWhenClickRegister() {
        steps.user().defaultSteps().opensDefaultUrl()
            .shouldSee(steps.pages().homer().logoYandex())
            .clicksOn(steps.pages().homer().createAccountBtnHeadBanner())
            .shouldBeOnUrl(containsString(PASSPORT_URL_REGISTRATION));
    }

    @Test
    @Title("Кнопка «Войти» в хедере перекидывает в паспорт")
    @TestCaseId("130")
    public void shouldGoToPassportWhenClickLogIn() {
        steps.user().defaultSteps().opensDefaultUrl()
            .shouldSee(steps.pages().homer().logoYandex())
            .clicksOn(steps.pages().homer().logInBtnHeadBanner())
            .shouldBeOnUrl(containsString(PASSPORT_AUTH_URL));
    }
}