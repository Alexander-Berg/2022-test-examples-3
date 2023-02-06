package ru.yandex.market.mbi.api.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mbi.api.client.entity.notification.context.OrderContextParams;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты для {@link SendMessageToSupplierController}.
 *
 * @author avetokhin 06/03/18.
 */
@DbUnitDataSet(before = "SendMessageToSupplierControllerTest.before.csv")
class SendMessageToSupplierControllerTest extends FunctionalTest {
    private static final int NOTIFICATION_TYPE = 1519887055;
    private static final int ORDER_NOTIFICATION_TYPE = 1563377943;
    private static final long SUPPLIER_ID = 774L;
    private static final String XML_PARAMS = "<feature-name>Акции на Маркете</feature-name>";
    private static final String ORDER_XML_PARAMS = "<abo-info><order-id>123</order-id></abo-info>";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(DateTimes.MOSCOW_TIME_ZONE);

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        "Тест: сегодня, время по владивостоку (+7)",
                        OffsetDateTime.now(ZoneOffset.ofHours(10)),
                        1111,
                        1
                ),
                Arguments.of(
                        "Тест: завтра, время по калининграду (-1)",
                        OffsetDateTime.now(ZoneOffset.ofHours(2)).plusDays(1),
                        1112,
                        2
                ),
                Arguments.of(
                        "Тест: фиксированная дата, московское время",
                        LocalDateTime.from(DATE_TIME_FORMATTER.parse("11.11.2021 11:00"))
                                .atOffset(ZoneOffset.ofHours(3)),
                        SUPPLIER_ID,
                        0L
                )
        );
    }

    @Test
    void supplierNotFound() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.sendMessageToSupplier(1L, NOTIFICATION_TYPE, XML_PARAMS)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    void testProcessedOffersNotificationSent() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        String notificationData =
                // language=xml
                "<processed-offers-data>\n" +
                        "    <READY>4</READY>\n" +
                        "    <NEED_INFO>3</NEED_INFO>\n" +
                        "    <SUSPENDED>2</SUSPENDED>\n" +
                        "    <REJECTED>1</REJECTED>\n" +
                        "    <NEED_CONTENT>5</NEED_CONTENT>" +
                        "</processed-offers-data>";

        FunctionalTestHelper.post(
                URL_PREFIX + port + "/send-notification-to-supplier",
                new HttpEntity<>(
                        // language=xml
                        "<send-message-to-supplier-request>" +
                                "    <supplier-id>" + SUPPLIER_ID + "</supplier-id>" +
                                "    <notification-type>" + NOTIFICATION_TYPE + "</notification-type>" +
                                "    <notification-data>" +
                                "    <![CDATA[" + notificationData + "]]>" +
                                "    </notification-data>" +
                                "</send-message-to-supplier-request>",
                        headers)
        );

        verifySentNotificationType(partnerNotificationClient, 1, NOTIFICATION_TYPE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testData")
    void testProcessedNowExpressNotificationSent(
            String name,
            OffsetDateTime acceptBeforeTime,
            long supplierId,
            long warehouseId
    ) {
        final int notificationType = 1643556238;
        LocalDateTime localDateTime = acceptBeforeTime.atZoneSameInstant(DateTimes.MOSCOW_TIME_ZONE).toLocalDateTime();
        String dateTime = DATE_TIME_FORMATTER.format(localDateTime);
        //language=xml
        String orderXml = "" +
                "<order>\n" +
                "    <id>12345</id>\n" +
                "    <status>PENDING</status>\n" +
                "    <money-amount>5 000</money-amount>\n" +
                "    <accept-before-time>" + dateTime + "</accept-before-time>\n" +
                "    <fulfilment-warehouse-id>" + warehouseId + "</fulfilment-warehouse-id>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>Товар 1</title>\n" +
                "            <sku>sku1</sku>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "</order>\n";

        mbiApiClient.sendMessageToSupplier(
                supplierId,
                notificationType,
                orderXml,
                new OrderContextParams(12345, OrderStatus.PENDING.getId())
        );

        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }

    @Test
    void notificationSent() {
        SendNotificationResponse response =
                mbiApiClient.sendMessageToSupplier(SUPPLIER_ID, NOTIFICATION_TYPE, XML_PARAMS);
        assertThat(response, notNullValue());
        assertThat(response.getNotificationGroupId(), equalTo(1L));

        verifySentNotificationType(partnerNotificationClient, 1, NOTIFICATION_TYPE);
    }

    @Test
    void sendNotificationAboutOrder() {
        SendNotificationResponse response =
                mbiApiClient.sendMessageToSupplier(SUPPLIER_ID, ORDER_NOTIFICATION_TYPE, ORDER_XML_PARAMS);
        assertThat(response, notNullValue());

        verifySentNotificationType(partnerNotificationClient, 1, ORDER_NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Пора создать заявку на отгрузку 1567083640.xsl")
    void notificationFFCreateRequestShipment() {
        final int notificationType = 1567083640;
        SendNotificationResponse response =
                mbiApiClient.sendMessageToSupplier(SUPPLIER_ID, notificationType, null);
        assertThat(response, notNullValue());
        assertThat(response.getNotificationGroupId(), equalTo(1L));

        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }

    @Test
    void testSendOrderAcceptMessageViaTelegram() {
        final int notificationType = 1612872409;
        //language=xml
        String orderXml = "" +
                "<order>\n" +
                "    <accept-before-time>19:15</accept-before-time>\n" +
                "    <id>12345</id>\n" +
                "    <status>PENDING</status>\n" +
                "    <money-amount>5 000</money-amount>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>Товар 1</title>\n" +
                "            <sku>sku1</sku>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "</order>\n";

        mbiApiClient.sendMessageToSupplier(
                SUPPLIER_ID,
                notificationType,
                orderXml,
                new OrderContextParams(12345, OrderStatus.PENDING.getId())
        );

        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }

    @Test
    void testSendProcessingOrderAcceptMessageViaTelegram() {
        final int notificationType = 1612872409;
        //language=xml
        String orderXml = "" +
                "<order>\n" +
                "    <accept-before-time>19:15</accept-before-time>\n" +
                "    <id>12345</id>\n" +
                "    <status>PROCESSING</status>\n" +
                "    <money-amount>5 000</money-amount>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>Товар 1</title>\n" +
                "            <sku>sku1</sku>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "</order>\n";

        mbiApiClient.sendMessageToSupplier(
                SUPPLIER_ID,
                notificationType,
                orderXml,
                new OrderContextParams(12345, OrderStatus.PROCESSING.getId())
        );

        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
    }
}
