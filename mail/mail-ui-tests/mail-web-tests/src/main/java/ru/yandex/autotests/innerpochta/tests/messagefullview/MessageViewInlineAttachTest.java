package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Инлайн аттач в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.ATTACHES)
public class MessageViewInlineAttachTest extends BaseTest {

    private static final String MSG_WITH_INLINE_ATTACH = "инлайн";
    private static final String FWD_MSG_WITH_INLINE_ATTACH = "Fwd: инлайн";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            MSG_WITH_INLINE_ATTACH,
            messageHTMLBodyBuilder(user).makeBodyWithInlineAttachAndText()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Пересылаем письмо с инлайн-аттачем")
    @TestCaseId("4309")
    public void shouldForwardMessageWithInlineAttach() {
        user.messagesSteps().clicksOnMessageWithSubject(MSG_WITH_INLINE_ATTACH);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardMessageButton())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().expandedPopup().popupTo())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail());
        user.hotkeySteps().pressHotKeys(onComposePopup().expandedPopup().popupTo(), ENTER);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().disableComposeAlert();
        user.defaultSteps().opensDefaultUrl();
        user.messagesSteps().clicksOnMessageWithSubject(FWD_MSG_WITH_INLINE_ATTACH);
        user.defaultSteps().shouldSee(onMessageView().shownPictures());
    }
}
