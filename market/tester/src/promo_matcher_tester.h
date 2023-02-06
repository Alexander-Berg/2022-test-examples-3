#pragma once
#include "params.h"

#include <market/library/trees/category_tree.h>

#include <mapreduce/yt/interface/client.h>


class TPromoMatcherTester {

public:
    TPromoMatcherTester(const TParams& params);
    ~TPromoMatcherTester();

    void Run();

private:
    const TParams& Params;
    TIntrusivePtr<NYT::IClient> YtClient;
    THolder<const Market::ICategoryTree> CategoryTree;
    THolder<class TPromoMatcher> PromoMatcher;
    THashSet<TString> WareMd5s;
};
