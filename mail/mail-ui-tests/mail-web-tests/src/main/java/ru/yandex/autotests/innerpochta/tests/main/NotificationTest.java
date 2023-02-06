package ru.yandex.autotests.innerpochta.tests.main;

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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * Created by cosmopanda
 */
@Aqua.Test
@Title("Тест на нотификации")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.NOTIFICATION)
public class NotificationTest extends BaseTest {

    private static final String EVENT_NAME = "Началась «Поговорить»";
    private static final String EVENT_PLACE = "Камчатка";
    private static final String NAME_SENDER = "Default-Имя Default Фамилия";
    private static final String URL_NOTIFICATION = "?current_folder=1&fromNotification=1";

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
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Показ и закрытие уведомления о встрече")
    @TestCaseId("2204")
    public void shouldSeeAndCloseMeetingNotify() {
        user.settingsSteps().createMeeting();
        user.defaultSteps().shouldSee(onMessagePage().notificationEventBlock())
            .onMouseHover(onMessagePage().notificationEventBlock())
            .shouldHasText(onMessagePage().notificationEventBlock().titleEvent(), EVENT_NAME)
            .shouldHasText(onMessagePage().notificationEventBlock().placeEvent(), EVENT_PLACE)
            .clicksOn(onMessagePage().notificationEventBlock().closeEvent())
            .shouldNotSee(onMessagePage().notificationEventBlock());
    }

    @Test
    @Title("Уведомление о новом сообщении")
    @TestCaseId("2199")
    public void shouldSeeMessageNotify() {
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS);
        String sbj = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "").getSubject();
        user.defaultSteps().shouldSeeWithWaiting(onMessagePage().notificationEventBlock(), 20)
            .onMouseHover(onMessagePage().notificationEventBlock())
            .shouldHasText(onMessagePage().notificationEventBlock().nameSender(), NAME_SENDER)
            .clicksOn(onMessagePage().notificationEventBlock().nameSender())
            .shouldBeOnUrl(containsString(URL_NOTIFICATION));
        user.messageViewSteps().shouldSeeMessageSubject(sbj);
    }

    @Test
    @Title("Нет нотифайки в инбоксе и композе")
    @TestCaseId("2302")
    public void shouldNotSeeMessageNotify() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomName());
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldNotSee(onMessagePage().notificationEventBlock())
            .opensFragment(QuickFragments.COMPOSE);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomName());
        user.defaultSteps().shouldNotSee(onMessagePage().notificationEventBlock());
    }
}
