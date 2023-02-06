#pragma once

#include <market/library/common_types/common_types.h>
#include <util/generic/fwd.h>

namespace NMarketReport::NGlobal {
    void LoadTestShops(const TString& path);
    const THashSet<TShopId>& GetTestShopsIds();
}
