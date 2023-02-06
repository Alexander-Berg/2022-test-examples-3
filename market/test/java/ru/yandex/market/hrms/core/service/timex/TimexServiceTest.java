package ru.yandex.market.hrms.core.service.timex;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

import lombok.RequiredArgsConstructor;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Area;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.DirectedArea;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EAreaDirectionType;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EnterAreaEvent;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.WorkingArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

@RequiredArgsConstructor
public class TimexServiceTest extends AbstractCoreTest {

    public static Area AREA_MAIN = new Area("", "", false, "ФФЦ Софьино", "", null, null);
    public static Area AREA1 = new Area("", "", false, "area1", "", null, null);
    public static Area AREA2 = new Area("", "", false, "area2", "", null, null);

    public static WorkingArea WORKING_AREA = new WorkingArea("", new DirectedArea[]
            {
                    new DirectedArea(AREA1, EAreaDirectionType.fromValue("Exit"), false, ""),
                    new DirectedArea(AREA2, EAreaDirectionType.fromValue("Enter"), false, ""),
            },
            "", false, "Софьино Тест", "");

    public static Employee EMPLOYEE = new Employee(WORKING_AREA, "", null, AREA_MAIN, null, "", null, "", "", "", "",
            "", "",
            null, null, "", null, "", null, false, "", false, false, null, "Тестовый", null, "Тестович",
            "", "Тест", "", "!test_oid!", null, null, null, "", null, "", "", null, false, "", "");

    public static GregorianCalendar TIME_UTC = GregorianCalendar.from(ZonedDateTime.of(2020, 10, 11, 4, 5, 6, 0,
            ZoneId.of("UTC")));

    public static EnterAreaEvent[] TEST_EVENTS = new EnterAreaEvent[]
            {
                    new EnterAreaEvent(0f, null, "Софьино Тест", EMPLOYEE,
                            null, false, null, false, "", null, null, TIME_UTC, "", null, null, "Enter area", null,
                            null)
            };

    @Autowired
    private TimexServiceMock timexService;

    @Autowired
    private ApplicationContext context;

    @Test
    @DbUnitDataSet(before = "TimexServiceTest.destTableIsEmpty.csv", after = "TimexServiceTest.1rowInserted.csv")
    public void shouldLoadTimexLogsWhenDestTableIsEmpty() {
        var fakeTimexApiFacade = context.getBean(FakeTimexApiFacade.class);
        fakeTimexApiFacade.clearCounters();

        timexService.loadHistoryFromTimex();

        Assertions.assertEquals(1, fakeTimexApiFacade.getLogonUserCalled());
        Assertions.assertEquals(2, fakeTimexApiFacade.getGetPackEnterAreaEventsCalled());
        Assertions.assertEquals(1, fakeTimexApiFacade.getLogoutUserCalled());
    }
}
