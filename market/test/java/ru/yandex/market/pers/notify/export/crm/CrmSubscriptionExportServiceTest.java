package ru.yandex.market.pers.notify.export.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.pers.notify.export.ChangedSubscriptionDao;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.subscription.SubscriptionDao;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.request.trace.TskvRecordBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.model.NotificationTransportType.PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_LIVE_STREAM;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_PERSONAL_ADVERTISING;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus.SUBSCRIBED;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus.UNSUBSCRIBED;

/**
 * @author vtarasoff
 * @since 03.08.2021
 */
public class CrmSubscriptionExportServiceTest extends MarketMailerMockedDbTest {
    private static final Uid PUID = new Uid(123L);
    private static final Uuid UUID = new Uuid("456");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private ChangedSubscriptionDao changedSubscriptionDao;

    private TestCrmSubscriptionTskvWriter writer;
    private CrmSubscriptionExportService service;

    private Subscription advertising;
    private Subscription liveStream;
    private Subscription personal;

    private String advertisingRecord;
    private String liveStreamRecord;
    private String personalRecord;

    @BeforeEach
    public void setUp() throws Exception {
        writer = new TestCrmSubscriptionTskvWriter();
        service = new CrmSubscriptionExportService(jdbcTemplate, writer, changedSubscriptionDao);

        var subscriptions = subscriptionDao.saveAll(List.of(
                new Subscription(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, PUSH, STORE_PUSH_LIVE_STREAM, SUBSCRIBED),
                new Subscription(UUID, PUSH, STORE_PUSH_PERSONAL_ADVERTISING, UNSUBSCRIBED)
        ));

        advertising = subscriptions.get(0);
        liveStream = subscriptions.get(1);
        personal = subscriptions.get(2);

        var records = subscriptions
                .stream()
                .map(this::recordBy)
                .collect(Collectors.toUnmodifiableList());

        advertisingRecord = records.get(0);
        liveStreamRecord = records.get(1);
        personalRecord = records.get(2);
    }

    private String recordBy(Subscription subscription) {
        return new TskvRecordBuilder()
                .add("ID_VALUE", subscription.getIdentity().getValue())
                .add("ID_TYPE", subscription.getIdentity().getType().getId())
                .add("CHANNEL", subscription.getChannel().getId())
                .add("TYPE", subscription.getType().getId())
                .add("KEY_PARAMS", "no_params")
                .add("STATUS", subscription.getStatus().getId())
                .add("CREATED_AT", subscription.getCreatedAt().getEpochSecond() * 1000)
                .add("MODIFIED_AT", subscription.getModifiedAt().getEpochSecond() * 1000)
                .build();
    }

    @Test
    public void testExportAll() {
        service.exportAll();

        List<String> records = writer.getRecords();
        assertThat(records, hasSize(3));

        assertThat(records.get(0), equalTo(advertisingRecord));
        assertThat(records.get(1), equalTo(liveStreamRecord));
        assertThat(records.get(2), equalTo(personalRecord));
    }

    @Test
    public void testExportChangedOnly() {
        changedSubscriptionDao.saveAll(List.of(advertising.getId(), personal.getId()));

        service.exportChanged();

        List<String> records = writer.getRecords();
        assertThat(records, hasSize(2));

        assertThat(records.get(0), equalTo(advertisingRecord));
        assertThat(records.get(1), equalTo(personalRecord));

        List<Long> changedIds = changedSubscriptionDao.findAll();
        assertTrue(changedIds.isEmpty());
    }
}
