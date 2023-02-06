package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Отображение аватарок компаний")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarsCompanyTest {

    private String subject = Utils.getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        Set<By> ignoredElements = new HashSet<>(IGNORED_ELEMENTS);
        ignoredElements.remove(cssSelector(".mail-User-Avatar"));
        parallelRun.withIgnoredElements(ignoredElements);
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(subject)
                    .withSender(new UserSpec("ebay@ebay.com", "Other User"))
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(subject)
                    .withSender(new UserSpec("info@twitter.com", "Other User"))
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(subject)
                    .withSender(new UserSpec("noreply@youtube.com", "Other User"))
                    .build()
            )
            .closeConnection();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
    }

    @Test
    @Title("Отображение аватарок компаний у писем в списке писем")
    @TestCaseId("4157")
    public void shouldSeeCompanyAvatarsInMessageList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().displayedMessages().list().get(0).avatarImg());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
