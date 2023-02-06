#pragma once

#include <extsearch/images/base/cbir/test_data/protos/test_cbir_data.pb.h>
#include <util/generic/hash.h>
#include <util/generic/string.h>

namespace NImages {
    namespace NCbir {
        THashMap<TString, TString> GetIndexTestFiles();
        bool GetTestDataForShard(const TString& shard, NCbirTestData::TTestDocData& docTestExtInfo);
    }
}
