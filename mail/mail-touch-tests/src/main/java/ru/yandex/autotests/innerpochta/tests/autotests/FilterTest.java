package ru.yandex.autotests.innerpochta.tests.autotests;

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
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.IMPORTANT_LABEL;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на фильтры по папке")
@Features(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class FilterTest {

    private static final String WITH_ATTACHMENTS = "С вложениями",
        IMPORTANT = "Важные",
        UNREAD = "Непрочитанные";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().apiLabelsSteps().markImportant(
            steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), IMPORTANT, Utils.getRandomString())
        );
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            WITH_ATTACHMENTS,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), UNREAD, Utils.getRandomString());
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().apiFoldersSteps().createTemplateFolder();
        steps.user().apiFoldersSteps().createArchiveFolder();
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[] urls() {
        return new Object[][]{
            {TRUE, INBOX},
            {FALSE, ARCHIVE},
            {FALSE, DRAFT},
            {FALSE, TEMPLATE},
            {FALSE, SPAM},
            {FALSE, TRASH},
        };
    }

    @Test
    @Title("Не должны видеть фильтры в архиве, черновиках, спаме, удалённых, табах")
    @TestCaseId("1175")
    @UseDataProvider("urls")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldNotSeeFiltersInSomeFolders(Boolean setting, String folder) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Меняем настройку табов",
            of(FOLDER_TABS, setting)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            FOLDER_ID.makeTouchUrlPart(steps.user().apiFoldersSteps().getFolderBySymbol(folder).getFid())
        )
            .clicksOn(steps.pages().touch().messageList().headerBlock().filterName())
            .shouldNotSee(steps.pages().touch().messageList().filterList());
    }

    @Test
    @Title("Не должны видеть фильтры в метках")
    @TestCaseId("1175")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldNotSeeFiltersInLabels() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(LABEL_ID.makeTouchUrlPart(IMPORTANT_LABEL))
            .clicksOn(steps.pages().touch().messageList().headerBlock().filterName())
            .shouldNotSee(steps.pages().touch().messageList().filterList());
    }

    @Test
    @Title("Должны закрыть меню фильтров по тапу в шапку")
    @TestCaseId("1161")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldHideFiltersByHeader() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().filterName())
            .shouldSee(steps.pages().touch().messageList().filterList())
            .clicksOn(steps.pages().touch().messageList().headerBlock().filterName())
            .shouldNotSee(steps.pages().touch().messageList().filterList());
    }

    @Test
    @Title("Должны закрыть меню фильтров по тапу в паранжу")
    @TestCaseId("1163")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldHideFiltersByBackground() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().filterName())
            .offsetClick(10, 300)
            .shouldNotSee(steps.pages().touch().messageList().filterList());
    }

    @Test
    @Title("Должны свернуть меню фильтров при переходе между папками")
    @TestCaseId("1172")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldHideFiltersInNewFolder() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().headerBlock().filterName(),
            steps.pages().touch().messageList().headerBlock().sidebar(),
            steps.pages().touch().sidebar().inboxFolder()
        )
            .shouldNotSee(steps.pages().touch().messageList().filterList());
    }
}
