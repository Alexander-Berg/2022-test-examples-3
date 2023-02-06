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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PRIORITY_TAB;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на полоску опт-ина в списке писем")
@Features({FeaturesConst.OPTIN, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@Description("У юзера есть наразобранные новые рассылки")
public class OptInLineTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем табы",
            of(
                FOLDER_TABS, FALSE,
                PRIORITY_TAB, FALSE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Должны кликнуть в полоску")
    @TestCaseId("6319")
    public void shouldClickOnLine() {
        user.defaultSteps().offsetClick(onMessagePage().optInLine(), 10, 10)
            .shouldNotSee(onMessagePage().optInLine())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().optinDisableBtn());
    }

    @Test
    @Title("Должны кликнуть в крестик")
    @TestCaseId("6320")
    public void shouldClickOnCross() {
        user.defaultSteps().shouldSee(onMessagePage().optInLine())
            .clicksOn(onMessagePage().optInLine().closeBtn())
            .shouldNotSee(
                onMessagePage().optInLine(),
                onUnsubscribePopupPage().subscriptionIframe()
            );
    }

    @Test
    @Title("Должны кликнуть на кнопку «Позже»")
    @TestCaseId("6321")
    public void shouldClickOnLater() {
        user.defaultSteps().clicksOn(onMessagePage().optInLine().laterBtn())
            .shouldNotSee(
                onMessagePage().optInLine(),
                onUnsubscribePopupPage().subscriptionIframe()
            );
    }

    @Test
    @Title("Должны кликнуть на кнопку «Разобрать»")
    @TestCaseId("6322")
    public void shouldClickOnSort() {
        user.defaultSteps().clicksOn(onMessagePage().optInLine().sortBtn())
            .shouldNotSee(onMessagePage().optInLine())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().optinDisableBtn());
    }

    @Test
    @Title("Должны увидеть только в табах и входящих")
    @TestCaseId("6323")
    public void shouldSeeInInboxOnly() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(
                FOLDER_TABS, TRUE,
                PRIORITY_TAB, FALSE
            )
        );
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().optInLine())
            .clicksOn(onMessagePage().newsTab())
            .shouldSee(onMessagePage().optInLine())
            .clicksOn(onMessagePage().attachmentsTab())
            .shouldNotSee(onMessagePage().optInLine());
    }

    @Test
    @Title("Должны увидеть полоску после рефреша")
    @TestCaseId("6325")
    public void shouldSeeAfterRefresh() {
        user.defaultSteps().shouldSee(onMessagePage().optInLine())
            .clicksOn(onMessagePage().optInLine().closeBtn())
            .shouldNotSee(onMessagePage().optInLine())
            .refreshPage()
            .shouldSee(onMessagePage().optInLine());
    }
}
