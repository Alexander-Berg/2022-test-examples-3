package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Шаблоны")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.TEMPLATES)
public class TemplateTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private String subject = getRandomString();
    private String msgText = getRandomString();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().createTemplateWithAttachmentsAndHTMLBody(
            lock.firstAcc(),
            subject,
            messageHTMLBodyBuilder(stepsProd.user()).addBoldText(msgText).addInlineAttach(IMAGE_ATTACHMENT).build()
        );
        stepsProd.user().apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        stepsProd.user().apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), getRandomString(), getRandomString());
    }

    @Test
    @Title("Открываем выпадушку шаблонов")
    @TestCaseId("3230")
    public void shouldSeeTemplatesDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
            .shouldSee(st.pages().mail().composePopup().expandedPopup().templatePopup());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Новый шаблон»")
    @TestCaseId("3231")
    public void shouldSeeNewTemplate() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
            .shouldSee(st.pages().mail().composePopup().expandedPopup().templatePopup().saveBtn());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем шаблон из списка")
    @TestCaseId("3232")
    public void shouldSeeCurrentTemplate() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
            .clicksOn(
                st.pages().mail().composePopup().expandedPopup().templatePopup().templateList().get(1),
                st.pages().mail().composePopup().expandedPopup().templatesBtn()
            )
            .shouldSee(st.pages().mail().composePopup().expandedPopup().templatePopup().saveAsNewTemplateBtn());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отвечаем на письмо шаблоном с аттачами")
    @TestCaseId("4647")
    public void shouldReplyWithTemplateWithAttach() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .rightClick(st.user().pages().MessagePage().displayedMessages().list().get(0).subject())
                .shouldSee(st.user().pages().MessagePage().allMenuList().get(0))
                .clicksOn(st.user().pages().MessagePage().allMenuList().get(0).reply())
                .shouldSee(st.user().pages().ComposePopup().expandedPopup())
                .clicksOn(st.user().pages().ComposePopup().expandedPopup().templatesBtn())
                .shouldSee(st.user().pages().ComposePopup().expandedPopup().templatePopup())
                .clicksOn(st.user().pages().ComposePopup().expandedPopup().templatePopup().templateList().get(2))
                .shouldContainText(st.user().pages().ComposePopup().expandedPopup().bodyInput(), msgText);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
