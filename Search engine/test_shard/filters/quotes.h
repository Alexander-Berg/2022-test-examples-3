#pragma once

/* See https://st.yandex-team.ru/SEARCH-7284 */

#include "filter.h"

namespace NTestShard {

class TQuotesFilter : public IQueryFilter {
public:
    bool IsPassed(const TAttrRefTree& query) override;

    inline TString Name() const override {
        return "Quotes";
    }
};

}
