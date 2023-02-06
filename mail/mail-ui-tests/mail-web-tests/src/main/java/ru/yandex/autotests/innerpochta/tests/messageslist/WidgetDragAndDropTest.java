package ru.yandex.autotests.innerpochta.tests.messageslist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DND;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SHOW_WIDGETS_DECOR;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Тест на драг-н-дроп писем с виджетами")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@Description("Юзеру каждый день приходят письма с виджетами")
public class WidgetDragAndDropTest extends BaseTest {

    private static final String HOLEY_WIDGET_SUBJ_SUBSTR = "Your Booking Details";
    private static final String PLAIN_WIDGET_SUBJ_SUBSTR = "Your Amazon.com order of";

    private static final String FOLDER_WITH_HOLEY_WIDGETS = "tomorrow";
    private static final String FOLDER_WITH_PLAIN_WIDGETS = "Покупки";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем выделение цветом билетов/броней гостиниц, покупок и драг-н-дроп",
            of(
                SETTINGS_SHOW_WIDGETS_DECOR, TRUE,
                SETTINGS_DND, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Драг-н-дроп письма-виджета с дырявым корешком")
    @TestCaseId("2619")
    public void shouldDragAndDropHoleyWidget() {
        user.leftColumnSteps().opensCustomFolder(FOLDER_WITH_HOLEY_WIDGETS);
        MessageBlock msgBlock = getMostRecentMessageBySubject(HOLEY_WIDGET_SUBJ_SUBSTR);
        user.defaultSteps().dragAndDrop(msgBlock, user.pages().MessagePage().toolbar().forwardButton());
        user.composeSteps().shouldSeeSubject("Fwd: " + msgBlock.subject().getText());
    }

    @Test
    @Title("Драг-н-дроп письма-виджета с прямым корешком")
    @TestCaseId("2619")
    public void shouldDragAndDropPlainWidget() {
        user.leftColumnSteps().opensCustomFolder(FOLDER_WITH_PLAIN_WIDGETS);
        MessageBlock msgBlock = getMostRecentMessageBySubject(PLAIN_WIDGET_SUBJ_SUBSTR);
        String fullSubject = msgBlock.subject().getText();
        user.defaultSteps().dragAndDrop(msgBlock, user.pages().MessagePage().toolbar().forwardButton());
        user.composeSteps().shouldSeeSubject("Fwd: " + fullSubject);
    }

    @Step("Получаем последнее письмо с темой «{0}»")
    private MessageBlock getMostRecentMessageBySubject(String subject) {
        return user.pages().MessagePage().displayedMessages().list().filter(
            msg -> msg.subject().getText().contains(subject)
        ).get(0);
    }
}
