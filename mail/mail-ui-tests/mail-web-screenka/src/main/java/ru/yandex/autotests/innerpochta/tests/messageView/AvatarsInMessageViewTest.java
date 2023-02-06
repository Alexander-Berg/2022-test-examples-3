package ru.yandex.autotests.innerpochta.tests.messageView;

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
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Аватарки в шапке письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.MESSAGE_HEADER)
@RunWith(DataProviderRunner.class)
public class AvatarsInMessageViewTest {

    private static final String USER_WITHOUT_AVATAR = "yandex-team-31613.23887@yandex.ru";
    private static final String USER_WITH_AVATAR = "testoviy-test103@ya.ru";

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {LAYOUT_2PANE, EMPTY_STR},
            {LAYOUT_3PANE_VERTICAL, EMPTY_STR},
            {LAYOUT_2PANE, STATUS_TRUE},
        };
    }

    @Before
    public void setUp() {
        Message msg = stepsProd.user().apiMessagesSteps().addBccEmails(USER_WITH_AVATAR)
            .addCcEmails(USER_WITHOUT_AVATAR).sendMailWithCcAndBcc(
                lock.firstAcc().getSelfEmail(),
                Utils.getRandomName(),
                Utils.getRandomString()
            );
        stepsProd.user().apiMessagesSteps().markLetterRead(msg);
    }

    @Test
    @Title("Аватарки отправителя и получателей")
    @TestCaseId("4110")
    @UseDataProvider("testData")
    public void shouldSeeRecieverAvatars(String layout, String messageInListSetting) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().messageViewSteps().expandCcAndBccBlock();
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead().senderAvatar());
            shouldSeeReceiversAvatarsLoaded(st);
            st.user().defaultSteps().onMouseHover(st.pages().mail().msgView().messageHead().senderAvatar());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем нужный вид почты и отображение письма",
            of(
                SETTINGS_PARAM_LAYOUT, layout,
                SETTINGS_OPEN_MSG_LIST, messageInListSetting
            )
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("При просмотре спамного письма не показываем аватарки")
    @TestCaseId("6207")
    public void shouldNotSeeUserAvatarsInSpam() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().messageViewSteps().expandCcAndBccBlock();
        };
        stepsProd.user().apiMessagesSteps()
            .moveMessagesToSpam(stepsProd.user().apiMessagesSteps().getAllMessages().get(0));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(QuickFragments.SPAM).run();
    }

    @Step("Должны видеть, что все аватарки загрузились")
    private void shouldSeeReceiversAvatarsLoaded(InitStepsRule st) {
        for (MailElement receiverAvatar : st.pages().mail().msgView().messageHead().receiversAvatarsList()) {
            st.user().defaultSteps().shouldSee(receiverAvatar);
        }
    }
}
