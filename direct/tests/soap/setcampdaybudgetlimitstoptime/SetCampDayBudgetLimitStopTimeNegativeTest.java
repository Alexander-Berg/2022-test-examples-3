package ru.yandex.autotests.directintapi.tests.soap.setcampdaybudgetlimitstoptime;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1251
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_CAMP_DAY_BUDGET_LIMIT_STOP_TIME)
public class SetCampDayBudgetLimitStopTimeNegativeTest {
    protected static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(SetCampDayBudgetLimitStopTimeNegativeTest.class);

    private static final String ZERO_TIME = "0000-00-00 00:00:00";
    private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static Long campaignId;
    private static int orderID;

    private DateTime dateTime;
    private Map<Integer, String> request;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(campaignId);

        dateTime = DateTime.now();
        request = new HashMap<>();
    }


    //https://jira.yandex-team.ru/browse/DIRECT-26225
    @Test
    public void setCampDayBudgetLimitStopTimeWithZeroOrderID() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime c нулевым orderID");
        int zeroOrderID = 0;
        String errorMessage = String.format("Bad OrderID '%d'", zeroOrderID);
        request.put(zeroOrderID, formatter.print(dateTime));
        request.put(orderID, formatter.print(dateTime));
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTimeExpectedError(request, startsWith(errorMessage));

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(ZERO_TIME));
    }

    @Test
    public void setCampDayBudgetLimitStopTimeWithNegativeOrderID() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime c отрицательным orderID");
        int negativeOrderID = -10;
        String errorMessage = String.format("Bad OrderID '%d'", negativeOrderID);
        request.put(negativeOrderID, formatter.print(dateTime));
        request.put(orderID, formatter.print(dateTime));
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTimeExpectedError(request, startsWith(errorMessage));

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(ZERO_TIME));
    }

    @Test
    public void setCampDayBudgetLimitStopTimeWithInvalidStopTime() {
        log.info("вызываем метод setCampDayBudgetLimitStopTime c неверным stopTime");
        String invalidTime = "1234";
        String errorMessage = String.format("Bad stop time '%s'", invalidTime);
        request.put(orderID, invalidTime);
        darkSideSteps.getSoapClientSteps().setCampDayBudgetLimitStopTimeExpectedError(request, startsWith(errorMessage));

        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("Неверное значения для поля DayBudgetStopTime",
                campaignFakeInfo.getDayBudgetStopTime(), equalTo(ZERO_TIME));
    }

}
