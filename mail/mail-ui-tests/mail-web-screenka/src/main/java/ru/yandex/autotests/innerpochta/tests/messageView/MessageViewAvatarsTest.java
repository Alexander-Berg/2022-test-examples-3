package ru.yandex.autotests.innerpochta.tests.messageView;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.yandex.xplat.common.YSDate;
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
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Аватарки в просмотре письма")
@Features(FeaturesConst.AVATARS)
@Tag(FeaturesConst.AVATARS)
@Stories(FeaturesConst.AVATARS)
@RunWith(DataProviderRunner.class)
public class MessageViewAvatarsTest {

    private static final String THREAD = "thread";
    private String threadSubject = getRandomString();
    private String subject = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Before
    public void setUp() {
        Set<By> ignoredElements = new HashSet<>(IGNORED_ELEMENTS);
        ignoredElements.remove(cssSelector(".mail-User-Avatar"));
        parallelRun.withIgnoredElements(ignoredElements);
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(threadSubject)
                    .withTimestamp(new YSDate(dateFormat.format(date.plusMonths(3)) + "Z"))
                    .withSender(new UserSpec("hello@yandex.ru", "Other User"))
                    .addReceiver(new UserSpec(lock.firstAcc().getSelfEmail(), lock.firstAcc().getLogin()))
                    .build()
            );
        stepsProd.user().apiMessagesSteps()
            .sendMessageToThreadWithSubjectWithNoSave(threadSubject, lock.firstAcc(), "");
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(threadSubject)
                    .withSender(new UserSpec(USER_WITH_AVATAR_EMAIL, "Other User"))
                    .addReceiver(new UserSpec("hello@yandex.ru", "Other User"))
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(subject)
                    .withSender(new UserSpec(USER_WITH_AVATAR_EMAIL, "Other User"))
                    .addReceiver(new UserSpec(lock.firstAcc().getSelfEmail(), lock.firstAcc().getLogin()))
                    .build()
            )
            .closeConnection();
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Смотрим на аватарки в просмотре письма на отдельной странице")
    @TestCaseId("3561")
    @DoTestOnlyForEnvironment("Not IE")
    public void shouldSeeAvatarsInMessageFullView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensFragment(QuickFragments.SENT);
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldBeOnUrlWith(QuickFragments.MESSAGE)
                .clicksOn(st.pages().mail().msgView().messageHead().recipientsCount());
            shouldSeeAvatarsOnMessageViewLoaded(st);
        };

        createMsgWithQuote();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на карточку отправителя в просмотре письма на отдельной странице")
    @TestCaseId("3561")
    @DoTestOnlyForEnvironment("Not IE")
    public void shouldSeeMailCardInFullView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(subject);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().fromName())
                .shouldSee(st.pages().mail().msgView().mailCard());
            shouldSeeAvatarsLoaded(st);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на аватарки в просмотре письма в списке писем")
    @TestCaseId("3562")
    public void shouldSeeAvatarsInMessageCompactView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(subject);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().messageHead().recipientsCount());
            st.user().defaultSteps().shouldBeOnUrl(containsString(THREAD));
            shouldSeeAvatarsLoaded(st);
        };

        createMsgWithQuote();
        enableOpenMsgListSettings();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на карточку отправителя в просмотре письма в списке писем")
    @TestCaseId("3562")
    public void shouldSeeMailCardInCompactView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(subject);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().fromName())
                .shouldSee(st.pages().mail().msgView().mailCard());
            shouldSeeAvatarsLoaded(st);
        };

        enableOpenMsgListSettings();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на аватарки в просмотре письма в 3pane")
    @TestCaseId("3563")
    @DataProvider({LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    public void shouldSeeAvatarsInMessageView3Pane(String layout) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(threadSubject);
            st.user().defaultSteps()
                .shouldBeOnUrl(containsString(THREAD))
                .clicksOn(st.pages().mail().msgView().messageHead().recipientsCount());
            shouldSeeAvatarsLoaded(st);
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на карточку отправителя в просмотре письма в 3pane")
    @TestCaseId("3563")
    @DataProvider({LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    public void shouldSeeMailCardInView3Pane(String layout) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(threadSubject);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().fromName())
                .shouldSee(st.pages().mail().msgView().mailCard());
            shouldSeeAvatarsLoaded(st);
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на аватарки в Quick reply")
    @TestCaseId("3564")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL, SETTINGS_LAYOUT_3PANE_HORIZONTAL})
    public void shouldSeeMailCardInView2Pane(String layout) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(threadSubject);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .shouldSee(st.pages().mail().msgView().quickReply().sendButton());
            shouldSeeAvatarsLoaded(st);
        };

        enableOpenMsgListSettings();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем layout",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Включаем просмотр письма в списке писем и треды")
    private void enableOpenMsgListSettings() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем и треды",
            of(
                SETTINGS_OPEN_MSG_LIST, STATUS_ON,
                SETTINGS_FOLDER_THREAD_VIEW, true
            )
        );
    }

    @Step("Выключаем треды")
    private void disableThreadSettings() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
    }

    @Step("Должны видеть, что все аватарки загрузились")
    private void shouldSeeAvatarsLoaded(InitStepsRule steps) {
        List<MailElement> allAvatars = steps.pages().mail().msgView().allAvatars().waitUntil(not(empty()));
        steps.user().defaultSteps().shouldSee(allAvatars.toArray(new MailElement[0]));
    }

    @Step("Должны видеть, что все аватарки на странице просмотра письма загрузились")
    private void shouldSeeAvatarsOnMessageViewLoaded(InitStepsRule steps) {
        List<MailElement> allAvatars = steps.pages().mail().msgView().allAvatarsMessageView().waitUntil(not(empty()));
        steps.user().defaultSteps().shouldSee(allAvatars.toArray(new MailElement[0]));
    }

    @Step("Создаём письмо с цитированием")
    private void createMsgWithQuote() {
        disableThreadSettings();
        stepsTest.user().loginSteps().forAcc(lock.firstAcc()).logins();
        stepsTest.user().messagesSteps().clicksOnMessageWithSubject(subject);
        stepsTest.user().defaultSteps().clicksOn(stepsTest.pages().mail().msgView().toolbar().replyButton())
            .appendTextInElement(
                stepsTest.pages().mail().composePopup().expandedPopup().bodyInput(), getRandomString())
            .clicksOn(stepsTest.pages().mail().composePopup().expandedPopup().sendBtn()).
            opensDefaultUrl();
    }
}
