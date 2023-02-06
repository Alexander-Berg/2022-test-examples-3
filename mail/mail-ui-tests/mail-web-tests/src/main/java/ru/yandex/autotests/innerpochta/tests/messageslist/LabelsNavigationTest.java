package ru.yandex.autotests.innerpochta.tests.messageslist;

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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тесты на навигацию по меткам")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.LABELS)
public class LabelsNavigationTest extends BaseTest {

    private final static String USER_FOLDER = Utils.getRandomString();
    private final static String USER_LABEL = Utils.getRandomString();
    private final static String USER_INNER_FOLDER = Utils.getRandomString();
    private static final String ATTACH_LOCAL_PDF_NAME = "doc.pdf";

    private String subject;
    private Folder folder;
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
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        folder = user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        user.apiLabelsSteps().addNewLabel(USER_LABEL, LABELS_PARAM_GREEN_COLOR);
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Ошибка заполнения полей при создание фильтра для метки")
    @TestCaseId("1536")
    public void shouldSeeNotifyAboutEmptyFieldsInFilter() {
        user.defaultSteps()
            .clicksOn(onMessagePage().labelsNavigation().userLabels().get(0))
            .shouldSee(onHomePage().putMarkAutomaticallyButton())
            .clicksOn(onHomePage().putMarkAutomaticallyButton())
            .clicksOn(onFiltersOverview().newFilterPopUp().submitFilterButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().emptyNotification());
    }

    @Test
    @Title("Снимаем метку с письма")
    @TestCaseId("4295")
    public void shouldUnmarkMessageInLabel() {
        user.messagesSteps().selectMessageWithSubject(subject)
            .markMessageWithCustomLabel(USER_LABEL);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().labelsNavigation().userLabels().get(0));
        user.messagesSteps().selectMessageWithSubject(subject)
            .unlabelsMessageWithCustomMark();
        user.defaultSteps().refreshPage();
        user.defaultSteps().shouldSee(onHomePage().putMarkAutomaticallyButton());
    }

    @Test
    @Title("Открываем письма с меткой, по клику на метку в теме письма")
    @TestCaseId("1537")
    public void testOpenCustomLabel() {
        user.messagesSteps().clicksOnMessageWithSubject(subject)
            .clicksOnMessageCheckBoxWithSubject(subject)
            .markMessageWithCustomLabel(USER_LABEL);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().labelsNavigation().userLabels().get(0));
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeThatMessageIsLabeledWith(USER_LABEL, subject);
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).labels().get(0));
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Проставление метки «С вложениями»")
    @TestCaseId("1538")
    public void testWithAttachmentsLabel() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), ATTACH_LOCAL_PDF_NAME)
            .inputsSubject(subject)
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().showWithAttach());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Проставление метки «Непрочитанные»")
    @TestCaseId("1539")
    public void testUnreadLabel() {
        String secondSubject = user.apiMessagesSteps().sendMail(
            lock.firstAcc(),
            Utils.getRandomName(),
            ""
        ).getSubject();
        String thirdSubject = user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.apiFoldersSteps().createNewSubFolder(USER_INNER_FOLDER, folder);
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(1));
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(thirdSubject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(2))
            .clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldSeeMessageWithSubject(subject, secondSubject, thirdSubject)
            .shouldSeeThatMessagesAreInFolder(USER_FOLDER, subject)
            .shouldSeeThatMessagesAreInFolder(INBOX_RU, secondSubject)
            .shouldSeeThatMessagesAreInFolder(USER_INNER_FOLDER, thirdSubject);
    }
}
