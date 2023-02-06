package ru.yandex.autotests.innerpochta.tests.homer;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Тесты на незалогиновые страницы Гомера")
@Features(FeaturesConst.HOMER)
@Tag(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class HomerGeneralTest {

    private final static String BASE_BUSY_URL = "/basebusy?err=1";

    private ScreenRulesManager rules = screenRulesManager()
        .withLock(null);

    private InitStepsRule stepsTest = rules.getStepsTest();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @DataProvider
    public static Object[][] urlPath() {
        return new Object[][]{
            {"/subscribe.html", "Вы подписаны"},
            {"/unsubscribe.html", "Вы отписаны"},
            {"/crossdomain.xml", "xml version"},
            {"/host-root2/s.jsx", "Вы собираетесь перейти по ссылке"},
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().defaultSteps().setsWindowSize(1920, 1080);
        stepsTest.user().defaultSteps().setsWindowSize(1920, 1080);
    }

    @Test
    @Title("Проверяем базовую верстку страницы")
    @TestCaseId("145")
    public void shouldSeeHomer() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().opensDefaultUrlWithDomain("ru");

        parallelRun.withActions(actions).run();
    }

    @Test
    @Title("Гомер для основных доменов")
    @TestCaseId("141")
    @DataProvider({"ru", "com", "com.tr"})
    public void shouldSeeHomerForDomains(String domain) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .opensDefaultUrlWithDomain(domain);

        parallelRun.withActions(actions).run();
    }

    @Test
    @Title("Гомер ресайз-версия для маленького окна")
    @TestCaseId("182")
    public void shouldSeeHomerTouch() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .opensDefaultUrlWithDomain("ru")
                // нельзя использовать withDomain() из-за бага в методе, валится после .com.tr из-за двух точек
                .setsWindowSize(900, 3500);
        };

        parallelRun.withActions(actions).run();
    }

    @Test
    @Title("Переходим по урлам")
    @TestCaseId("107")
    @UseDataProvider("urlPath")
    public void shouldSeeHomerPages(String urlPath, String text) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldContainText(st.pages().homer().pageContent(), text);

        parallelRun.withActions(actions).withDomain("ru").withUrlPath(urlPath).run();
    }

    @Test
    @Title("Открываем страницу BaseBusy")
    @TestCaseId("22")
    public void shouldSeeBaseBusyPage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensDefaultUrlWithPostFix(BASE_BUSY_URL)
                /* Ждем пока страница загрузится */
                .waitInSeconds(2);
        };

        parallelRun.withActions(actions).run();
    }
}