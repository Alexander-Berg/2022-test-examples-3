package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
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
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static ru.lanwen.diff.uri.core.filters.SchemeFilter.scheme;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getUserUid;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created by cosmopanda
 */
@Aqua.Test
@Title("Открыть письмо в списке писем")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryOpenMsgInListTest extends BaseTest {
    private Message msg;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams("Включаем 2pane", of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE))
            .callWithListAndParams(
                "Выключаем просмотр письма в списке писем",
                of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
            );
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Проверяем включение настройки просмотра письма в списке")
    @TestCaseId("2170")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66332")
    public void shouldSeeMsgInList() {
        user.defaultSteps().turnTrue(onOtherSettings().blockSetupOther().topPanel().openMsgInList())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.INBOX)
            .shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().shouldBeOnUrlNotDiffWith(
            buildCheckUrl(QuickFragments.INBOX_THREAD, "t"),
            scheme("http", "https")
        )
            .shouldSee(onMessageView().contentToolbarBlock());
    }

    @Test
    @Title("Проверяем выключение настройки просмотра письма в списке")
    @TestCaseId("2162")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66332")
    public void shouldNotSeeMsgInList() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .deselects(onOtherSettings().blockSetupOther().topPanel().openMsgInList())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.INBOX)
            .shouldSee(onMessagePage().displayedMessages());
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().shouldBeOnUrlNotDiffWith(
            buildCheckUrl(QuickFragments.MESSAGE, ""),
            scheme("http", "https")
        )
            .shouldNotSee(onMessageView().contentToolbarBlock());
    }

    @Test
    @Title("Проверяем, что в 3pane нет настройки")
    @TestCaseId("2171")
    public void shouldNotSeeSettingsMsgView() {
        user.apiSettingsSteps()
            .callWithListAndParams("Включаем 3pane", of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        user.defaultSteps().refreshPage()
            .shouldSee(onOtherSettings().blockSetupOther())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().openMsgInList());
    }

    private String buildCheckUrl(QuickFragments fragment, String thread) {
        return webDriverRule.getBaseUrl() + "/" +
            format("?uid=%s", getUserUid(lock.firstAcc().getLogin())) +
            fragment.makeUrlPart() + "/" + thread +
            msg.getMid();
    }
}
