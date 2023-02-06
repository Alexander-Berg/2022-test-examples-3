package ru.yandex.direct.core.entity.deal.repository;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestMdsConstants;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.DealNotificationsRecord;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.DEAL_NOTIFICATIONS;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealNotificationRepositoryTest {
    @Autowired
    private DealNotificationRepository dealNotificationRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private int shard;
    private ClientId clientId;
    private String notificationId;
    private Long dealId;
    private String serializedMdsKey;
    private String mdsUrl;

    @Before
    public void setUp() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        shard = client.getShard();
        clientId = client.getClientId();

        DealInfo deal = steps.dealSteps().addRandomDeals(client, 1).iterator().next();
        dealId = deal.getDealId();

        notificationId = "20180327-977066821";
        serializedMdsKey = "1214/deal-notifications/60631523/161903/yandex-direct-deal-confirmation-20180327-977066821.pdf";
        mdsUrl = TestMdsConstants.TEST_PATH_URL + serializedMdsKey;
    }

    @Test
    public void testLogNotification() {
        Long logId = dealNotificationRepository.logNotification(shard, clientId, notificationId,
                dealId, serializedMdsKey, mdsUrl);

        DealNotificationsRecord fetchedRecord = dslContextProvider.ppc(shard)
                .select(DEAL_NOTIFICATIONS.CLIENT_ID,
                        DEAL_NOTIFICATIONS.CLIENT_NOTIFICATION_ID,
                        DEAL_NOTIFICATIONS.CREATION_TIME,
                        DEAL_NOTIFICATIONS.DEAL_ID,
                        DEAL_NOTIFICATIONS.PDF_MDS_KEY,
                        DEAL_NOTIFICATIONS.PDF_MDS_URL)
                .from(DEAL_NOTIFICATIONS)
                .where(DEAL_NOTIFICATIONS.DEAL_NOTIFICATION_ID.eq(logId))
                .fetchOneInto(DEAL_NOTIFICATIONS);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(fetchedRecord.getClientid()).isEqualTo(clientId.asLong());
            softly.assertThat(fetchedRecord.getClientNotificationId()).isEqualTo(notificationId);
            softly.assertThat(fetchedRecord.getDealId()).isEqualTo(dealId);
            softly.assertThat(fetchedRecord.getPdfMdsKey()).isEqualTo(serializedMdsKey);
            softly.assertThat(fetchedRecord.getPdfMdsUrl()).isEqualTo(mdsUrl);

            softly.assertThat(fetchedRecord.getCreationTime())
                    .isBefore(LocalDateTime.now().plusMinutes(5))
                    .isAfter(LocalDateTime.now().minusMinutes(5));
        });
    }
}
