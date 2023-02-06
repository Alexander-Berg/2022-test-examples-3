package ru.yandex.autotests.innerpochta.tests.messagelist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllLabelsRule.removeAllLabelsRule;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveOldMessagesRule.removeOldMessagesRule;
import static ru.yandex.autotests.innerpochta.rules.resources.UnPinAllMessagesRule.unPinAllMessagesRule;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SHOW_WIDGETS_DECOR;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Верстка действий с письмами с виджетами")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@Description("Юзеру каждый день приходить письма с виджетами. Пак: 5d3048248a900e03944367b9")
public class WidgetActionsTest {

    private final String MSG_SUBJ_SUBSTR_TO_SELECT = "Your Booking Details";
    private final String OLDER_THAN_DAYS = "14";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(unPinAllMessagesRule(stepsProd.user()))
        .around(removeAllLabelsRule(stepsProd.user()))
        .around(removeOldMessagesRule(stepsProd.user(), INBOX, OLDER_THAN_DAYS));

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем корешки, объединяем аватары с чекбоксами",
            of(
                SETTINGS_SHOW_WIDGETS_DECOR, TRUE,
                SETTINGS_PARAM_MESSAGE_AVATARS, TRUE,
                SETTINGS_PARAM_MESSAGE_UNION_AVATARS, TRUE
            )
        );
        stepsProd.user().apiMessagesSteps().markAllMsgUnRead();
    }

    @Test
    @Title("Верстка выбранного виджета, аватарка в чекбоксе")
    @TestCaseId("2616")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSelectWidgetByClickOnAvatar() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT).avatarAndCheckBox()
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка выбранного виджета, аватарка отдельно от чекбокса")
    @TestCaseId("2616")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSelectWidgetByClickOnCheckBox() {
        disableAvatarsUnion();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT).checkBox()
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка прочитанного письма-виджета")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMarkWidgetRead() {
        Consumer<InitStepsRule> actions = st ->
            markMessageRead(st, getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка непрочитанного письма-виджета")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMarkWidgetUnread() {
        Consumer<InitStepsRule> actions = st -> {
            MessageBlock msgBlock = getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT);
            markMessageRead(st, msgBlock);
            markMessageUnread(st, msgBlock);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка закрепленного письма-виджета")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldPinWidget() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT).avatarAndCheckBox(),
            st.pages().mail().home().toolbar().pinBtn()
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка метки на письме с виджетом")
    @TestCaseId("2618")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldMarkWidgetWithLabel() {
        String labelName = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(
                getMostRecentMessageBySubject(st, MSG_SUBJ_SUBSTR_TO_SELECT).avatarAndCheckBox(),
                st.pages().mail().home().toolbar().markMessageDropDown(),
                st.pages().mail().home().labelsDropdownMenu().createNewLabel()
            );
            st.user().settingsSteps().inputsLabelName(labelName);
            st.user().defaultSteps().clicksOn(
                st.pages().mail().settingsFoldersAndLabels().newLabelPopUp().createMarkButton()
            );
            st.user().defaultSteps().clicksOn(st.pages().mail().home().labelsNavigation().userLabels().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Отключаем объединение аватара с чекбоксом")
    private void disableAvatarsUnion() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Отключаем объединение аватара с чекбоксом",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE)
        );
    }

    @Step("Помечаем письмо прочитанным")
    private void markMessageRead(InitStepsRule st, MessageBlock messageBlock) {
        st.user().defaultSteps().clicksOn(
            messageBlock.avatarAndCheckBox(),
            st.pages().mail().home().toolbar().markAsReadButton()
        );
    }

    @Step("Помечаем письмо непрочитанным")
    private void markMessageUnread(InitStepsRule st, MessageBlock messageBlock) {
        st.user().defaultSteps().clicksOn(
            messageBlock.avatarAndCheckBox(),
            st.pages().mail().home().toolbar().markAsUnreadButton()
        );
    }

    @Step("Получаем последнее письмо с темой «{1}»")
    private MessageBlock getMostRecentMessageBySubject(InitStepsRule st, String subject) {
        return st.user().pages().MessagePage().displayedMessages().list().filter(
            msg -> msg.subject().getText().contains(subject)
        ).get(0);
    }
}
