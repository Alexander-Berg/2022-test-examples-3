package ru.yandex.autotests.innerpochta.tests.screentests;

import com.google.common.collect.Sets;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.className;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.USER_FOLDER_FID_8;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author oleshko
 */
@Aqua.Test
@Description("У юзера подготовлены папка с вложенными папками на диске")
@Title("Тесты на сохранение на диск")
@Features(FeaturesConst.SAVE_TO_DISK)
@Stories(FeaturesConst.GENERAL)
@UseCreds(SaveToDiskScreenTest.CREDS)
public class SaveToDiskScreenTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        className(".js-save-all-disk-loading")
    );

    public static final String CREDS = "SaveToDiskTest";
    private static final String USER_WITH_FULLED_DISK = "UserWithFulledDisk";
    private static final String SUBJECT_DISK = "Attachments from disk";
    private static final String SUBJECT_DEVICE = "Attachments from device";
    private static final int FOLDER_WITH_FOLDERS = 2;
    private static final String SUCCESS_FOR_ONE = "загружен";
    private static final String SUCCESS_FOR_ALL = "Файлы сохранены в папке «Загрузки»";
    private static final String TEXT_LOADING = "Загрузка";
    private static final String SUBJECT_HEAVY_ATTACH = "Письмо с тяжелым аттачем";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Вложенные папки при сохранении на диск")
    @TestCaseId("321")
    public void shouldSeePopupWithDiskFolders() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DISK);
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().attachmentsBlock())
                .clicksOn(st.pages().touch().messageView().attachmentsBlock().attachments().get(0))
                .clicksOn(st.pages().touch().messageView().viewer().saveToDisk())
                .shouldSee(st.pages().touch().messageView().saveToDisk())
                .clicksOn(st.pages().touch().messageView().saveToDisk().folders().get(FOLDER_WITH_FOLDERS))
                .shouldSee(st.pages().touch().messageView().saveToDisk().activeFolder())
                .clicksOn(st.pages().touch().messageView().saveToDisk().folders().get(FOLDER_WITH_FOLDERS + 1))
                .shouldSee(st.pages().touch().messageView().saveToDisk().activeFolder())
                .clicksOn(st.pages().touch().messageView().saveToDisk().folders().get(FOLDER_WITH_FOLDERS + 2));
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .run();
    }

    @Test
    @Title("Проверяем ошибку при сохранении аттача на диск повторно")
    @TestCaseId("319")
    public void shouldNotSaveAttachmentsTwice() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DEVICE);
            saveAttachmentToDisk(st, 2);
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().statusLineError())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageView().statusLineError());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .run();
    }

    @Test
    @Title("Статуслайн об успешном сохранении на диск одного аттача")
    @TestCaseId("324")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-7002")
    public void shouldSeeSuccessAttachmentSaving() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DISK);
            saveAttachmentToDisk(st, 1);
            st.user().defaultSteps()
                .shouldContainText(st.pages().touch().messageView().statusLineInfo(), SUCCESS_FOR_ONE);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .run();
    }

    @Test
    @Title("Статуслайн о начале сохранения на диск одного аттача")
    @TestCaseId("324")
    public void shouldSeeProcessAttachmentSaving() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DEVICE);
            saveAttachmentToDisk(st, 1);
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageView().statusLineInfo())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldContainText(st.pages().touch().messageView().statusLineInfo(), TEXT_LOADING);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .run();
    }

    @Test
    @Title("Статуслайн об успешном сохранении на диск всех аттачей")
    @TestCaseId("726")
    public void shouldSeeSuccessAllAttachmentSaving() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DEVICE);
            saveAllAttachmentsToDisk(st);
            st.user().defaultSteps().shouldNotSee(st.pages().touch().messageView().attachmentsBlock().trobber())
                .shouldSee(st.pages().touch().messageList().statusLineInfo())
                .waitInSeconds(3);
            st.pages().touch().messageList().statusLineInfo()
                .waitUntil(
                    "Текст статуслайна отличается от ожидаемого",
                    hasText(SUCCESS_FOR_ALL),
                    10
                );
            st.user().defaultSteps().executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldContainText(st.pages().touch().messageList().statusLineInfo(), SUCCESS_FOR_ALL);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .run();
    }

    @Test
    @Title("Статуслайн о начале сохранения на диск всех аттачей")
    @TestCaseId("726")
    public void shouldSeeProcessAllAttachmentSaving() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_DEVICE);
            saveAllAttachmentsToDisk(st);
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().statusLineInfo())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldContainText(st.pages().touch().messageList().statusLineInfo(), TEXT_LOADING);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_8))
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Ошибка сохранения на диск аттача из-за недостатка места")
    @TestCaseId("703")
    @UseCreds(USER_WITH_FULLED_DISK)
    public void shouldSeeErrorOfNoEnoughDiskSpace() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_HEAVY_ATTACH);
            saveAttachmentToDisk(st, 1);
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageView().statusLineError())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageView().statusLineError());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Ошибка сохранения на диск всех аттачей из-за недостатка места")
    @TestCaseId("699")
    @UseCreds(USER_WITH_FULLED_DISK)
    public void shouldSeeErrorOfNoEnoughDiskSpaceWhenSaveAllAttaches() {
        Consumer<InitStepsRule> act = st -> {
            openMsg(st, SUBJECT_HEAVY_ATTACH);
            saveAllAttachmentsToDisk(st);
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageView().statusLineError())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageView().statusLineError());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Step("Открываем аттач во вьюере, сохраняем его на диск n раз")
    private void saveAttachmentToDisk(InitStepsRule st, int n) {
        for (int i = 0; i < n; i++)
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageView().attachmentsBlock())
                .clicksOn(st.pages().touch().messageView().attachmentsBlock().attachments().get(0))
                .clicksOn(st.pages().touch().messageView().viewer().saveToDisk())
                .clicksOn(st.pages().touch().messageView().saveToDisk().saveBtn())
                .shouldSee(st.pages().touch().messageView().attachmentsBlock());
    }

    @Step("Дважды сохраняем все аттачи на диск")
    private void saveAllAttachmentsToDisk(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().touch().messageView().attachmentsBlock().saveToDisk());
    }

    @Step("Открываем письмо с темой")
    private void openMsg(InitStepsRule st, String subject) {
        st.user().defaultSteps().shouldSee(st.pages().touch().messageList().headerBlock())
            .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), subject);
    }
}
