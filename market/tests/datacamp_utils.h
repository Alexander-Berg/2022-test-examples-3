#pragma once

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

namespace NMarket {

TString GetCargoTypesAsString(const Market::DataCamp::Offer& offer);

}  // namespace NMarket
