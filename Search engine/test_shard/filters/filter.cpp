#include "filter.h"
#include "quotes.h"
#include "size.h"
#include "spaces.h"

namespace NTestShard {

TRequestFilter::TRequestFilter(const TVector<TString>& filterNames) {
    for (auto& filter: filterNames) {
        EnabledFilters_.insert(filter);
    }
    EnabledFilters_.insert("Default");

    RegisterFilter<TQuotesFilter>();
    RegisterFilter<TSizeFilter>();
    RegisterFilter<TSpacesFilter>();
}

bool TRequestFilter::IsPassed(const TAttrRefTree& attrs) const {
    for (const auto& filter : Filters_) {
        if (!filter->IsPassed(attrs)) {
            return false;
        }
    }
    return true;
}

template<typename T>
void TRequestFilter::RegisterFilter() {
    auto holder = MakeHolder<T>();
    if (EnabledFilters_.contains(holder->Name())) {
        Filters_.push_back(std::move(holder));
    }
}

}
