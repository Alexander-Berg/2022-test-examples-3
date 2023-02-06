package ru.yandex.market.hrms.core.service.lenel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.test.configurer.LenelApiConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LenelClientTest extends AbstractCoreTest {

    @Autowired
    private LenelClient lenelClient;

    @Autowired
    private LenelApiConfigurer lenelApiConfigurer;

    private final String TEST_LAST_NAME = "laretsLastName1";
    private final String TEST_FIRST_NAME = "laretsFirstName1";
    private final String TEST_MID_NAME = "laretsMidName1";

    private final static String CREATE_CARDHOLDER_SUCCESS = """
            {
                "Success": true,
                "Message": "Added 1 object",
                "Data": {
                    "ID": 65364,
                    "SSNO": null,
                    "AllowedVisitors": false,
                    "FirstName": "laretsFirstName1",
                    "MiddleName": "laretsMidName1",
                    "Email": null,
                    "LastName": "laretsLastName1",
                    "LastChanged": "2021-09-14T20:23:35",
                    "Badges": []
                }
            }
            """;

    private final static String UPDATE_CARDHOLDER_SUCCESS = """
            {
                "Success": true,
                "Message": "Updated 1 object(s)",
                "Data": [
                    {
                        "ID": 65638,
                        "SSNO": null,
                        "AllowedVisitors": true,
                        "FirstName": "name2",
                        "MiddleName": "laretsMidName1",
                        "Email": null,
                        "LastName": "laretsLastName1",
                        "LastChanged": "2021-09-20T13:02:58",
                        "Badges": []
                    }
                ]
            }
            """;

    private final static String REMOVE_CARDHOLDER_SUCCESS = """
            {
                "Success": true,
                "Message": "Deleted 1 object(s)",
                "Data": [
                    {
                        "ID": 65363
                    }
                ]
            }
            """;

    private final static String ADD_BADGE_SUCCESS = """
            {
                "Success": true,
                "Message": "Added 1 object",
                "Data": {
                    "BADGEKEY": 90415,
                    "ID": 12345
                }
            }
            """;

    private final static String REMOVE_BADGE_SUCCESS = """
            {
                "Success": true,
                "Message": "Deleted 1 object(s)",
                "Data": [
                    {
                        "ID": 12345
                    }
                ]
            }
            """;

    private final static String GET_BADGE_BY_ID_AND_KEY_SUCCESS = """
            {
              "id": 12345,
              "badgeKey": 90758,
              "type": 1,
              "typeDesc": "Сотрудник",
              "status": 1,
              "statusDesc": "Актив.",
              "activateDate": "2021-09-17T00:00:00",
              "deactivateDate": "2031-09-17T00:00:00",
              "lastChanged": "2021-09-17T12:28:52",
              "PersonID": 65638
            }
            """;

    private final static String GET_LOGGED_EVENTS_SUCCESS = """
            {
              "Success": true,
              "Message": "Operation successful. Retreived 30 entities.",
              "Data": [
                {
                  "cardholderId": 112233,
                  "cardNumber": 123456789,
                  "lastName": "last_name1",
                  "firstName": "first_name1",
                  "midName": "mid_name1",
                  "event": "Доступ разрешен",
                  "eventDetails": "Доступ разрешен, вход не осуществлен",
                  "checkpoint": "OKO 01 / 11 - 11.22 (лев. турникет выход)",
                  "timeUtc": "2021-09-18T14:34:27.000Z"
                }
              ]
            }
            """;

    @Test
    public void createCardholder_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockCreateCardholderSuccess(CREATE_CARDHOLDER_SUCCESS);

        var response = lenelClient.createCardholder(TEST_LAST_NAME, TEST_MID_NAME, TEST_FIRST_NAME);

        assertAll(
                () -> assertNotNull(response),
                () -> assertTrue(response.isSuccess()),
                () -> assertEquals("Added 1 object", response.getMessage()),
                () -> assertEquals(65364, response.getData().getId()),
                () -> assertEquals(TEST_FIRST_NAME, response.getData().getFirstName()),
                () -> assertEquals(TEST_MID_NAME, response.getData().getMiddleName()),
                () -> assertEquals(TEST_LAST_NAME, response.getData().getLastName()),
                () -> assertFalse(response.getData().isAllowedVisitors()),
                () -> assertNull(response.getData().getEmail()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 14, 20, 23, 35),
                        response.getData().getLastChanged())
        );
    }

    @Test
    public void createCardholder_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockCreateCardholderForbidden();

        var response = lenelClient.createCardholder(TEST_LAST_NAME, TEST_MID_NAME, TEST_FIRST_NAME);

        assertAll(
                () -> assertNotNull(response),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }

    @Test
    public void updateCardholder_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockUpdateCardholderSuccess(UPDATE_CARDHOLDER_SUCCESS);
        String newFirstName = "name2";

        var response = lenelClient.updateCardholder(65638, TEST_LAST_NAME, TEST_MID_NAME, newFirstName, true);

        assertAll(
                () -> assertNotNull(response),
                () -> assertTrue(response.isSuccess()),
                () -> assertEquals("Updated 1 object(s)", response.getMessage()),
                () -> assertEquals(65638, response.getData().getId()),
                () -> assertEquals(newFirstName, response.getData().getFirstName()),
                () -> assertEquals(TEST_MID_NAME, response.getData().getMiddleName()),
                () -> assertEquals(TEST_LAST_NAME, response.getData().getLastName()),
                () -> assertTrue(response.getData().isAllowedVisitors()),
                () -> assertNull(response.getData().getEmail()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 20, 13, 2, 58),
                        response.getData().getLastChanged())
        );
    }

    @Test
    public void updateCardholder_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockUpdateCardholderForbidden();
        String newFirstName = "name2";

        var response = lenelClient.updateCardholder(65638, TEST_LAST_NAME, TEST_MID_NAME, newFirstName, true);

        assertAll(
                () -> assertNotNull(response),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }

    @Test
    public void removeCardholder_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockRemoveCardholderSuccess(REMOVE_CARDHOLDER_SUCCESS);

        var response = lenelClient.removeCardholder(65363);

        assertAll(
                () -> assertNotNull(response),
                () -> assertNotNull(response.getData()),
                () -> assertTrue(response.isSuccess()),
                () -> assertEquals("Deleted 1 object(s)", response.getMessage()),
                () -> assertEquals(65363, response.getData().getId())
        );
    }

    @Test
    public void removeCardholder_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockRemoveCardholderForbidden();

        var response = lenelClient.removeCardholder(65363);

        assertAll(
                () -> assertNotNull(response),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }

    @Test
    public void addBadgeToCardholder_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockAddBadgeToCardholderSuccess(ADD_BADGE_SUCCESS);

        var activateDate = Instant.now().minus(1, ChronoUnit.DAYS);
        var deactivateDate = Instant.now();

        var response = lenelClient.addBadgeToCardholder(65363, 12345, 1, 1, activateDate, deactivateDate);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(12345, response.getData().getId()),
                () -> assertEquals(90415, response.getData().getBadgeKey())
        );
    }

    @Test
    public void addBadgeToCardholder_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockAddBadgeToCardholderForbidden();

        var activateDate = Instant.now().minus(1, ChronoUnit.DAYS);
        var deactivateDate = Instant.now();

        var response = lenelClient.addBadgeToCardholder(65363, 12345, 1, 1, activateDate, deactivateDate);

        assertAll(
                () -> assertNotNull(response),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }

    @Test
    public void removeBadge_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockRemoveBadgeSuccess(REMOVE_BADGE_SUCCESS);

        var response = lenelClient.removeBadge(12345);

        assertAll(
                () -> assertNotNull(response),
                () -> assertNotNull(response.getData()),
                () -> assertTrue(response.isSuccess()),
                () -> assertEquals("Deleted 1 object(s)", response.getMessage()),
                () -> assertEquals(12345, response.getData().getId())
        );
    }

    @Test
    public void removeBadge_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockRemoveBadgeForbidden();

        var response = lenelClient.removeBadge(12345);

        assertAll(
                () -> assertNotNull(response),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }

    @Test
    public void getBadgeById_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockGetBadgeByIdSuccess(GET_BADGE_BY_ID_AND_KEY_SUCCESS);

        var response = lenelClient.getBadgeById(12345);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(12345, response.getId()),
                () -> assertEquals(90758, response.getBadgeKey()),
                () -> assertEquals(1, response.getType()),
                () -> assertEquals("Сотрудник", response.getTypeDesc()),
                () -> assertEquals(1, response.getStatus()),
                () -> assertEquals("Актив.", response.getStatusDesc()),
                () -> assertEquals(65638, response.getCardholderId()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 17, 0, 0, 0),
                        response.getActivateDate()),
                () -> assertEquals(LocalDateTime.of(2031, 9, 17, 0, 0, 0),
                        response.getDeactivateDate()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 17, 12, 28, 52),
                        response.getLastChanged())
        );
    }

    @Test
    public void getBadgeById_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockGetBadgeByIdForbidden();

        var response = lenelClient.getBadgeById(12345);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(0, response.getId()),
                () -> assertEquals(0, response.getBadgeKey()),
                () -> assertEquals(0, response.getStatus()),
                () -> assertEquals(0, response.getType()),
                () -> assertEquals(0, response.getCardholderId()),
                () -> assertNull(response.getTypeDesc()),
                () -> assertNull(response.getStatusDesc()),
                () -> assertNull(response.getActivateDate()),
                () -> assertNull(response.getDeactivateDate()),
                () -> assertNull(response.getLastChanged())
        );
    }

    @Test
    public void getBadgeByKey_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockGetBadgeByKeySuccess(GET_BADGE_BY_ID_AND_KEY_SUCCESS);

        var response = lenelClient.getBadgeByKey(90758);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(12345, response.getId()),
                () -> assertEquals(90758, response.getBadgeKey()),
                () -> assertEquals(1, response.getType()),
                () -> assertEquals("Сотрудник", response.getTypeDesc()),
                () -> assertEquals(1, response.getStatus()),
                () -> assertEquals("Актив.", response.getStatusDesc()),
                () -> assertEquals(65638, response.getCardholderId()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 17, 0, 0, 0),
                        response.getActivateDate()),
                () -> assertEquals(LocalDateTime.of(2031, 9, 17, 0, 0, 0),
                        response.getDeactivateDate()),
                () -> assertEquals(LocalDateTime.of(2021, 9, 17, 12, 28, 52),
                        response.getLastChanged())
        );
    }

    @Test
    public void getBadgeByKey_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockGetBadgeByKeyForbidden();

        var response = lenelClient.getBadgeByKey(90758);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(0, response.getId()),
                () -> assertEquals(0, response.getBadgeKey()),
                () -> assertEquals(0, response.getStatus()),
                () -> assertEquals(0, response.getType()),
                () -> assertEquals(0, response.getCardholderId()),
                () -> assertNull(response.getTypeDesc()),
                () -> assertNull(response.getStatusDesc()),
                () -> assertNull(response.getActivateDate()),
                () -> assertNull(response.getDeactivateDate()),
                () -> assertNull(response.getLastChanged())
        );
    }

    @Test
    public void getLoggedEvents_validInput_returnsSuccessResult() {
        lenelApiConfigurer.mockGetLoggedEventsSuccess("scenario1", "end", GET_LOGGED_EVENTS_SUCCESS);

        var response = lenelClient.getLoggedEvents(1631964865, 1);

        assertAll(
                () -> assertNotNull(response),
                () -> assertNotNull(response.getData()),
                () -> assertTrue(response.isSuccess()),
                () -> assertEquals("Operation successful. Retreived 30 entities.", response.getMessage()),
                () -> assertEquals(1, response.getData().length),
                () -> assertEquals(112233, response.getData()[0].getCardholderId()),
                () -> assertEquals(123456789, response.getData()[0].getCardNumber()),
                () -> assertEquals("last_name1", response.getData()[0].getLastName()),
                () -> assertEquals("first_name1", response.getData()[0].getFirstName()),
                () -> assertEquals("mid_name1", response.getData()[0].getMidName()),
                () -> assertEquals("Доступ разрешен", response.getData()[0].getEvent()),
                () -> assertEquals("Доступ разрешен, вход не осуществлен", response.getData()[0].getEventDetails()),
                () -> assertEquals("OKO 01 / 11 - 11.22 (лев. турникет выход)", response.getData()[0].getCheckpoint()),
                () -> assertEquals(Instant.parse("2021-09-18T14:34:27Z"), response.getData()[0].getTimeUtc())
        );
    }

    @Test
    public void getLoggedEvents_validInput_returnsFailureResult() {
        lenelApiConfigurer.mockGetLoggedEventsForbidden();

        var response = lenelClient.getLoggedEvents(1631964865, 1);

        assertAll(
                () -> assertNotNull(response),
                () -> assertFalse(response.isSuccess()),
                () -> assertThat(response.getMessage(), containsString("returned http code 403")),
                () -> assertNull(response.getData())
        );
    }
}
