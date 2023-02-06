#pragma once

#include <util/generic/string.h>
#include <market/report/proto/ReportStateRearrFlag.pb.h>

namespace NMarketReport {
    class TTestDataRecord {
    public:
        TTestDataRecord(const TString& reqId,
                        const TString& yandexUid,
                        const TString& flagName,
                        const TString& value,
                        const TString& allTestBuckets,
                        MarketSearch::TReportStateRearrFlags* rearrFlags)
            : ReqId(reqId)
            , YandexUid(yandexUid)
            , FlagName(flagName)
            , Value(value)
            , AllTestBuckets(allTestBuckets)
            , RearrFlags(rearrFlags)
        { }

        void WriteToLog() const;

    private:
        void AddRearrFlag() const;

        const TString ReqId;
        const TString YandexUid;
        const TString FlagName;
        const TString Value;
        const TString AllTestBuckets;
        mutable MarketSearch::TReportStateRearrFlags* RearrFlags;
    };
}
