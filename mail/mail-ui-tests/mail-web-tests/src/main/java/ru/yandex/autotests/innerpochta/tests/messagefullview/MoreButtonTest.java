package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест выпадушку кнопки «Еще» в тулбаре")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class MoreButtonTest extends BaseTest {

    private static final String URL_MSG_SOURCE = "/message-source/";
    private static final String URL_MSG_EML = "/yandex_email.eml";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().toolbar().moreBtn());
    }

    @Test
    @Title("Проверяем раскрытие выпадушки")
    @TestCaseId("2163")
    public void shouldSeeMoreBlock() {
        user.defaultSteps().shouldSee(
            onMessageView().moreBlock(),
            onMessageView().moreBlock().createFilter(),
            onMessageView().moreBlock().messageInfo(),
            onMessageView().moreBlock().translateBtn(),
            onMessageView().moreBlock().messageInfo()
        );
    }

    @Test
    @Title("Проверка кнопки «Создать правило»")
    @TestCaseId("2178")
    public void shouldCreateFilterFromMoreToolbar() {
        user.defaultSteps().shouldSee(onMessageView().moreBlock())
            .clicksOn(onMessageView().moreBlock().createFilter())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_FILTERS_CREATE, "message=" + msg.getMid())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(1).inputCondition(), msg.getSubject());
    }

    @Test
    @Title("Проверка кнопки «Свойства письма»")
    @TestCaseId("2179")
    public void shouldSeeMessageInfo() {
        user.defaultSteps().clicksOn(onMessageView().moreBlock().messageInfo())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(URL_MSG_SOURCE))
            .shouldBeOnUrl(containsString(URL_MSG_EML));
    }

    @Test
    @Title("Проверка кнопки «Перевести»")
    @TestCaseId("2180")
    public void shouldSeeTranslateNotification() {
        user.defaultSteps().clicksOn(onMessageView().moreBlock().translateBtn())
            .shouldSee(onMessageView().translateNotification());
    }
}
