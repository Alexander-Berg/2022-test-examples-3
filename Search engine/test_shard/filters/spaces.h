#pragma once

/* See https://st.yandex-team.ru/SEARCH-7284
 * and https://st.yandex-team.ru/SEARCH-7294
 */

#include "filter.h"

namespace NTestShard {

class TSpacesFilter : public IQueryFilter {
public:
    bool IsPassed(const TAttrRefTree& query) override;

    inline TString Name() const override {
        return "Spaces";
    }
};

}
