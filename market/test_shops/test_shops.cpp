#include <market/report/library/global/test_shops/test_shops.h>

#include <market/report/library/error_code/error_code.h>
#include <market/report/library/error_logger/error_logger.h>
#include <market/report/library/loading_tracer/loading_tracer.h>
#include <market/library/execution_stats/execution_stats_collector.h>

#include <util/folder/path.h>
#include <util/generic/hash.h>
#include <util/generic/singleton.h>
#include <util/stream/file.h>
#include <util/string/type.h>

using namespace NMarketReport;

namespace {
    class TTestShopHolder {
    public:
        void Load(const TString& path) {
            EXECUTION_STATS_FUNCTION();

            if (!TFsPath(path).Exists()) {
                ReportErrorLog(NReportError::TEST_SHOPS_IDS_FILE_NOT_FOUND) << NLog::TDataFile(path) << " not found: " << path;
                return;
            }

            TLoadingTracer trace("Test Shops", path);
            TFileInput reader{path};
            for (TString line; reader.ReadLine(line);) {
                if (IsNumber(line)) {
                    TestShopsIds.insert(FromString<TShopId>(line));
                } else {
                    ReportErrorLog(NReportError::CANNOT_PARSE_TEST_SHOP_ID) << NLog::TDataFile(path) << " incorrect shop id: " << line;
                }
            }
        }

        const THashSet<TShopId>& GetTestShopsIds() const {
            return TestShopsIds;
        }

    private:
        THashSet<TShopId> TestShopsIds;
    };
}

namespace NMarketReport::NGlobal {
    void LoadTestShops(const TString& path) {
        Singleton<TTestShopHolder>()->Load(path);
    }

    const THashSet<TShopId>& GetTestShopsIds() {
        return Singleton<TTestShopHolder>()->GetTestShopsIds();
    }
}
