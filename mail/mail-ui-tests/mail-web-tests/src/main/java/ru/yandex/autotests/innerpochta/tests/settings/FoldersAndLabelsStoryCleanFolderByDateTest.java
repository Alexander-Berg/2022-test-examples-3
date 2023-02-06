package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Очистка папок по дате писем")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
@RunWith(DataProviderRunner.class)
public class FoldersAndLabelsStoryCleanFolderByDateTest extends BaseTest {

    private String subject;
    private static final String WEEK = "одной недели";
    private static final String TWO_WEEKS = "двух недель";
    private static final String MONTH = "одного месяца";
    private static final String TWO_MONTHS = "двух месяцев";
    private static String sbjTwoMonths = Utils.getRandomName();
    private static String sbjMonth = Utils.getRandomName();
    private static String sbjWeek = Utils.getRandomName();
    private static String sbjTwoWeeks = Utils.getRandomName();

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {WEEK, sbjWeek},
            {TWO_WEEKS, sbjTwoWeeks},
            {MONTH, sbjMonth},
            {TWO_MONTHS, sbjTwoMonths}
        };
    }

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
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        user.imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusMonths(3)) + "Z"))
                    .withSubject(sbjTwoMonths)
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusMonths(1).minusDays(5)) + "Z"))
                    .withSubject(sbjMonth)
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusDays(8)) + "Z"))
                    .withSubject(sbjWeek)
                    .build()
            )
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(date.minusWeeks(3)) + "Z"))
                    .withSubject(sbjTwoWeeks)
                    .build()
            )
            .closeConnection();
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Очистка папок по дате писем")
    @TestCaseId("1743")
    @UseDataProvider("testData")
    public void testMoveMessagesToTrashByDate(String date, String sbj) {
        user.defaultSteps().onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().inboxFolderCounter())
            .clicksOn(
                onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder(),
                onFoldersAndLabelsSetup().cleanFolderPopUp().advancedOptions(),
                onFoldersAndLabelsSetup().cleanFolderPopUp().msgDateSelect()
            )
            .clicksOnElementWithText(onSettingsPage().selectConditionDropdown().conditionsList(), date)
            .clicksOn(onFoldersAndLabelsSetup().cleanFolderPopUp().confirmCleaningBtn())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldNotSeeMessageWithSubject(sbj);
    }
}
