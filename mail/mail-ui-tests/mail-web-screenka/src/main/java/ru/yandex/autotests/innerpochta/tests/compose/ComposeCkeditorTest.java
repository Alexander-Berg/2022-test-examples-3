package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.TEMPLATE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Оформление текста писем")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ComposeCkeditorTest {

    private static final String ATTACH_LOCAL_NAME = "attach.png";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".composeHeader-Text"),
        cssSelector(".ComposeStack"),
        cssSelector(".ns-view-right-box"),
        cssSelector(".mail-NestedList-Item-Info"),
        cssSelector(".js-messages-pager-scroll")
    );
    private String text = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(DISK_USER_TAG);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiFoldersSteps().createTemplateFolder();
        stepsProd.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сохраняем в черновике аттачи и отформатированный текст")
    @TestCaseId("3361")
    public void shouldSeeStyledTextAndAttachesInDrafts() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text)
                .clicksOn(st.pages().mail().home().foldersNavigation().draftFolder())
                .shouldBeOnUrlWith(QuickFragments.DRAFT)
                .refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().composeSteps().shouldSeeFormattedTextAreaContains(text);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            hotkeyEnterInMailBody(st);
            inputStyledTextWithHover(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().bold(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().underline()
            );
            st.user().composeSteps().uploadLocalFile(
                st.pages().mail().composePopup().expandedPopup().localAttachInput(),
                ATTACH_LOCAL_NAME
            );
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().closeBtn())
                .clicksOn(st.pages().mail().home().foldersNavigation().draftFolder())
                .shouldBeOnUrlWith(QuickFragments.DRAFT)
                .refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().expandedPopup().attachPanel()
                .linkedAttach().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withIgnoredElements(IGNORE_THIS)
            .runSequentially();
    }

    @Test
    @Title("Сохраняем в шаблоне аттачи и отформатированный текст")
    @TestCaseId("3362")
    public void shouldSeeStyledTextAndAttachesInTemplates() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().notificationBlock().createTemplateBtn())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
                .clicksOn(
                    st.pages().mail().composePopup().expandedPopup().templatePopup().saveBtn(),
                    st.pages().mail().composePopup().expandedPopup().closeBtn()
                )
                .shouldBeOnUrlWith(TEMPLATE)
                .refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().composeSteps().shouldSeeFormattedTextAreaContains(text);
            hotkeyEnterInMailBody(st);
            inputStyledTextWithHover(
                st,
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().bold(),
                st.pages().mail().composePopup().expandedPopup().toolbarBlock().underline()
            );
            st.user().composeSteps().uploadLocalFile(
                st.pages().mail().composePopup().expandedPopup().localAttachInput(),
                ATTACH_LOCAL_NAME
            );
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
                .clicksOn(
                    st.pages().mail().composePopup().expandedPopup().templatePopup().updateBtn(),
                    st.pages().mail().composePopup().expandedPopup().closeBtn()
                )
                .refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().expandedPopup().attachPanel()
                .linkedAttach().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withUrlPath(TEMPLATE).withAcc(lock.firstAcc()).withIgnoredElements(IGNORE_THIS)
            .runSequentially();
    }

    @Step("Применяем стиль и пишем строку в тело письма")
    private void inputStyledTextWithHover(InitStepsRule st, WebElement... buttons) {
        for (WebElement button : buttons) {
            st.user().defaultSteps().onMouseHoverAndClick(button)
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text)
                .onMouseHoverAndClick(button);
            hotkeyEnterInMailBody(st);
        }
    }

    @Step("Нажимаем комбинацию клавиш CTRL + Enter в тело письма")
    private void hotkeyEnterInMailBody(InitStepsRule st) {
        st.user().hotkeySteps().pressHotKeys(st.pages().mail().composePopup().expandedPopup().bodyInput(), ENTER);
    }
}
