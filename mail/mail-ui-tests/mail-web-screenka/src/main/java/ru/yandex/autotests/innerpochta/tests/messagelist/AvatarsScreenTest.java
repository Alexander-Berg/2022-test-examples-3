package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Общие тесты на аватары")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarsScreenTest {

    private static final String DEFAULT_SUBJECT = "subj";
    private static final String PROFILE_NAME_1 = "Петр Петров";
    private static final String PROFILE_NAME_2 = "Максим Шаповалов";

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
    public void disableIgnoringAvatars() {
        Set<By> ignoredElements = new HashSet<>(IGNORED_ELEMENTS);
        ignoredElements.remove(cssSelector(".mail-User-Avatar"));
        parallelRun.withIgnoredElements(ignoredElements);
    }

    @Test
    @Title("Отображение буквенной аватарки")
    @TestCaseId("4102")
    public void shouldSeeAlphabeticAvatar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.user().pages().HomePage().checkMailButton());
            st.user().messagesSteps().shouldSeeMessageWithSubject(DEFAULT_SUBJECT);
        };
        stepsProd.user().apiMessagesSteps()
            .sendMailFromName(lock.firstAcc(), DEFAULT_SUBJECT, "", PROFILE_NAME_1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение буквенной аватарки после удаления контакта")
    @TestCaseId("4102")
    public void shouldSeeAlphabeticAvatarAfterRemovingContact() {
        stepsProd.user().apiAbookSteps().removeAllAbookContacts();
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.user().pages().HomePage().checkMailButton());
            st.user().messagesSteps().shouldSeeMessageWithSubject(DEFAULT_SUBJECT);
        };
        stepsProd.user().apiMessagesSteps()
            .sendMailFromName(lock.firstAcc(), DEFAULT_SUBJECT, "", PROFILE_NAME_2);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
