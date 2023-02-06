package ru.yandex.autotests.innerpochta.tests.screentests.Avatars;


import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.passport.api.core.rules.LogTestStartRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на аватарки в просмотре письма/треда")
@Features(FeaturesConst.AVATARS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarInMsgViewScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static LogTestStartRule start = new LogTestStartRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .addBccEmails(USER_WITH_AVATAR_EMAIL)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), getRandomName(), "");
    }

    @Test
    @Title("Аватарки в деталях письма")
    @TestCaseId("111")
    public void shouldSeeMsgDetails() {
        Consumer<InitStepsRule> actions = this::openMsgAndExpandMsgDetails;

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны отображаться аватарки разных доменов в деталях письма")
    @TestCaseId("749")
    @DataProvider({"@ya.ru", "@yandex.by", "@yandex.com", "@yandex.kz", "@yandex.ru", "@yandex.ua"})
    public void shouldSeeAvatarsFromAliases(String alias) {
        Consumer<InitStepsRule> actions = this::openMsgAndExpandMsgDetails;

        stepsProd.user().apiMessagesSteps().deleteAllMessagesInFolder(
            stepsProd.user().apiFoldersSteps().getFolderBySymbol(INBOX)
        );
        stepsTest.user().imapSteps().connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSender(new UserSpec(
                        USER_WITH_AVATAR_EMAIL.replaceAll("@.*", "") + alias,
                        "Other User"
                    ))
                    .withSubject(getRandomString())
                    .build()
            )
            .closeConnection();
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Step("Открываем письмо, раскрываем детали письма")
    private void openMsgAndExpandMsgDetails(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock())
            .shouldNotSee(st.pages().touch().messageView().msgLoaderInView())
            .clicksOn(st.pages().touch().messageView().toolbar())
            .shouldSee(st.pages().touch().messageView().msgDetails());
    }
}
