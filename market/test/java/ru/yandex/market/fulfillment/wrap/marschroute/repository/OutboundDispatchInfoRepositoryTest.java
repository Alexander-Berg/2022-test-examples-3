package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundDispatchInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;

import java.time.LocalDateTime;
import java.util.Optional;

@DatabaseSetup("classpath:repository/outbound_dispatch_info/setup.xml")
class OutboundDispatchInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private OutboundDispatchInfoRepository repository;

    @Test
    @ExpectedDatabase(value = "classpath:repository/outbound_dispatch_info/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void findByOutboundId() {
        softly
            .assertThat(repository.findByOutboundId(new ResourceId("ya1", "partner1")))
            .isEqualTo(Optional.of(new OutboundDispatchInfo()
                .setYandexId("ya1")
                .setPartnerId("partner1")
                .setDispatchPartnerId("dispatch1")
                .setCreated(LocalDateTime.parse("2019-08-02T16:21:59"))));

        softly
            .assertThat(repository.findByOutboundId(new ResourceId("ya2", "partner2")))
            .isEqualTo(Optional.of(new OutboundDispatchInfo()
                .setYandexId("ya2")
                .setPartnerId("partner2")
                .setDispatchPartnerId("dispatch2")
                .setCreated(LocalDateTime.parse("2019-08-02T16:22:00"))));

        softly
            .assertThat(repository.findByOutboundId(new ResourceId("ya-id", "partner-id")))
            .isEqualTo(Optional.empty());
    }

    @Test
    @ExpectedDatabase(value = "classpath:repository/outbound_dispatch_info/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertExisting() {
        repository.insertIfMissing(
            OutboundDispatchInfo.of("ya1", "new-parent-id", "new-dispatch-id"));
    }

    @Test
    @ExpectedDatabase(value = "classpath:repository/outbound_dispatch_info/expected_with_inserted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertMissing() {
        repository.insertIfMissing(new OutboundDispatchInfo()
            .setYandexId("ya3")
            .setPartnerId("partner3")
            .setDispatchPartnerId("dispatch3")
            .setCreated(LocalDateTime.parse("2019-08-02T16:23:00")));
    }
}
