package ru.yandex.market.partner.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link GetCutoffMessagesServantlet}.
 */
@DbUnitDataSet(before = "GetCutoffMessagesServantletTest.before.csv")
class GetCutoffMessagesServantletTest extends FunctionalTest {
    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        PartnerNotificationMessageServiceTest.mockPN(partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("SUBJECT_1")
                        .body("BODY_1")
                        .priority(1L)
                        .groupId(1L),
                new WebUINotificationResponse()
                        .subject("SUBJECT_2")
                        .body("BODY_2")
                        .priority(1L)
                        .groupId(2L)
                );
    }

    @Test
    void getMessagesTest() {
        ResponseEntity<String> response = get(10, 10774, "json");
        JsonTestUtil.assertEquals(response, getClass(), "json/GetCutoffMessagesServantletTest.json");
    }

    private ResponseEntity<String> get(long userId, long campaignId, String format) {
        return FunctionalTestHelper.get(baseUrl + "/getCutoffMessages?" +
                        "_user_id={userId}&" +
                        "id={campaignId}&" +
                        "format={format}",
                userId, campaignId, format);
    }
}
