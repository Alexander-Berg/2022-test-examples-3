package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на нацдомены")
@Description("На юзере заранее создано письмо с аттачами")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.NAT_DOMAINS)
@RunWith(DataProviderRunner.class)
public class NatDomainsTest {

    private static final String CREDS_FR = "UserFr";
    private static final String CREDS_EE = "UserEE";

    private ScreenRulesManager rules = screenRulesManager()
        .withLock(AccLockRule.use().names(CREDS_FR, CREDS_EE));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
            {YandexDomain.FR, CREDS_FR},
            {YandexDomain.EE, CREDS_EE}
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Сохраняем аттач на Диск")
    @TestCaseId("3605")
    @UseDataProvider("data")
    public void shouldSeeSaveToDiskPopup(YandexDomain domain, String acc) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().forAcc(lock.acc(acc)).loginsToDomain(domain);
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().home().mail360HeaderBlock())
                .onMouseHoverAndClick(
                    st.pages().mail().home().displayedMessages().list().get(0)
                        .attachments().list().get(0).save()
                )
                .waitInSeconds(7); //ждем загрузки внешнего фрейма чтобы удостовериться, что аттач сохранили успешно
        };
        parallelRun.withActions(actions).run();
    }
}
