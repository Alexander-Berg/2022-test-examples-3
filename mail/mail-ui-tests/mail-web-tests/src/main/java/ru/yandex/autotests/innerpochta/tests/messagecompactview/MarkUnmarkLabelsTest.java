package ru.yandex.autotests.innerpochta.tests.messagecompactview;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на метки")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.LABELS)
public class MarkUnmarkLabelsTest extends BaseTest {

    private static final String USER_LABEL = Utils.getRandomString();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiLabelsSteps().addNewLabel(USER_LABEL, LABELS_PARAM_GREEN_COLOR);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-horizontal",
            of(SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL)
        );
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("3pane: Ставим метку на письмо из просмотра письма")
    @TestCaseId("1607")
    public void messageViewMarkWithCustomLabel() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.messagesSteps().markMessageWithCustomLabelByContentToolbar(USER_LABEL);
        user.defaultSteps().shouldContainText(onMessageView().messageLabel().get(0), USER_LABEL)
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeLabelsOnMessage(USER_LABEL, msg.getSubject());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.messagesSteps().unmarkMessageWithCustomLabelByContentToolbar(USER_LABEL);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeThatMessageIsNotLabeledWithCustomMark(msg.getSubject());
    }

    @Test
    @Title("Ставим на письмо метку «Важное» из просмотра письма")
    @TestCaseId("1608")
    public void messageViewMarkImportantLabel() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE)
        );
        user.defaultSteps().refreshPage()
            .opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .onMouseHoverAndClick(onMessageView().messageHead().labelImportance())
            .shouldSee(onMessageView().messageHead().activeLabelImportance())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeThatMessageIsImportant(msg.getSubject());
        shouldSeeImportantCounterIs(1);
    }

    @Step("Счетчик писем с меткой Важные должен быть равен «{0}»")
    public void shouldSeeImportantCounterIs(int expectedCounter) {
        assertThat(
            "Счетчик писем с меткой Важные отличен от ожидаемого",
            user.pages().MessagePage().msgFiltersBlock().showImportant().getText(),
            equalTo(Integer.toString(expectedCounter))
        );
    }
}
