package ru.yandex.market.pricelabs.tms.processing.offers;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.model.types.BidsFrom;
import ru.yandex.market.pricelabs.model.types.ShopStatus;

@Value
@Builder
class FullLoopParams {
    ShopStatus status;
    String stateReasons;
    boolean processByPlv2;
    boolean disabledByUser;
    boolean processedToday;
    boolean expectSync;
    boolean expectRequest;
    boolean expectChange;
    boolean searchOffer;
    int position;
    boolean minPriceInModel;
    BidsFrom bidsFrom;
    boolean cleanExistingOffers;
    int batchSize;
    LoopMode loopMode;
    String response;

    public static class FullLoopParamsBuilder {
        FullLoopParamsBuilder() {
            status = ShopStatus.ACTIVE;
            stateReasons = "";
            processByPlv2 = true;
            position = StrategiesCalculator.TYPE_HIDE;
            bidsFrom = BidsFrom.UI_OR_API;
            cleanExistingOffers = true;
            batchSize = 1000;
            loopMode = LoopMode.NORMAL;
            response = LogOutput.OK;
        }

    }
}
