package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.services.database.model.Task;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferSource;

public class AutostrategiesMonitoringJobVendorBlueTest extends AbstractAutostrategiesMonitoringJobTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.vendorBlue);
        executors.autostrategyOffersVendorSource().clearTargetTable();
    }

    @Override
    protected AutostrategyFilter autostrategyFilter() {
        return new AutostrategyFilter()
                .type(AutostrategyFilter.TypeEnum.VENDOR)
                .vendor(new AutostrategyFilterVendor().models(List.of(1)));
    }

    /*
    @Override
    protected AutostrategySettings autostrategySettings() {
        return new AutostrategySettings()
                .type(AutostrategySettings.TypeEnum.VPOS)
                .vpos(new AutostrategySettingsVPOS().maxBid(300L).position(1));
    }

    @Override
    protected void expectAmoreSettings(MarketAmoreService.TAutostrategies.AutostrategyParams.Builder params) {
        params.getPositionalBuilder().setPosition(1).setMaxBid(300);
    }
    */

    @Override
    protected void acceptTargetOffer(AutostrategyOfferTarget offer) {
        offer.setModel_id(0);
    }

    @Override
    protected void assertAutostrategyOffersClean(int id, Consumer<AutostrategyOfferTarget> updateOfferBefore,
                                                 Consumer<AutostrategyOfferTarget> updateOffer, Instant before,
                                                 Instant now) {
        assertAutostrategyOffers(id, updateOfferBefore, before);
    }

    @Override
    protected void acceptAutostrategy(int id, Instant now, Task task) {
        executors.autostrategyOffersVendorTarget().clearTargetTable();
        executors.autostrategyOffersVendorSource().insert(List.of(
                autostrategyOfferSource(ApiConst.VIRTUAL_SHOP_BLUE, ApiConst.VIRTUAL_FEED_BLUE, FEED_ID + ".4370",
                        SHOP_ID, id, DATASOURCE_ID, now, BUSINESS_ID, 0, 300, o -> {
                            o.setJob_id(task.getJob_id());
                            o.setTask_id(task.getTask_id());
                        }),
                autostrategyOfferSource(ApiConst.VIRTUAL_SHOP_BLUE, ApiConst.VIRTUAL_FEED_BLUE, FEED_ID + ".4371",
                        SHOP_ID, id, DATASOURCE_ID, now, BUSINESS_ID, 0, 300, o -> {
                            o.setJob_id(task.getJob_id());
                            o.setTask_id(task.getTask_id());
                        })
        ));
    }

    @Override
    protected void assertAutostrategyOffers(int id, Consumer<AutostrategyOfferTarget> updateOffer, Instant now) {
        super.assertAutostrategyOffers(id, updateOffer, getInstant());
    }

    @Override
    protected void assertEmptyWebServerResponse(Instant now) {
        return; // TODO Поддержать mbi-bidding для CPA стратегий
    }
}
