package ru.yandex.market.notifier.application;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotifierProperties;
import ru.yandex.market.notifier.mock.NotifierTestMockFactory;
import ru.yandex.market.notifier.service.NotifierPropertiesHolder;
import ru.yandex.market.notifier.service.ShopMetaService;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.pers.notify.PersNotifyClient;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class AbstractTestBase {

    @Autowired
    protected NotifierTestMockFactory mockFactory;
    @Autowired
    protected ShopMetaService shopMetaService;
    @Autowired
    protected CheckouterClient checkouterClient;
    @Autowired
    protected PersNotifyClient persNotifyClient;
    @Autowired
    protected PushApi pushClient;
    @Autowired
    @Qualifier("masterDS")
    private DataSource dataSource;
    @Autowired
    protected EventTestUtils eventTestUtils;
    @Autowired
    @Qualifier("clock")
    protected TestableClock testableClock;
    @Autowired
    protected NotifierProperties notifierProperties;
    @Autowired
    protected NotifierPropertiesHolder notifierPropertiesHolder;

    @BeforeEach
    public void cleanDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
            databasePopulator.addScript(new ClassPathResource("sql/truncate_tables.sql"));
            databasePopulator.populate(connection);
        }
    }

    protected static Notification getNotificationWithDeliveryChannel(ChannelType type,
                                                                     List<Notification> notifications) {
        return notifications.stream()
                .filter(n -> !CollectionUtils.isEmpty(n.getDeliveryChannels()))
                .filter(n -> n.getDeliveryChannels().stream().anyMatch(dc -> dc.getType() == type))
                .findAny()
                .orElse(null);
    }

    /**
     * Замораживет время во всем приложении.
     *
     * @param instant An instantaneous point on the time-line
     * @param zoneId  A time-zone ID
     */
    protected void setFixedTime(Instant instant, ZoneId zoneId) {
        this.testableClock.setFixed(instant, zoneId);
    }

    /**
     * Очищает зафиксированное время.
     */
    protected void clearFixed() {
        testableClock.clearFixed();
    }

    protected Clock getClock() {
        return this.testableClock;
    }

    protected DataSource getDatasource() {
        return dataSource;
    }
}
