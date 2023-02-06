package ru.yandex.autotests.innerpochta.tests.messageslist;

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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Тест управления рассылками при мультиавторизации")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MULTI_AUTH)
@Description("Юзеры подписаны на разные рассылки, ровно одна рассылка у каждого")
public class MultiAuthSubscriptionTest extends BaseTest {

    private static final String FIRST_ACC = "MultiAuthSubscriptionTest";
    private static final String SECOND_ACC = "MultiAuthSubscriptionTest2";

    private static final String FIRST_ACC_SUBSCRIPTION = "Skyeng";
    private static final String SECOND_ACC_SUBSCRIPTION = "Cossa.ru";

    private AccLockRule lock = AccLockRule.use().names(FIRST_ACC, SECOND_ACC);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(FIRST_ACC)).logins().multiLoginWith(lock.acc(SECOND_ACC));
    }

    @Test
    @Title("Рассылки должны соответствовать аккаунту пользователя")
    @TestCaseId("5088")
    public void shouldSeeAppropriateSubscriptions() {
        user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().switchOnWindow(0);
        user.loginSteps().changeUserFromShapka(lock.acc(FIRST_ACC).getLogin());
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().shouldSeeElementsCount(user.pages().SubscriptionsSettingsPage().subscriptions(), 2)
            .shouldSeeThatElementHasText(
                user.pages().SubscriptionsSettingsPage().subscriptions().get(0),
                FIRST_ACC_SUBSCRIPTION
            ).switchOnWindow(1);
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().shouldSeeElementsCount(user.pages().SubscriptionsSettingsPage().subscriptions(), 1)
            .shouldSeeThatElementHasText(
                user.pages().SubscriptionsSettingsPage().subscriptions().get(0),
                SECOND_ACC_SUBSCRIPTION
            );
    }
}
