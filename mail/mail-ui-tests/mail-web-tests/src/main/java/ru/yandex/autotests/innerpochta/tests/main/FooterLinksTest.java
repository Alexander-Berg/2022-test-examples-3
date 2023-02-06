package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.LITE_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.MORDA_URL;

/**
 * Created by mabelpines
 */
@Aqua.Test
@Title("Тест на ссылки в футере")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.GENERAL)
public class FooterLinksTest extends BaseTest {

    private static final String SUPPORT_URL = "https://yandex.ru/support/mail/";
    private static final String ANDROID_URL = "https://mobile.yandex.ru/apps/android/mail";
    private static final String APPLE_URL = "https://mobile.yandex.ru/apps/iphone/mail";
    private static final String ADVERTISE_URL = "https://yandex.ru/adv";

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
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Проверяем ссылку на морду")
    @TestCaseId("1503")
    public void testMordaLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().mordaLink())
            .switchOnJustOpenedWindow()
            .shouldNotSee(onMessagePage().footerLineBlock().mordaLink())
            .shouldBeOnUrl(MORDA_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ссылку на андроид")
    @TestCaseId("1503")
    public void testAndroidLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().androidLink())
            .switchOnJustOpenedWindow()
            .shouldNotSee(onMessagePage().footerLineBlock().androidLink())
            .shouldBeOnUrl(ANDROID_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ссылку на apple")
    @TestCaseId("1503")
    public void testAppleLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().appleLink())
            .switchOnJustOpenedWindow()
            .shouldNotSee(onMessagePage().footerLineBlock().appleLink())
            .shouldBeOnUrl(APPLE_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ссылку на страничку с рекламой")
    @TestCaseId("1503")
    public void testAdvertsingLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().advertisingLink())
            .shouldNotSee(onMessagePage().footerLineBlock().advertisingLink())
            .shouldBeOnUrl(ADVERTISE_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ссылку на страничку с журналом")
    @TestCaseId("3632")
    public void testJournalLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().journalLink())
            .shouldNotSee(onMessagePage().footerLineBlock().journalLink())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_JOURNAL)
            .shouldSee(
                onJournalSettingsPage().journalBlock().journalDescription(),
                onJournalSettingsPage().journalBlock().journalIp(),
                onJournalSettingsPage().journalBlock().journalLog()
            );
    }

    @Test
    @Title("Проверяем запись о входе в почту в журнале")
    @TestCaseId("3632")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60502")
    public void shouldSeeLastLoginEntryInJournal() {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM. YYYY"));
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("H:mm"));

        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().journalLink())
            .waitInSeconds(10) //ждём пока свежая запись о входе появится в журнале
            .refreshPage()
            .shouldSeeThatElementTextEquals(
                onJournalSettingsPage().journalBlock().journalLogItem().get(0).journalLogItemProtocol(),
                "Вход в Почту"
            )
            .shouldSeeThatElementTextEquals(
                onJournalSettingsPage().journalBlock().journalLogItem().get(0).journalLogItemDayname(),
                currentDate
            );
        shouldBeEqualTime(
            onJournalSettingsPage().journalBlock().journalLogItem().get(0).journalLogItemCell(),
            currentTime
        );
    }

    @Test
    @Title("Проверяем ссылку на страничку с помощью")
    @TestCaseId("1503")
    public void testSupportLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().helpBtn())
            .shouldNotSee(onMessagePage().footerLineBlock().helpBtn())
            .shouldBeOnUrl(SUPPORT_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ссылку на Lite-версию")
    @TestCaseId("1503")
    public void testLiteLink() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().liteMailLink())
            .shouldNotSee(onMessagePage().footerLineBlock().liteMailLink())
            .shouldBeOnUrl(containsString(LITE_URL));
    }

    @Step("Время в журнале должно отличаться не более чем на минуту от текущего")
    private void shouldBeEqualTime(MailElement cell, String current) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
        LocalTime currentTime = LocalTime.parse(current, formatter);
        LocalTime cellTime = LocalTime.parse(cell.getText(), formatter);
        int DIFF_IN_MINS = 1;

        assertThat(
            "Текущее время меньше чем проверяемое",
            (currentTime.getMinute() - cellTime.getMinute()) <= DIFF_IN_MINS
        );
    }

}
