package ru.yandex.autotests.innerpochta.tests.screentests.Avatars;

import com.google.common.collect.Sets;
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
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.passport.api.core.rules.LogTestStartRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_RQST;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.DRAFT_FOLDER;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COM;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на аватарки")
@Features(FeaturesConst.AVATARS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarScreenTest {

    private static final String SELF_EMAIL = "yandex-team-mailt-56@yandex.ru";
    private static final String SEARCH_INPUT = "yandex";
    private static final String SELF_EMAIL_PART = "yandex-team-";

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 60)
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".composeYabbles-to"),
        cssSelector(".search-inputHelper")
    );

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
        stepsProd.user().apiMessagesSteps()
            .addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), getRandomName(), "");
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(getRandomName())
                    .withSender(new UserSpec(USER_WITH_AVATAR_EMAIL, "Other User"))
                    .addReceiver(new UserSpec(acc.firstAcc().getSelfEmail(), "Other User"))
                    .build()
            )
            .closeConnection();
    }

    @Test
    @Title("Двухбуквенная монограмма в аватарке залогиненного юзера")
    @TestCaseId("629")
    public void shouldSeeMonogramInFolderList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().headerBlock())
                .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldSee(st.pages().touch().sidebar().sidebarAvatar());

        stepsProd.user().apiMessagesSteps().deleteAllMessagesInFolder(
            stepsProd.user().apiFoldersSteps().getFolderBySymbol(INBOX)
        ); //удаляем письма, чтобы при сранении скринов не маячил прыщик и каунтер непрочитанных у папки
        parallelRun.withAcc(acc.firstAcc()).withIgnoredAreas(IGNORED_AREA).withActions(actions).run();
    }

    @Test
    @Title("Аватарка удалённых в поиске")
    @TestCaseId("694")
    public void shouldSeeTrashAvatarInSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().search().messageBlock());

        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, TRASH);
        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT)))
            .run();
    }

    @Test
    @Title("Аватарки в саджесте композа")
    @TestCaseId("691")
    public void shouldSeeAvatarsInComposeSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksAndInputsText(st.pages().touch().composeIframe().inputTo(), SEARCH_INPUT)
                .shouldSee(st.pages().touch().composeIframe().composeSuggest());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Монограмы алиасов в композе")
    @TestCaseId("109")
    public void shouldSeeAliasesAvatarsInComposeSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldNotSee(st.pages().touch().messageList().bootPage())
                .clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .offsetClick(st.pages().touch().composeIframe().fieldFrom(), 100, 10)
                .shouldSee(st.pages().touch().composeIframe().suggestAliases());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Аватарка в яблах в композе")
    @TestCaseId("689")
    @DataProvider({SELF_EMAIL_PART, USER_WITH_AVATAR_EMAIL})
    public void shouldSeeAvatarsInYabbles(String rqst) {
        Consumer<InitStepsRule> actions = st ->
            addContactFromSuggest(st, rqst);

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Аватарки в поисковом саджесте")
    @TestCaseId("108")
    public void shouldSeeAvatarsInSearchSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksAndInputsText(st.pages().touch().search().header().input(), SEARCH_INPUT)
                .shouldSee(st.pages().touch().search().searchSuggestAvatar());

        parallelRun.withActions(actions).withAcc(acc.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart())
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Должны увидеть аватарки в ябблах при просмотре черновика")
    @TestCaseId("172")
    public void shouldSeeAvatarsInDraftEdit() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().yabble());
        };
        stepsProd.user().apiMessagesSteps().prepareDraft(USER_WITH_AVATAR_EMAIL, getRandomName(), "");
        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER)).run();
    }

    @Issue("QUINN-6601")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Test
    @Title("Должен уведеть аватарку у яббла после редактировании адреса")
    @TestCaseId("1086")
    public void shouldSeeAvatarEditedYabble() {
        Consumer<InitStepsRule> actions = st -> {
            addContactFromSuggest(st, SELF_EMAIL);
            st.user().defaultSteps().clicksOn(
                st.pages().touch().composeIframe().yabble(),
                st.pages().touch().composeIframe().editableYabble()
            );
            st.user().hotkeySteps().pressHotKeys(
                st.pages().touch().composeIframe().editableYabble(),
                Keys.BACK_SPACE.toString(), Keys.BACK_SPACE.toString()
            );
            st.user().defaultSteps().appendTextInElement(st.pages().touch().composeIframe().editableYabble(), COM)
                .clicksOn(st.pages().touch().composeIframe().inputBody())
                .shouldSee(st.pages().touch().composeIframe().yabble());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Аватарки в поисковой выдаче")
    @TestCaseId("681")
    public void shouldSeeAvatarsInSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksAndInputsText(st.pages().touch().search().header().input(), SEARCH_INPUT)
                .clicksOn(st.pages().touch().search().header().find())
                .shouldSee(st.pages().touch().search().messageBlock());

        parallelRun.withActions(actions).withAcc(acc.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart())
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Step("Добавляем ябблы в композе")
    private void addContactFromSuggest(InitStepsRule st, String rqst) {
        st.user().touchSteps().switchToComposeIframe();
        st.user().defaultSteps().clicksAndInputsText(st.pages().touch().composeIframe().inputTo(), rqst)
            .shouldSee(st.pages().touch().composeIframe().composeSuggest())
            .waitInSeconds(1)
            .clicksOn(st.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())).get(0))
            .shouldSee(st.pages().touch().composeIframe().yabble());
    }
}
