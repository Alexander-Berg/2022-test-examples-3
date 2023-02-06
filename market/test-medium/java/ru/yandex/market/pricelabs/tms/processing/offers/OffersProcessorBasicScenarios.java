package ru.yandex.market.pricelabs.tms.processing.offers;

import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.AutostrategyParams;
import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.CpaParams;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;

import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest.LARGE_BATCH;
import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest.SMALL_BATCH;

public class OffersProcessorBasicScenarios {

    private OffersProcessorBasicScenarios() {
        //
    }

    static Object[] sizes() {
        return new Object[]{LARGE_BATCH, SMALL_BATCH};
    }

    static Object[] autostrategiesScenarios() {
        // Поддерживается ограниченный набор параметров
        var cpaSettings = new AutostrategySettings()
                .type(AutostrategySettings.TypeEnum.CPA)
                .cpa(new AutostrategySettingsCPA()
                        .drrBid(300L));
        var cpaAmore = AutostrategyParams.newBuilder()
                .setCpa(CpaParams.newBuilder().setCpa(300))
                .build();

        return new Object[][]{
                {new FullLoopParams.FullLoopParamsBuilder().build(),
                        cpaSettings, cpaAmore}
        };
    }
}
