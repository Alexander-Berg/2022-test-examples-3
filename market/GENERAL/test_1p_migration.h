#pragma once

#include <market/idx/datacamp/proto/offer/UnitedOffer.pb.h>
#include <util/generic/maybe.h>

namespace NMarket::NDataCamp::NFixer {
    TMaybe<Market::DataCamp::UnitedOffer> UpdateUnitedCatalogFor1pTest(const Market::DataCamp::UnitedOffer& sourceOffer);
}
