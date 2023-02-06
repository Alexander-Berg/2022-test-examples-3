#include "test_data_log_writer.h"

#include <market/report/library/global/time/time.h>

#include <market/report/library/static/static.h>

#include <util/stream/str.h>

void NMarketReport::TTestDataRecord::AddRearrFlag() const {
    if (RearrFlags) {
        auto rearrFlg = RearrFlags->add_flags();
        *(rearrFlg->mutable_name()) = FlagName;
        *(rearrFlg->mutable_value()) = Value;
    }
}

void NMarketReport::TTestDataRecord::WriteToLog() const {
    TStringStream result;
    result << "tskv\ttskv_format=market-experiments-log\t"
           << "unixtime=" << NGlobal::Time() << '\t'
           << "req_id=" << ReqId << '\t'
           << "yandexuid=" << YandexUid << '\t'
           << "rearr_factors=" << FlagName << '\t'
           << "test_data=" << Value << '\t'
           << "test_buckets=" << AllTestBuckets << Endl;
    Static::writeTestDataLog(result.Str());

    AddRearrFlag();
}
