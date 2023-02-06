#include "test_utils.h"

const Market::DataCamp::Flag* FindFlag(const Market::DataCamp::OfferStatus& status, Market::DataCamp::DataSource source) {
    for (const auto& flag : status.disabled()) {
        if (flag.meta().source() == source) {
            return &flag;
        }
    }
    return nullptr;
}
