#pragma once

#include "index_iterator.h"
#include "keyinv_searcher.h"
#include "tarc_searcher.h"

#include <search/tools/test_shard/options.h>

#include <util/generic/string.h>

namespace NTestShard {

class TIndexIteratorFactory {
public:
    TIndexIteratorFactory(const TOptions& opts);

    THolder<IIndexIterator> CreateIterator(const TString& attribute) const;

private:
    TString ShardDir_;
    TKeyInvSearcher KeyInvSearcher_;
    TArcSearcher ArcSearcher_;
    ui32 MaxAsteriskDocs_ = 0;
    ui32 MinAsteriskPrefix_ = 0;
};

}
