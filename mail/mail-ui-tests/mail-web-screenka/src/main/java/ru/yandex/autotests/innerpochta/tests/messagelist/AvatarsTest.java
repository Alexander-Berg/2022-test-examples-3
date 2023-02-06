package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
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
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Отображение различных типов аватарок")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarsTest {

    private static final String DARK_THEME_QUERY_PARAM = "?theme=lamp";
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
    public void disableIgnoringAvatars() {
        Set<By> ignoredElements = new HashSet<>(IGNORED_ELEMENTS);
        ignoredElements.remove(cssSelector(".mail-User-Avatar"));
        parallelRun.withIgnoredElements(ignoredElements);
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(subject)
                    .withSender(new UserSpec(USER_WITH_AVATAR_EMAIL, "Other User"))
                    .addReceiver(new UserSpec(lock.firstAcc().getSelfEmail(), "Other User"))
                    .build()
            )
            .closeConnection();
        stepsProd.user().apiMessagesSteps().sendMessageToThreadWithSubject(subject, lock.firstAcc(), "");
        stepsProd.user().apiMessagesSteps().moveMessagesFromFolderToFolder(
            SENT,
            stepsProd.user().apiMessagesSteps().getAllMessages().get(1)
        );
    }

    @Test
    @Title("Отображение аватарок у писем в списке писем")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    @TestCaseId("4108")
    public void shouldSeeAvatarsInMessageList(String layout) {
        Consumer<InitStepsRule> actions = this::shouldSeeAvatarsLoaded;

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем отображение (2pane/3pane)",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение аватарок у писем в списке писем в тёмной теме")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    @TestCaseId("4167")
    public void shouldSeeAvatarsInMessageListWithDarkTheme(String layout) {
        Consumer<InitStepsRule> actions = this::shouldSeeAvatarsLoaded;

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем отображение (2pane/3pane)",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withUrlPath(DARK_THEME_QUERY_PARAM).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение аватарок на треде в папке «Входящие»")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    @TestCaseId("4101")
    public void shouldSeeAvatarsInThreadInbox(String layout) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().expandsMessagesThread(subject);
            shouldSeeAvatarsLoaded(st);
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем отображение (2pane/3pane) и включаем треды",
            of(
                SETTINGS_PARAM_LAYOUT, layout,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Должны видеть, что все аватарки загрузились")
    private void shouldSeeAvatarsLoaded(InitStepsRule st) {
        for (MessageBlock messageBlock : st.pages().mail().home().displayedMessages().list()) {
            st.user().defaultSteps().shouldSee(messageBlock.avatarImg());
        }
    }
}
