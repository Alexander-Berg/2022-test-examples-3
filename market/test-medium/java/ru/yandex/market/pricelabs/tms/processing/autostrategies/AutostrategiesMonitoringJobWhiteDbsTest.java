package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.time.Instant;

import NMarket.NAmore.NAutostrategy.MarketAmoreService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

public class AutostrategiesMonitoringJobWhiteDbsTest extends AutostrategiesMonitoringJobWhiteTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.white);
    }

    @Override
    protected AutostrategySettings autostrategySettings() {
        return new AutostrategySettings()
                .type(AutostrategySettings.TypeEnum.CPA)
                .cpa(new AutostrategySettingsCPA().drrBid(200L));
    }

    @Override
    protected void expectAmoreSettings(MarketAmoreService.TAutostrategies.AutostrategyParams.Builder params) {
        params.getCpaBuilder().setCpa(200);
    }

    @Override
    protected void acceptTargetOffer(AutostrategyOfferTarget offer) {
        offer.setBid(200);
    }

    @Override
    protected void assertEmptyWebServerResponse(Instant now) throws InvalidProtocolBufferException {
        super.assertEmptyWebServerResponse(now);
    }

    @Override
    protected void assertWebServerAutostrategySentResponse(int autostrategyId, Instant now)
            throws InvalidProtocolBufferException {
        super.assertWebServerAutostrategySentResponse(autostrategyId, now);
    }

    @Override
    protected void assertWebServerNoOfferResponse(int shopId) throws InvalidProtocolBufferException {
        super.assertWebServerNoOfferResponse(shopId);
    }
}
