package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Верстка печати из просмотра письма в списке писем")
@Description("У пользователя должно быть письмо и группа писем c аттачами")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.COMPACT_VIEW)
public class PrintMessageCompactViewTest {

    private static final String SINGLE_MESSAGE_SUBJECT = "PRINT_MSG";
    private static final String THREAD_MESSAGE_SUBJECT = "PRINT_GROUP_MSG";

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
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            SINGLE_MESSAGE_SUBJECT,
            getRandomString(),
            IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            THREAD_MESSAGE_SUBJECT,
            getRandomString(),
            IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendMessageToThreadWithSubject(THREAD_MESSAGE_SUBJECT, lock.firstAcc(), "");
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
    }

    @Test
    @Title("Печать одиночного письма")
    @TestCaseId("927")
    public void shouldPrintMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SINGLE_MESSAGE_SUBJECT);
            st.user().defaultSteps().clicksOn(
                    st.pages().mail().msgView().contentToolbarBlock().moreBtn(),
                    st.pages().mail().msgView().miscField().printBtn()
                )
                .switchOnWindow(1);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Печать группы писем")
    @TestCaseId("927")
    public void shouldPrintMessageGroup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(THREAD_MESSAGE_SUBJECT);
            st.user().defaultSteps().clicksOn(
                    st.pages().mail().msgView().messageSubject().threadToolbarButton(),
                    st.pages().mail().msgView().commonToolbar().printButton()
                )
                .switchOnWindow(1);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
