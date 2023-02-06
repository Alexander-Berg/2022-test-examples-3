#pragma once
#include "market/qpipe/prices/test_helpers/common.h"
#include <market/library/snappy-protostream/proto_snappy_stream.h>

class TQIdxRecordWrapper
{
public:
    TQIdxRecordWrapper(TQIdxRecord& raw, const TFeedId feedId, const TOfferId& offerId, const TTimestamp offersRobotSession)
        : Record(&raw)
    {
        Record->set_feed_id(feedId);
        Record->set_offer_id(offerId);
        Record->set_offers_robot_session(offersRobotSession);
    }

    TQIdxRecordWrapper& WithPrice(uint64_t price)
    {
        Record->mutable_binary_price()->set_price(price);
        return *this;
    }

    TQIdxRecordWrapper& WithHasGone()
    {
        Record->set_flags(Record->flags() | NMarket::NDocumentFlags::OFFER_HAS_GONE);
        return *this;
    }

    TQIdxRecordWrapper& WithVat(int vat)
    {
        Record->set_vat(vat);
        return *this;
    }

    TQIdxRecordWrapper& WithVersion(uint32_t version)
    {
        Record->set_qidx_version(version);
        return *this;
    }

    TQIdxRecord* Record = nullptr;
};

class TQIdxDelta
{
public:
    TQIdxDelta() {}
    TQIdxDelta(const TString& filename);

    TQIdxRecordWrapper AddRecord(const TFeedId feedId, const TOfferId& offerId, const TTimestamp offersRobotSession)
    {
        Records.push_back(TQIdxRecord());
        return TQIdxRecordWrapper(Records.back(), feedId, offerId, offersRobotSession);
    }

    void Load(const TString& filename)
    {
        NMarket::TSnappyProtoReader reader(filename, "GLOG");
        TQIdxRecord offer;

        Records.clear();
        while (reader.Load(offer)) {
            Records.push_back(offer);
        }
    }

    void Save(const TString& filename) const
    {
        NMarket::TSnappyProtoWriter writer(filename, "GLOG");
        for (const auto& rec: Records) {
            writer.Write(rec);
        }
    }

private:
    TDeque<TQIdxRecord> Records;
};
