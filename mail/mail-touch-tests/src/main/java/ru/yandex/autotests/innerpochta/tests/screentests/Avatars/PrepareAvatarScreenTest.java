package ru.yandex.autotests.innerpochta.tests.screentests.Avatars;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.passport.api.core.rules.LogTestStartRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.USER_FOLDER_FID_7;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на аватарки")
@Description("У юзера стоит аватарка")
@Features(FeaturesConst.AVATARS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class PrepareAvatarScreenTest {

    private static final String SUBJECT = "subject";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".leftPanel-group_folders")
    );

    private String groupName;

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static LogTestStartRule start = new LogTestStartRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, DRAFT, TRASH, SENT));

    @Test
    @Title("Аватарка в уведомлении о новом письме")
    @TestCaseId("105")
    public void shouldStatusLineAboutNewMsg() {
        Consumer<InitStepsRule> act = st -> {
            st.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), SUBJECT, "");
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().statusLineNewMsg())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageList().statusLineNewMsg());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_7))
            .run();
    }

    @Test
    @Title("Должны увидеть аватарку у пользователя")
    @Description("У тестового пользователя стоит аватарка в формате png")
    @TestCaseId("341")
    public void shouldSeeAvatar() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .shouldSee(st.pages().touch().messageList().headerBlock())
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().sidebarAvatar());

        parallelRun.withActions(act).withAdditionalIgnoredElements(IGNORE_THIS).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Групповая аватарка в саджесте композа")
    @TestCaseId("690")
    public void shouldSeeGroupAvatarInComposeSuggest() {
        groupName = getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), groupName)
                .shouldSee(st.pages().touch().composeIframe().composeSuggest());
        };
        Name firstName = new Name().withFirst(getRandomName());
        Contact contact1 = stepsTest.user().abookSteps().createDefaultContact().withName(firstName);
        Contact contact2 = stepsTest.user().abookSteps().createDefaultContact().withName(firstName);
        stepsTest.user().apiAbookSteps().removeAllAbookContacts()
            .addNewContacts(
                contact1,
                contact2,
                stepsTest.user().abookSteps().createDefaultContact().withName(firstName)
            );
        stepsProd.user().apiAbookSteps().removeAllAbookGroups()
            .addNewAbookGroupWithContacts(
                groupName,
                stepsProd.user().apiAbookSteps().getPersonalContacts().get(0),
                stepsProd.user().apiAbookSteps().getPersonalContacts().get(1)
            );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }
}
