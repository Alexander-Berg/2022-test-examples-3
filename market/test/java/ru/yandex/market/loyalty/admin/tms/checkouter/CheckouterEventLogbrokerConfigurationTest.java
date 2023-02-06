package ru.yandex.market.loyalty.admin.tms.checkouter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.loyalty.admin.config.logbroker.CheckouterEventsLogbrokerConsumerConfiguration;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@TestPropertySource(locations = "classpath:test.properties")
@Import(CheckouterEventsLogbrokerConsumerConfiguration.class)
public class CheckouterEventLogbrokerConfigurationTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private List<LogbrokerReader> readers;

    @Test
    public void shouldCreateReaders() {
        assertThat(readers, hasSize(CheckouterEventsLogbrokerConsumerConfiguration.CONSUMER_COUNT));
    }

}
