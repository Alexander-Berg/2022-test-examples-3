package ru.yandex.autotests.innerpochta.tests.contextmenu;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Проверяем пункт “Поставить метку“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextMarkWithLabelTest extends BaseTest {

    private static final String CUSTOM_LABEL_1 = Utils.getRandomString();
    private static final String[] LABEL_LIST = {"Важные", CUSTOM_LABEL_1, "Новая метка…"};

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL_1, LABELS_PARAM_GREEN_COLOR);
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Выпадушка с метками")
    @TestCaseId("1243")
    public void shouldSeeLabelList() {
        user.leftColumnSteps().shouldSeeCustomLabel(CUSTOM_LABEL_1);
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeItemsInAdditionalContextMenu(LABEL_LIST);
    }

    @Test
    @Title("Ставим метку важное на письмо")
    @TestCaseId("1245")
    public void shouldBeMarkMessageWithImportantLabel() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel())
            .shouldNotSee(onMessagePage().allMenuListInMsgList().get(1).removeLabel())
            .clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(), "Важные");
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().allMenuListInMsgList().get(1).removeLabel(), "Важные")
            .clicksOn(onMessagePage().allMenuListInMsgList().get(1).removeLabel());
        user.messagesSteps().shouldSeeThatMessagesAreSelected()
            .shouldSeeThatMessageIsNotImportant(subject);
    }

    @Test
    @Title("Ставим существующую метку на тред")
    @TestCaseId("1246")
    public void shouldBeMarkThreadWithCustomLabel() {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 2)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(),
            CUSTOM_LABEL_1
        );
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(CUSTOM_LABEL_1, subject)
            .expandsMessagesThread(subject)
            .shouldSeeThatAllMessagesInThreadIsLabeledWith(CUSTOM_LABEL_1);
    }

    @Test
    @Title("Ставим существующую метку на письмо в треде")
    @TestCaseId("1247")
    public void shouldBeMarkOneMessageInThread() {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 2)
            .expandsMessagesThread(subject);
        user.defaultSteps()
            .rightClick(user.pages().MessagePage().displayedMessages().messagesInThread().get(0).sender());
        user.messagesSteps().shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(), CUSTOM_LABEL_1);
        user.messagesSteps().shouldSeeThatMessageInThreadIsLabeledWith(CUSTOM_LABEL_1, 0)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps()
            .shouldSeeThatElementTextEquals(onMessagePage().allMenuListInMsgList().get(1).removeLabel(), CUSTOM_LABEL_1)
            .clicksOn(onMessagePage().allMenuListInMsgList().get(1).removeLabel());
        user.messagesSteps().shouldSeeThatAllMessagesInThreadIsNotLabeledWithMarks();
    }
}
