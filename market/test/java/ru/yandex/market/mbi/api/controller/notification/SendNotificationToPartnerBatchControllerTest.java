package ru.yandex.market.mbi.api.controller.notification;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationToPartnerRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "SendNotificationBatchControllerTest.before.csv")
class SendNotificationToPartnerBatchControllerTest extends FunctionalTest {
    private static final int PROMO_NOTIFICATION_TYPE = 1606180002;
    private static final int ORDER_NOTIFICATION_TYPE = 1563377943;
    private static final int ORDER_ERRORS_NOTIFICATION_TYPE = 1558087772;
    private static final int WHITE_NOTIFICATION_TYPE = 1485961856;
    private static final long PARTNER_ID = 1L;
    private static final String ORDER_XML =
            // language=xml
            "<abo-info>\n" +
                    "   <order-id>123</order-id>\n" +
                    "</abo-info>";
    private static final String RECENT_PROMOS_XML =
            // language=xml
            "<available-recent-promos>\n" +
                    "   <recent-promos-info>\n" +
                    "       <promo>\n" +
                    "           <startDate>2021-08-2</startDate>\n" +
                    "           <endDate>2021-08-5</endDate>\n" +
                    "           <name>Back to  school 2021</name>\n" +
                    "           <offersCount>11</offersCount>\n" +
                    "           <maxDiscount>22</maxDiscount>\n" +
                    "           <minDiscount>11</minDiscount>\n" +
                    "           <categoriesCount>2</categoriesCount>\n" +
                    "       </promo>\n" +
                    "   </recent-promos-info>\n" +
                    "</available-recent-promos>";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testErrors(boolean needAdditionalData) {
        var okRequest = new SendNotificationToPartnerRequest(
                PARTNER_ID,
                PROMO_NOTIFICATION_TYPE,
                RECENT_PROMOS_XML
        );
        var wrongPartnerId = new SendNotificationToPartnerRequest(
                100,
                PROMO_NOTIFICATION_TYPE,
                RECENT_PROMOS_XML
        );
        var wrongNotificationType = new SendNotificationToPartnerRequest(
                PARTNER_ID,
                -1,
                RECENT_PROMOS_XML
        );
        var requests = List.of(okRequest, wrongPartnerId, okRequest, wrongNotificationType, okRequest);
        var responses = mbiApiClient.sendNotificationsToPartners(requests, needAdditionalData);
        assertThat(responses).hasSameSizeAs(requests);
        verifySentNotificationType(partnerNotificationClient, 4, -1L, PROMO_NOTIFICATION_TYPE);
    }

    @Test
    void testNotificationSendContextsAreCorrect() {
        var requests = List.of(
                new SendNotificationToPartnerRequest(
                        PARTNER_ID,
                        PROMO_NOTIFICATION_TYPE,
                        RECENT_PROMOS_XML
                ),
                new SendNotificationToPartnerRequest(
                        PARTNER_ID + 1,
                        WHITE_NOTIFICATION_TYPE,
                        null
                ),
                new SendNotificationToPartnerRequest(
                        PARTNER_ID + 2,
                        PROMO_NOTIFICATION_TYPE,
                        RECENT_PROMOS_XML
                ),
                new SendNotificationToPartnerRequest(
                        PARTNER_ID + 3,
                        ORDER_NOTIFICATION_TYPE,
                        ORDER_XML
                )
        );
        var responses = mbiApiClient.sendNotificationsToPartners(requests, true);
        assertThat(responses).hasSameSizeAs(requests);
        verifySentNotificationType(
                partnerNotificationClient,
                requests.size(),
                requests.stream().mapToLong(SendNotificationToPartnerRequest::getNotificationType).toArray()
        );
    }

    @Test
    void testNotificationContent() {
        var requests = List.of(
                new SendNotificationToPartnerRequest(
                        PARTNER_ID + 2,
                        ORDER_NOTIFICATION_TYPE,
                        ORDER_XML
                ),
                new SendNotificationToPartnerRequest(
                        PARTNER_ID,
                        ORDER_ERRORS_NOTIFICATION_TYPE,
                        null
                )
        );
        var responses = mbiApiClient.sendNotificationsToPartners(requests, true);
        assertThat(responses).hasSameSizeAs(requests);
        verifySentNotificationType(
                partnerNotificationClient,
                requests.size(),
                requests.stream().mapToLong(SendNotificationToPartnerRequest::getNotificationType).toArray()
        );
    }
}
