package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
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

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author crafty
 */

@Aqua.Test
@Title("Аватарки в композе")
@Features({FeaturesConst.COMPOSE})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.AVATARS)
@Description("Заранее подготовлена переписка с пользователями с аватарками и контакты с аватарками")
public class ComposeAvatarsTest {

    private static final String CONTACT_TO = "nat.test.ru@ya.ru";
    private static final String CONTACT_CC = "t.tuts2017@yandex.ru";
    private static final String CONTACT_BCC = "newnewnewa@yandex.ru";
    private static final String CONTACT_GROUP = "group";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".ns-view-footer"),
        cssSelector(".ns-view-collectors"),
        cssSelector(".js-layout-left-toggler"),
        cssSelector(".ns-view-compose-autosave-status"),
        cssSelector(".mail-NestedList-Item-Info"),
        cssSelector(".b-captcha__wrapper"),
        cssSelector(".b-account-activity__current-ip__ip"),
        cssSelector(".js-header-left-column"),
        cssSelector(".mail-SignatureChooser"),
        cssSelector(".ns-view-messages-filters-unread"),
        cssSelector(".mail-Compose-Field-Misc"),
        cssSelector(".ns-view-head-user"),
        cssSelector(".fid-6")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsTest.user(), DRAFT, TRASH));

    @Before
    public void setUp() {
        stepsTest.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_TRUE)
        );
    }

    @Test
    @Title("Проверяем аватарки отправителя и получателей в полях От кого, Кому, CC, BCC")
    @TestCaseId("4176")
    public void shouldSeeRecipientsAvatars() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), CONTACT_TO)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), CONTACT_CC)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), CONTACT_BCC)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем аватарки в саджесте")
    @TestCaseId("3565")
    public void shouldSeeAvatarsInSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
                .shouldSee(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0).avatar());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем аватарки в попапе «Добавить получателей»")
    @TestCaseId("3565")
    public void shouldSeeAvatarsInAbookPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
                .clicksOn(st.pages().mail().composePopup().abookBtn())
                .shouldSee(
                    st.pages().mail().compose().abookPopup().contacts().waitUntil(not(empty())).get(0).contactAvatar()
                );
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аватарки при ответе на тред")
    @TestCaseId("4177")
    public void shouldSeeAvatarsInThread() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .rightClick(st.pages().mail().home().displayedMessages().list().get(0).subject())
            .clicksOn(st.pages().mail().home().allMenuList().get(0).reply())
            .shouldSee(st.pages().mail().composePopup().expandedPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аватарки в развернутом qr")
    @TestCaseId("4128")
    public void shouldSeeAvatarInBigQR() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(
                st.pages().mail().home().displayedMessages().list().get(0).subject(),
                st.pages().mail().msgView().quickReplyPlaceholder()
            )
            .shouldSee(st.pages().mail().msgView().quickReply().openCompose());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем аватарку группы контактов")
    @TestCaseId("4226")
    public void shouldSeeGroupAvatar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT_GROUP);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

}
