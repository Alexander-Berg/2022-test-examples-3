package ru.yandex.market.ff4shops.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Проверяем {@link  SendNotificationCommand}
 */
@DbUnitDataSet(before = "sendNotificationCommand.before.csv")
class SendNotificationCommandTest extends AbstractTmsCommandTest {

    private static final String RESPONSE_XML =/*language=xml*/ "" +
            "<send-notification-response>" +
            "   <notification-group-id>" +
            "       42" +
            "   </notification-group-id>" +
            "</send-notification-response>";

    @Autowired
    @Value("${mbi.api.url}")
    protected String mbiApiUrl;
    @Autowired
    private SendNotificationCommand sendNotificationCommand;
    private MockRestServiceServer mockRestServiceServer;
    @Autowired
    @Qualifier("mbiApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    @BeforeEach
    void initMock() {
        mockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
    }

    @Test
    @DisplayName("Попытка вызвать действие, которого нет")
    void testNoSuchModeCommand() {
        Assertions.assertEquals("Unknown mode: null\n" +
                "Usage:\n" +
                "    send-notification <BY_PARTNER partner_id|BY_STATUS status|BY_ID notification_id>",
                executeCommand("BY_SOMETHING", "wrong"));
    }

    @Test
    @DisplayName("Попытка вызвать действие без нужных аргументов")
    void testWrongVariableCommand() {
        Assertions.assertEquals("Usage:\n" +
                        "    send-notification <BY_PARTNER partner_id|BY_STATUS status|BY_ID notification_id>",
                executeCommand("BY_SOMETHING"));
    }

    @Test
    @DisplayName("Удачная отправка по ид нотификации")
    @DbUnitDataSet(after = "sendNotificationCommand.after.csv")
    void testByIdCommand() {
        mockSuccessNotification(1);
        String executeCommand = executeCommand("BY_ID", "3764823");
        Assertions.assertEquals("Send notification: OrderNotificationModel{id=3764823, shopId=1001, " +
                "notificationTypeId=1643556238, orderId=187898235, orderStatusId=3, orderNotificationStatus=SENT," +
                " paramsXml='bla bla1'}", executeCommand);
    }

    @Test
    @DisplayName("Не удачная отправка по ид нотификации")
    @DbUnitDataSet(after = "sendNotificationCommand.before.csv")
    void testByIdFailCommand() {
        mockFailNotification(1);
        String executeCommand = executeCommand("BY_ID", "3764823");
        Assertions.assertEquals("Couldn't send notification by id 3764823", executeCommand);
    }

    @Test
    @DisplayName("Удачная отправка по ид партнера")
    @DbUnitDataSet(after = "sendNotificationCommand.after.csv")
    void testByPartnerIdCommand() {
        mockSuccessNotification(2);
        String executeCommand = executeCommand("BY_PARTNER", "1001");
        Assertions.assertEquals("Send by partner id notifications count = 1", executeCommand);
    }

    @Test
    @DisplayName("Не удачная отправка по ид партнера")
    @DbUnitDataSet(after = "sendNotificationCommand.before.csv")
    void testByPartnerIdFailCommand() {
        mockFailNotification(2);
        String executeCommand = executeCommand("BY_PARTNER", "1001");
        Assertions.assertEquals("Send by partner id notifications count = 0", executeCommand);
    }


    @Test
    @DisplayName("Удачная отправка по ид статусу")
    @DbUnitDataSet(after = "sendNotificationByStatusCommand.after.csv")
    void testByStatusCommand() {
        mockSuccessNotification(2);
        String executeCommand = executeCommand("BY_STATUS", "NOT_SENT");
        Assertions.assertEquals("Send by status notifications count = 2", executeCommand);
    }

    @Test
    @DisplayName("Не удачная отправка по статусу")
    @DbUnitDataSet(after = "sendNotificationCommand.before.csv")
    void testByStatusFailCommand() {
        mockFailNotification(2);
        String executeCommand = executeCommand("BY_STATUS", "NOT_SENT");
        Assertions.assertEquals("Send by status notifications count = 0", executeCommand);
    }


    void mockSuccessNotification(int count) {
        mockRestServiceServer.expect(ExpectedCount.times(count),
                        requestTo(String.format("%s/send-notification-to-supplier", mbiApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESPONSE_XML, MediaType.APPLICATION_XML));
    }

    void mockFailNotification(int count) {
        mockRestServiceServer.expect(ExpectedCount.times(count),
                        requestTo(String.format("%s/send-notification-to-supplier", mbiApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(response -> {
                    throw new RuntimeException("very bad response");
                });
    }

    private String executeCommand(String... args) {
        final CommandInvocation commandInvocation = commandInvocation("send-notification", (Object[]) args);
        sendNotificationCommand.executeCommand(commandInvocation, terminal());
        return terminalData();
    }
}
