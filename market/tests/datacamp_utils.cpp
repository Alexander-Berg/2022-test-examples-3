#include "datacamp_utils.h"

#include <util/string/split.h>
#include <util/string/strip.h>

TString NMarket::GetCargoTypesAsString(const Market::DataCamp::Offer& offer) {
    const TString delimiter = ",";
    TString actualCargoTypes;

    if (offer.content().partner().original().cargo_types().has_meta()) {
        for (i32 i = 0; i < offer.content().partner().original().cargo_types().value().size(); i++) {
            auto cargoType = offer.content().partner().original().cargo_types().value(i);
            if (!actualCargoTypes.Empty()) {
                actualCargoTypes += delimiter;
            }
            actualCargoTypes += ToString(cargoType);
        }
    }
    return actualCargoTypes;
}

