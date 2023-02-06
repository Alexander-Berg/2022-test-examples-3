package ru.yandex.market.pricelabs.tms.processing.offers;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.model.types.BidsFrom;

@Value
@Builder
class ShortLoopParams {
    LoopType loopType;
    boolean createStrategy;
    int position;
    boolean createFastModels;
    boolean disabledByUser;
    boolean processedToday;
    boolean expectLoop;
    boolean expectUpdated;
    boolean expectOfferChanged;
    boolean expectBidsGet;
    boolean expectBidsSet;
    boolean searchOffer;
    boolean minPriceInModel;
    BidsFrom bidsFrom;
    LoopMode loopMode;
    String response;
    boolean recommendationsAlreadyUpdated;


    public static class ShortLoopParamsBuilder {
        ShortLoopParamsBuilder() {
            loopType = LoopType.FULL;
            position = StrategiesCalculator.TYPE_HIDE;
            expectOfferChanged = true;
            bidsFrom = BidsFrom.UI_OR_API;
            loopMode = LoopMode.NORMAL;
            response = LogOutput.OK;
        }
    }
}
