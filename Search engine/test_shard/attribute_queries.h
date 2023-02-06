#pragma once

#include "options.h"

#include <search/tools/test_shard/common/inverse_index.h>
#include <search/tools/test_shard/filters/filter.h>
#include <search/tools/test_shard/idx_read/factory.h>
#include <search/tools/test_shard/idx_read/url_hash.h>

#include <util/generic/hash.h>
#include <util/generic/ptr.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/system/mutex.h>
#include <util/thread/pool.h>

#include <kernel/doom/wad/wad.h>


namespace NProto {

class TQuery;
class TQueryVector;

}

namespace NTestShard {

using NProto::TQuery;
using NProto::TQueryVector;

struct TSafeQueryVector {
    TMutex Lock;
    TQueryVector* Output;
};

class TAttributesQueriesBuilder {
public:
    TAttributesQueriesBuilder(const TOptions& opts);

    void PrepareCache(const THashSet<TStringBuf>& attrs);
    void Build(ui32 size, const TAttrSchemeTree& attrs, TSafeQueryVector& output);
    void Join();

private:
    void Worker(ui32 size, const TAttrSchemeTree& attrs, TSafeQueryVector& output);

private:
    TAttributeCache Cache_;

    TIndexIteratorFactory Factory_;
    TRequestFilter Filter_;

    THolder<IThreadPool> BuildersQueue_;
    TUrlHasher Hasher_;
    const TShardInfo& Shard_;

    TString CacheDir_;

    ui32 ThreadsCount_ = 0;
    ui64 ConfigHash_ = 0;
};

}
