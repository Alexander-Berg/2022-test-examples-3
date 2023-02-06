package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.List;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.StrategyState;
import ru.yandex.market.pricelabs.model.StrategyStateHistory;

@Value
@Builder
class Expect {
    @NonNull List<NewOfferGen> newRows;
    @NonNull List<Offer> existingRows;
    @NonNull List<Offer> expectRows;
    @NonNull List<AutostrategyOfferTarget> autostrategyOffers;
    @NonNull List<AutostrategyState> autostrategyStates;
    @NonNull List<AutostrategyStateHistory> autostrategyStatesHistory;
    @NonNull List<AutostrategyShopState> autostrategyShopStates;
    @NonNull List<StrategyState> strategyStates;
    @NonNull List<StrategyStateHistory> strategyStatesHistory;
    boolean disabledInShopExists;
    @NonNull String response;
    int feedId;
    @Nullable
    Runnable action;
    boolean isBlue;

    static class ExpectBuilder {
        ExpectBuilder() {
            newRows = List.of();
            existingRows = List.of();
            expectRows = List.of();
            autostrategyOffers = List.of();
            autostrategyStates = List.of();
            autostrategyStatesHistory = List.of();
            autostrategyShopStates = List.of();
            strategyStates = List.of();
            strategyStatesHistory = List.of();
            response = LogOutput.OK;
            action = null;
            isBlue = false;
        }
    }
}
