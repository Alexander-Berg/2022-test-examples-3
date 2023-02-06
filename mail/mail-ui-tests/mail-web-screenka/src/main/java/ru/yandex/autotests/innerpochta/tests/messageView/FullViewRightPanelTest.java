package ru.yandex.autotests.innerpochta.tests.messageView;

import com.google.common.collect.Sets;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WIDE_IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Правая колонка в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.RIGHT_PANEL)
public class FullViewRightPanelTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".js-toolbar-chevron"),
        cssSelector(".mail-Message-Head-Bottom"),
        cssSelector(".ns-view-footer"),
        cssSelector(".mail-User-Picture"),
        cssSelector(".ns-view-toolbar-box")
    );
    private String subj = Utils.getRandomName();
    private static final String LONG_BODY = StringUtils.repeat("hi ", 5000);
    private static final String LINK_TEXT = "Ссылка 1: https://disk.yandex.ru\nСсылка 2: https://disk.yandex" +
        ".ru/client/disk";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private List<Message> msgs;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Устанавливаем ширину почты на 1500px",
            of(SETTINGS_SIZE_VIEW_APP, 1500)
        );
    }

    @Test
    @Title("Должны видеть «Письма от»")
    @TestCaseId("2192")
    public void shouldSeeMessagesFrom() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageViewSideBar())
                .clicksOn(st.pages().mail().msgView().messageViewSideBar().messagesBySenderLink())
                .shouldSee(st.pages().mail().msgView().messageViewSideBar().messagesBySenderList());

        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj, "");
        stepsProd.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subj, lock.firstAcc(), "");
        msgs = stepsProd.user().apiMessagesSteps().getAllMessages();
        String url = QuickFragments.MSG_FRAGMENT.makeUrlPart(msgs.get(0).getMid());

        parallelRun.withActions(actions).withUrlPath(url).withIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc())
            .run();
    }

    @Test
    @Title("Должны видеть свёрнутую правую колонку, если она не влезает")
    @TestCaseId("6123")
    public void shouldSeeRightPanel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1000, 1000)
                .shouldSee(st.pages().mail().msgView().messageViewExpandSideBar())
                .shouldNotSee(st.pages().mail().msgView().messageViewSideBar());

        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithWideInlineAttachAndText()
        );
        msgs = stepsProd.user().apiMessagesSteps().getAllMessages();
        String url = QuickFragments.MSG_FRAGMENT.makeUrlPart(msgs.get(0).getMid());

        parallelRun.withActions(actions).withUrlPath(url).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть узкую правую колонку")
    @TestCaseId("2196")
    public void shouldSeeScrollInRightPanel() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1200, 1200)
                .refreshPage();
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageViewSideBar().messagesBySubjList());
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgView();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT,
            WIDE_IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT
        );
        msgs = stepsProd.user().apiMessagesSteps().getAllMessages();
        String url = QuickFragments.MSG_FRAGMENT.makeUrlPart(msgs.get(0).getMid());

        parallelRun.withActions(actions).withUrlPath(url).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть залипающий QR")
    @TestCaseId("2189")
    public void shouldSeeQR() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(
                st.pages().mail().msgView().messageViewSideBar(),
                st.pages().mail().msgView().quickReplyPlaceholder()
            );
        stepsProd.user().apiMessagesSteps()
            .sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(), Utils.getRandomString(),
                LONG_BODY
            );
        msgs = stepsProd.user().apiMessagesSteps().getAllMessages();
        String url = QuickFragments.MSG_FRAGMENT.makeUrlPart(msgs.get(0).getMid());

        parallelRun.withActions(actions).withUrlPath(url).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть правую колонку для соц сетей")
    @TestCaseId("2187")
    public void shouldSeeSocialRightPanel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView()
                .messageViewSideBar().messagesBySubjList());

        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject(Utils.getRandomString())
                    .withSender(new UserSpec("notification@facebookmail.com", "Facebook"))
                    .build()
            )
            .closeConnection();
        msgs = stepsProd.user().apiMessagesSteps().getAllMessages();
        String url = QuickFragments.MSG_FRAGMENT.makeUrlPart(msgs.get(0).getMid());

        parallelRun.withActions(actions).withUrlPath(url)
            .withIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc()).run();
    }
}
