package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.time.Instant;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

public class AutostrategiesMonitoringJobBlueTest extends AbstractAutostrategiesMonitoringJobTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.blue);
    }

    @Override
    protected void acceptTargetOffer(AutostrategyOfferTarget offer) {
        offer.setBid(300);
    }

    @Override
    protected void assertWebServerAutostrategySentResponse(int autostrategyId, Instant now)
            throws InvalidProtocolBufferException {
        super.assertWebServerAutostrategySentResponse(autostrategyId, now);
    }
}
