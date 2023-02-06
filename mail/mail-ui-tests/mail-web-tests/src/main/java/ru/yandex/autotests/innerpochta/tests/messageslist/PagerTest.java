package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;


/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Тест на Пэйджер")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.PAGER)
public class PagerTest extends BaseTest {

    private static final String PAGER_URL_POSTFIX = "/#inbox?datePager=";

    private Map<String, String> PAGER_MONTHS = new HashMap<String, String>() {{
        put("JANUARY", "янв");
        put("FEBRUARY", "фев");
        put("MARCH", "мар");
        put("APRIL", "апр");
        put("MAY", "мая");
        put("JUNE", "июн");
        put("JULY", "июл");
        put("AUGUST", "авг");
        put("SEPTEMBER", "сен");
        put("OCTOBER", "окт");
        put("NOVEMBER", "ноя");
        put("DECEMBER", "дек");
    }};

    private String sbj1 = Utils.getRandomName();
    private String sbj2 = Utils.getRandomName();

    private LocalDateTime now = LocalDateTime.now();
    private String prevYear = String.valueOf(now.getYear() - 1);
    private String lastTwoDigitsOfPrevYear = prevYear.substring(2);
    private String prevMonth = String.valueOf(now.minusMonths(1).getMonth());
    private String curYear = String.valueOf(now.getYear());
    private String curMonth = String.valueOf(now.getMonth());

    private String prevYearDatePattern = "[0-9][0-9]\\.[0-9][0-9]\\." + lastTwoDigitsOfPrevYear;
    private String prevMonthDatePattern = "[0-9]{1,2} " + PAGER_MONTHS.get(prevMonth);

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        user.imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusYears(1)) + "Z"))
                    .withSubject(sbj2)
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusMonths(1)) + "Z"))
                    .withSubject(sbj1)
                    .build()
            )
            .closeConnection();
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 5);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переход на предыдущий год")
    @TestCaseId("1553")
    public void testPagerPreviousYear() {
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .onMouseHover(onMessagePage().inboxPager().scrollLeft())
            .onMouseHoverAndClick(onMessagePage().inboxPager().scrollLeft())
            .clicksOn(onMessagePage().inboxPager().prevYear());
        user.messagesSteps().shouldSeeMessageWithSubject(sbj2);
        user.messagesSteps().shouldSeeMessagesWithDatePattern(prevYearDatePattern);
    }

    @Test
    @Title("Переход на предыдущий месяц")
    @TestCaseId("1554")
    public void testPagerPreviousMonth() {
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .clicksOn(
                onMessagePage().inboxPager().months().waitUntil(not(empty()))
                    .get(onMessagePage().inboxPager().months().size() - 2)
            );
        user.messagesSteps().shouldSeeMessageWithSubject(sbj1);
        if (Objects.equals(curMonth, "JANUARY")) {
            user.messagesSteps().shouldSeeMessagesWithDatePattern(prevYearDatePattern);
        } else user.messagesSteps().shouldSeeMessagesWithDatePattern(prevMonthDatePattern);
    }

    @Test
    @Title("Переход на предыдущий год в «залипающем» пэйджере")
    @TestCaseId("1555")
    public void testStickyPagerPreviousYear() {
        user.defaultSteps().setsWindowSize(1300, 400);
        user.defaultSteps().opensDefaultUrlWithPostFix(PAGER_URL_POSTFIX + curYear)
            .clicksOn(onMessagePage().inboxStickyPager().prevYear());
        user.messagesSteps().shouldSeeMessageWithSubject(sbj2);
        user.messagesSteps().shouldSeeMessagesWithDatePattern(prevYearDatePattern);
    }

    @Test
    @Title("Переход на прошлый месяц в «залипающем» пэйджере")
    @TestCaseId("1556")
    public void testStickyPagerPreviousMonth() {
        user.defaultSteps().setsWindowSize(1300, 400);
        user.defaultSteps().opensDefaultUrlWithPostFix(PAGER_URL_POSTFIX + curYear)
            .clicksOn(onMessagePage().inboxStickyPager().months().waitUntil(Matchers.not(empty()))
                .get(onMessagePage().inboxPager().months().size() - 2));
        user.messagesSteps().shouldSeeMessageWithSubject(sbj1);
        if (Objects.equals(curMonth, "JANUARY")) {
            user.messagesSteps().shouldSeeMessagesWithDatePattern(prevYearDatePattern);
        } else user.messagesSteps().shouldSeeMessagesWithDatePattern(prevMonthDatePattern);

    }
}
