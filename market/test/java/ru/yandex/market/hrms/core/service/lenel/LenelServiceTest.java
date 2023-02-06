package ru.yandex.market.hrms.core.service.lenel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.test.configurer.LenelApiConfigurer;

public class LenelServiceTest extends AbstractCoreTest {

    @Autowired
    private LenelService lenelService;

    @Autowired
    private LenelApiConfigurer lenelApiConfigurer;

    private final static String GET_LOGGED_EVENTS_SUCCESS = """
            {
              "Success": true,
              "Message": "Operation successful. Retreived 3 entities.",
              "Data": [
                {
                  "cardholderId": 112233,
                  "cardNumber": 111111111,
                  "lastName": "last_name1",
                  "firstName": "first_name1",
                  "midName": "mid_name1",
                  "event": "Доступ разрешен",
                  "eventDetails": "вход не осуществлен",
                  "checkpoint": "OKO 01 / 11 - 11.22 (лев. турникет выход)",
                  "timeUtc": "2021-09-18T14:34:27.0Z"
                },
                {
                  "cardholderId": 112233,
                  "cardNumber": 111111111,
                  "lastName": "last_name1",
                  "firstName": "first_name1",
                  "midName": "mid_name1",
                  "event": "Доступ разрешен",
                  "eventDetails": "Доступ разрешен",
                  "checkpoint": "OKO 01 / 11 - 11.22 (лев. турникет выход)",
                  "timeUtc": "2021-09-18T14:44:25.0Z"
                },
                {
                  "cardholderId": 445566,
                  "cardNumber": 222222222,
                  "lastName": "last_name2",
                  "firstName": "first_name2",
                  "midName": "mid_name2",
                  "event": "Доступ разрешен",
                  "eventDetails": "Доступ разрешен",
                  "checkpoint": "OKO 02 / 22 - 33.44 (лев. турникет выход)",
                  "timeUtc": "2021-09-19T14:35:27.0Z"
                }
              ]
            }
            """;

    private final static String GET_LOGGED_EVENTS_EMPTY = """
            {
              "Success": true,
              "Message": "Operation successful. Retreived 0 entities.",
              "Data": []
            }
            """;

    private void doLoadTest() {
        lenelApiConfigurer.mockGetLoggedEventsSuccess("no_data", "end", GET_LOGGED_EVENTS_EMPTY);
        lenelApiConfigurer.mockGetLoggedEventsSuccess("data_exists", "no_data", GET_LOGGED_EVENTS_SUCCESS);

        lenelService.loadHistoryFromLenel();
    }

    @Test
    @DbUnitDataSet(before = "LenelServiceTest.out.history.table.empty.csv",
            after = "LenelServiceTest.out.history.table.has.2.rows.csv")
    public void loadHistoryFromLenel_trackOutstaff_willLoadData() {

        // здесь есть единственная запись в outstaff_lenel_sync, которую нужно трекать,
        // и для нее будут загружены 2 строки истории

        doLoadTest();
    }

    @Test
    @DbUnitDataSet(before = "LenelServiceTest.emp.history.table.empty.csv",
            after = "LenelServiceTest.emp.history.table.has.1.row.csv")
    public void loadHistoryFromLenel_trackEmployee_willLoadData() {

        // здесь есть единственная запись в employee_lenel_sync, которую нужно трекать,
        // и для нее будет загружена 1 строка истории

        doLoadTest();
    }

    @Test
    @DbUnitDataSet(before = "LenelServiceTest.all.history.table.empty.csv",
            after = "LenelServiceTest.all.history.table.has.3.rows.csv")
    public void loadHistoryFromLenel_trackBoth_willLoadData() {

        // здесь есть записи в таблицах outstaff_lenel_sync и employee_lenel_sync,
        // которые нужно трекать, и для них будут загружены все 3 строки истории

        doLoadTest();
    }
}
