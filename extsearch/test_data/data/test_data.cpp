#include "test_data.h"

#include <kernel/searchlog/errorlog.h>
#include <library/cpp/http/simple/http_client.h>

namespace NImages {
    namespace NCbir {
        THashMap<TString, TString> GetIndexTestFiles() {
            /* example
            return THashMap<TString, TString> INDEX_TEST_FILES = {
                    {"imgsidx-000-20220116-142436", "/2748661122"},
                    {"imgsidx-001-20220116-142436", "/2748671532"},
                    ...
            };
            */
            return {};
        };

        bool GetTestDataForShard(const TString& shard, NCbirTestData::TTestDocData& docTestExtInfo) {
            const auto& dataMapping = GetIndexTestFiles();
            if (!dataMapping.contains(shard)) {
                return false;
            }
            TSimpleHttpClient connect("https://proxy.sandbox.yandex-team.ru", 443,
                                      TDuration::Seconds(30),
                                      TDuration::Seconds(5));
            SEARCH_INFO << "Get data for: " << shard << " from " << dataMapping.at(shard) << Endl;
            TStringStream data;
            connect.DoGet(dataMapping.at(shard), &data);
            Y_UNUSED(docTestExtInfo.ParseFromString(data.Str()));
            return true;
        }
    }
}
