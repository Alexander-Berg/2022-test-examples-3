package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Дата и время в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class MessageListTimeAndDateTest extends BaseTest {

    private static final String SEARCH_URL_POSTFIX = "/#search?request=";
    private static final String FULL_DATE_FMT = "dd.MM.yy";
    private static final String DAY_MONTH_FMT = "dd MMM";
    private static final String TIME_REGEX = "\\d{2}:\\d{2}";
    private static final String DAY_MONTH_REGEX = "\\d{1,2}\\s[а-я]{3}";
    private static final String FULL_DATE_REGEX = "(\\d{2}\\.){2}\\d{2}";
    private static final String[] MONTH_VALUES = {
        "янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"
    };
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    private static Calendar parseDate(String date) throws ParseException {
        Calendar parsedDate = Calendar.getInstance();
        if (date.contains(".")) {
            parsedDate.setTime(new SimpleDateFormat(FULL_DATE_FMT).parse(date));
        } else if (date.contains(" ")) {
            DateFormatSymbols symbols = DateFormatSymbols.getInstance(new Locale("ru"));
            symbols.setMonths(MONTH_VALUES);
            SimpleDateFormat format = new SimpleDateFormat(DAY_MONTH_FMT);
            format.setDateFormatSymbols(symbols);
            parsedDate.setTime(format.parse(date));
            parsedDate.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }
        return parsedDate;
    }

    @Before
    public void logIn() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        user.imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.withDayOfYear(1)) + "Z"))
                    .withSubject(Utils.getRandomName())
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusYears(1)) + "Z"))
                    .withSubject(Utils.getRandomName())
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.withHour(0).withMinute(1)) + "Z"))
                    .withSubject(Utils.getRandomName())
                    .build()
            )
            .closeConnection();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX);
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Письма отсортированы в хронологическом порядке")
    @TestCaseId("1962")
    public void shouldSeeMessagesInChronologicalOrder() {
        List<MessageBlock> messages = onMessagePage().displayedMessages().list();
        try {
            for (int i = 1; i < messages.size(); ++i) {
                assertTrue(
                    "Порядок писем неправильный",
                    compare(messages.get(i - 1).date().getText(), messages.get(i).date().getText()) >= 0
                );
            }
        } catch (ParseException e) {
            fail("Некорректный формат даты: " + e.getMessage());
        }
    }

    @Test
    @Title("У сегодняшнего письма отображается время, у остальных -- дата")
    @TestCaseId("1962")
    public void shouldSeeCorrectTimeDateFormat() {
        assertTrue(
            "У сегодняшнего письма должно отображаться время",
            onMessagePage().displayedMessages().list().get(0).date().getText().matches(TIME_REGEX)
        );
        assertTrue(
            "У письма этого года должны отображаться день и месяц, если сегодня не 1 января",
            (LocalDate.now().getMonthValue() == 1 && LocalDate.now().getDayOfMonth() == 1) ||
                onMessagePage().displayedMessages().list().get(1).date().getText().matches(DAY_MONTH_REGEX)
        );
        assertTrue(
            "У письма не этого года должна отображаться полная дата",
            onMessagePage().displayedMessages().list().get(2).date().getText().matches(FULL_DATE_REGEX)
        );
    }

    private int compare(String first, String second) throws ParseException {
        Calendar firstDate = parseDate(first);
        Calendar secondDate = parseDate(second);
        return firstDate.compareTo(secondDate);
    }
}
