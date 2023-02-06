package ru.yandex.market.pricelabs.tms.processing.offers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies;
import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.AutostrategyParams;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsVPOS;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.StrategyState;
import ru.yandex.market.pricelabs.model.StrategyStateHistory;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.misc.TimingUtils.timeSource;
import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest.FEED_ID;
import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest.SHOP_ID;

@RequiredArgsConstructor
public class OffersCheckers {
    @NonNull
    private final AbstractOffersProcessorTest test;

    static StrategyState strategyState(long strategyId, int offerCount) {
        return TmsTestUtils.strategyState(strategyId, s -> {
            var now = timeSource().getInstant();
            s.setShop_id(SHOP_ID);
            s.setFeed_id(FEED_ID);
            s.setLinked_at(now);
            s.setUpdated_at(now);
            s.setLinked_count(offerCount);
        });
    }

    static StrategyStateHistory strategyStateHistory(long strategyId, int offerCount) {
        return TmsTestUtils.strategyStateHistory(strategyId, s -> {
            var now = timeSource().getInstant();
            s.setShop_id(SHOP_ID);
            s.setFeed_id(FEED_ID);
            s.setLinked_at(now);
            s.setUpdated_at(now);
            s.setLinked_count(offerCount);
            s.setChange_id(now.toEpochMilli());
        });
    }

    static AutostrategyState autostrategyState(int autostrategyId, boolean enabled,
                                               int count, int sskuCount, Instant now) {
        return autostrategyState(SHOP_ID, autostrategyId, enabled, count, sskuCount, now);
    }

    static AutostrategyState autostrategyState(int shopId, int autostrategyId, boolean enabled,
                                               int count, int sskuCount, Instant now) {
        return autostrategyState(shopId, autostrategyId, enabled, count, sskuCount, now, Utils.emptyConsumer());
    }

    static AutostrategyState autostrategyState(int shopId, int autostrategyId, boolean enabled, int count,
                                               int sskuCount, Instant now, Consumer<? super AutostrategyState> init) {
        return TmsTestUtils.autostrategyState(autostrategyId, shopId, state -> {
            state.setLinked_enabled(enabled);
            state.setLinked_at(now);
            state.setLinked_count(count);
            state.setSsku_linked_count(sskuCount);
            state.setUpdated_at(now);
            init.accept(state);
        });
    }

    static AutostrategyStateHistory autostrategyStateHistory(int autostrategyId, boolean enabled,
                                                             int count, int sskuCount, Instant now) {
        return autostrategyStateHistory(SHOP_ID, autostrategyId, enabled, count, sskuCount, now);
    }

    static AutostrategyStateHistory autostrategyStateHistory(int shopId, int autostrategyId, boolean enabled,
                                                             int count, int sskuCount, Instant now) {
        return autostrategyStateHistory(shopId, autostrategyId, enabled, count, sskuCount, now, Utils.emptyConsumer());
    }

    static AutostrategyStateHistory autostrategyStateHistory(int shopId, int autostrategyId, boolean enabled, int count,
                                                             int sskuCount, Instant now,
                                                             Consumer<? super AutostrategyState> init) {
        return TmsTestUtils.autostrategyStateHistory(autostrategyId, shopId, state -> {
            state.setLinked_enabled(enabled);
            state.setLinked_at(now);
            state.setLinked_count(count);
            state.setSsku_linked_count(sskuCount);
            state.setUpdated_at(now);
            state.setChange_id(now.toEpochMilli());
            init.accept(state);
        });
    }

    static void resetWareAndCurrency(Offer offer) {
        // ware_md5 и currency не считается в старой модели
        offer.setWare_md5("");
        offer.setCurrency_id("");
        offer.setMax_bid(0L);
    }


    public void checkAmore(String method, Consumer<TAutostrategies.Builder> init)
            throws InvalidProtocolBufferException {
        var request = test.mockWebServerAmore.getMessage();
        assertEquals(method, request.getRequestUrl().queryParameter("what"));
        var actualAmoreBuilder = TAutostrategies.parseFrom(request.getBody().readByteArray())
                .toBuilder();
        var expectAmoreBuilder = TAutostrategies.newBuilder();
        init.accept(expectAmoreBuilder);

        Stream.of(expectAmoreBuilder, actualAmoreBuilder)
                .forEach(b -> b.getShopsBuilderList().forEach(s -> {
                    var newList = new ArrayList<>(s.getAsParamsList());
                    newList.sort(Comparator.comparingInt(AutostrategyParams::getUid));
                    s.clearAsParams();
                    s.addAllAsParams(newList);
                }));
        assertEquals(expectAmoreBuilder.build(), actualAmoreBuilder.build());
    }

    static AutostrategySave vendorAutostrategy(long maxBid, List<Long> businesses, List<Integer> models) {
        return new AutostrategySave()
                .name("v1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.VENDOR)
                        .vendor(new AutostrategyFilterVendor()
                                .businesses(businesses)
                                .models(models)))
                .settings(new AutostrategySettings()
                        .type(AutostrategySettings.TypeEnum.VPOS)

                        .vpos(new AutostrategySettingsVPOS()
                                .position(1)
                                .maxBid(maxBid)));
    }
}
