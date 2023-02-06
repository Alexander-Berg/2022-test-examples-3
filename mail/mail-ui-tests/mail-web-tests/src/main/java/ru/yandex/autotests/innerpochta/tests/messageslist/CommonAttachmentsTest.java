package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

@Aqua.Test
@Title("Тест на скрепку аттачей")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.ATTACHES)
public class CommonAttachmentsTest extends BaseTest {

    private static final String ATTACH_SIZE = "763 КБ";
    private static final String CORRECT_TOOLTIP = "attach.png (418 КБ)";
    private static final String DV_URL = "https://docs.yandex.ru";
    private String subj = getRandomString();
    private String subjThread = getRandomString();

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subjThread, "");
        user.apiMessagesSteps().sendMailWithAttachmentsToThreadWithSubject(lock.firstAcc().getSelfEmail(), subjThread,
            getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(lock.firstAcc().getSelfEmail(), subj,
            getRandomString(), PDF_ATTACHMENT, IMAGE_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем и закрываем попап аттачей в одиночном письме")
    @TestCaseId("1485")
    public void shouldSeeAttachments() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments())
            .onMouseHover(onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().download())
            .shouldSeeThatElementTextEquals(
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().size(), ATTACH_SIZE
            )
            .clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().infoBtn());
        user.messagesSteps().shouldSeeDropdownListOfAttachments();
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().messagePageAttachmentsBlock().counterBtn())
            .shouldNotSee(onMessagePage().messagePageAttachmentsBlock());
    }

    @Test
    @Title("Тест на виджеты аттачей в треде")
    @TestCaseId("1971")
    public void shouldSeeAttachInThread() {
        user.defaultSteps()
            .shouldSee(onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments())
            .onMouseHover(
                onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments().download()
            )
            .shouldSee(onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments().size())
            .clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments().infoBtn())
            .shouldSee(onMessagePage().messagePageAttachmentsBlock());
    }

    @Test
    @Title("Тест на НЕ показ виджета аттачей в инбоксе")
    @TestCaseId("1972")
    public void shouldNotSeeAttach() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем показ вложений",
            of(SETTINGS_DISABLE_INBOXATTACHS, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(
                onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments(),
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments()
            )
            .clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(subj).paperClip());
        user.messagesSteps().shouldSeeDropdownListOfAttachments();
        user.defaultSteps().opensFragment(QuickFragments.ATTACHMENTS)
            .refreshPage()
            .shouldSee(
                onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments(),
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments()
            );
    }

    @Test
    @Title("3pane: скрепка у треда в инбоксе")
    @TestCaseId("2003")
    public void shouldSeePaperClip3pane() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        user.defaultSteps().refreshPage()
            .shouldNotSee(
                onMessagePage().displayedMessages().firstMessageWithSubject(subjThread).attachments(),
                onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments()
            )
            .clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(subj).paperClip());
        user.messagesSteps().shouldSeeDropdownListOfAttachments();
    }

    @Test
    @Title("Тултип по ховеру на аттач-картинку должен содержать корректный текст")
    @TestCaseId("1948")
    public void shouldTooltipAfterHoverOnAttach() {
        String toolTip = onMessagePage().displayedMessages()
            .firstMessageWithSubject(subjThread).attachments().list().get(1).toolTip().getAttribute("title");
        assertThat("Тултип содержит неверный текст", toolTip, equalTo(CORRECT_TOOLTIP));
    }

    @Test
    @Title("Просмотр файла через DV через превью аттачей в инбоксе")
    @TestCaseId("1070")
    public void shouldOpenAttachmentInNewTab() {
        user.defaultSteps().onMouseHoverAndClick(
            onMessagePage().displayedMessages().firstMessageWithSubject(subj).attachments().list().get(0).show()
        )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DV_URL));
    }
}
