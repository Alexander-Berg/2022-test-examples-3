package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие скриночные тесты на фильтры по папке")
@Features(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class FilterScreenTest {

    private static final String WITH_ATTACHMENTS = "С вложениями",
        IMPORTANT = "Важные",
        UNREAD = "Непрочитанные";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
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

    @Before
    public void prepare() {
        stepsProd.user().apiLabelsSteps().markImportant(
            stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), IMPORTANT, Utils.getRandomString())
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            WITH_ATTACHMENTS,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead()
            .sendMailWithNoSave(acc.firstAcc(), UNREAD, Utils.getRandomString());
        stepsProd.user().apiFoldersSteps().purgeFolder(stepsProd.user().apiFoldersSteps().getFolderBySymbol(SENT));
    }

    @DataProvider
    public static Object[] filters() {
        return new Object[][]{
            {WITH_ATTACHMENTS},
            {IMPORTANT},
            {UNREAD}
        };
    }

    @Test
    @Title("Должен выбираться нужный фильтр")
    @TestCaseId("1164")
    @UseDataProvider("filters")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSelectFilter(String filter) {
        Consumer<InitStepsRule> act = st -> {
            openFilters(st);
            st.user().defaultSteps().shouldSeeElementsCount(st.pages().touch().messageList().messages(), 3)
                .clicksOnElementWithText(st.pages().touch().messageList().filterList(), filter)
                .shouldSeeElementsCount(st.pages().touch().messageList().messages(), 1);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть заглушки в пустых фильтрах")
    @TestCaseId("1173")
    @UseDataProvider("filters")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeуEmptyFilters(String filter) {
        Consumer<InitStepsRule> act = st -> {
            openFilters(st);
            st.user().defaultSteps().clicksOnElementWithText(st.pages().touch().messageList().filterList(), filter)
                .shouldSee(st.pages().touch().search().emptySearchResultImg());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(
            FOLDER_ID.makeTouchUrlPart(stepsProd.user().apiFoldersSteps().getFolderBySymbol(SENT).getFid())
        ).run();
    }

    @Test
    @Title("Должны видеть выпадушку с фильтрами")
    @TestCaseId("1160")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeFilterMenu() {
        Consumer<InitStepsRule> act = this::openFilters;

        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Step("Разворачиваем фильтры")
    private void openFilters(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().filterName())
            .shouldSee(st.pages().touch().messageList().filterList());
    }
}
