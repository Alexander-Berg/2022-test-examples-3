package ru.yandex.market.fulfillment.wrap.marschroute.service.outbound.tracking.stock.other;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

class MarschrouteNonFitOutboundStatusSyncTest extends RepositoryTest {

    @Autowired
    private MarschrouteNonFitOutboundStatusSync sync;

    @Test
    @DatabaseSetup("classpath:repository/non_fit_sync/1/setup.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:repository/non_fit_sync/1/expected.xml")
    void produceOnEmptyDatabase() {
        sync.produce();
    }

    @Test
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, modifiers = DateSetterModifier.class,
        value = "classpath:repository/non_fit_sync/2/expected.xml")
    void shouldMakeSyncWhenProduceWithOutboundLessThan90DaysInPastAndOutboundStatusInfoNull() {
        setupCreatedDateInDataSet("classpath:repository/non_fit_sync/2/setup.xml");
        sync.produce();
    }

    @Test
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, modifiers = DateSetterModifier.class,
        value = "classpath:repository/non_fit_sync/3/expected.xml")
    void shouldMakeSyncWhenProduceWithOutboundLessThan90DaysInPastAndOutboundStatusInfoIsNotNull() {
        setupCreatedDateInDataSet("classpath:repository/non_fit_sync/3/setup.xml");
        sync.produce();
    }

    @Test
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, modifiers = DateSetterModifier.class,
        value = "classpath:repository/non_fit_sync/4/expected.xml")
    void shouldSkipOutboundWhenProduceWithMoreThan90DaysInPast() {
        setupCreatedDateInDataSet("classpath:repository/non_fit_sync/4/setup.xml");
        sync.produce();
    }



}
