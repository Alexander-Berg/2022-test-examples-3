#pragma once
#include "market/qpipe/prices/test_helpers/common.h"
#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <google/protobuf/util/message_differencer.h>

class TQPipeDataWrapper
{
public:
    TQPipeDataWrapper(TQPipeData* raw, const TDataSource dataSource, const TTimestamp ts)
        : Data(raw)
    {
        Data->set_source(dataSource);
        Data->set_timestamp(ts);
        Data->set_type(NMarket::NQPipe::EntityType::PRICES);
    }

    TQPipeDataWrapper& WithPrice(uint64_t price)
    {
        Data->mutable_fields()->mutable_binary_price()->set_price(price);
        return *this;
    }

    TQPipeDataWrapper& WithDeleted(bool deleted = true)
    {
        Data->mutable_fields()->set_offer_deleted(deleted);
        return *this;
    }

    TQPipeDataWrapper& WithVat(int vat)
    {
        Data->mutable_fields()->set_vat(vat);
        return *this;
    }

    TQPipeDataWrapper& WithFlags(uint32_t flags)
    {
        Data->mutable_fields()->set_flags(flags);
        return *this;
    }

    TQPipeDataWrapper& WithVersion(uint32_t version)
    {
        Data->set_version(version);
        return *this;
    }

    TQPipeData* Data = nullptr;
};


class TQPipeRecordWrapper
{
public:
    TQPipeRecordWrapper(TQPipeRecord* raw, const TFeedId feedId, const TOfferId& offerId, const TMarketSku marketSku)
        : Offer(raw)
    {
        Offer->set_feed_id(feedId);
        if (offerId)
            Offer->set_offer_id(offerId);
        if (marketSku)
            Offer->set_market_sku(marketSku);
    }

    TQPipeDataWrapper AddData(const TDataSource dataSource, const TTimestamp ts)
    {
        return TQPipeDataWrapper(Offer->add_data(), dataSource, ts);
    }

    TQPipeRecord* Offer = nullptr;
};


class TQPipeDelta
{
private:
    uint32_t DefaultMergerVersion = 0;

public:
    TQPipeDelta() {}
    TQPipeDelta(uint32_t defaultMergerVersion) : DefaultMergerVersion(defaultMergerVersion) {}
    TQPipeDelta(const TString& filename)
    {
        Load(filename);
    }

    TQPipeRecordWrapper AddRecord(const TFeedId feedId, const TOfferId& offerId)
    {
        Records.push_back(TQPipeRecord());
        TQPipeRecord* record = &Records.back();
        record->set_merger_version(DefaultMergerVersion);
        return TQPipeRecordWrapper(record, feedId, offerId, 0);
    }

    TQPipeRecordWrapper AddMskuRecord(const TFeedId feedId, const TMarketSku marketSku, const TOfferId& offerId = "")
    {
        Records.push_back(TQPipeRecord());
        TQPipeRecord* record = &Records.back();
        record->set_merger_version(DefaultMergerVersion);
        return TQPipeRecordWrapper(record, feedId, offerId, marketSku);
    }

    void Load(const TString& filename)
    {
        NMarket::TSnappyProtoReader reader(filename, "QPIP");
        TQPipeRecord offer;

        Records.clear();
        while (reader.Load(offer)) {
            Records.push_back(offer);
        }
    }

    void Save(const TString& filename) const
    {
        NMarket::TSnappyProtoWriter writer(filename, "QPIP");
        for (const auto& rec: Records) {
            writer.Write(rec);
        }
    }

    operator TString() const
    {
        if (Records.empty())
            return "Empty";

        TString res;
        NProtobufJson::TProto2JsonConfig config;
        config.FormatOutput = false;

        for (const auto& rec: Records) {
            res += NProtobufJson::Proto2Json(rec, config) + "\n";
        }

        return res;
    }

    bool operator==(const TQPipeDelta& other) const
    {
        if (Records.size() != other.Records.size())
            return false;

        for (size_t i = 0; i < Records.size(); ++i) {
            if (!google::protobuf::util::MessageDifferencer::Equals(Records[i], other.Records[i]))
                return false;
        }

        return true;
    }

    bool operator!=(const TQPipeDelta& other) const {return !(*this == other);}

    void Sort()
    {
        ::Sort(Records.begin(), Records.end(), [](const TQPipeRecord& lhs, const TQPipeRecord& rhs) {
            if (lhs.feed_id() != rhs.feed_id())
                return lhs.feed_id() < rhs.feed_id();

            if (lhs.has_market_sku() && rhs.has_market_sku())
                return lhs.market_sku() < rhs.market_sku();

            if (lhs.has_market_sku())
                return true;

            if (rhs.has_market_sku())
                return false;

            return lhs.offer_id() < rhs.offer_id();
        });
    }

    TQPipeDelta Sorted() const
    {
        TQPipeDelta res = *this;
        res.Sort();
        return res;
    }

    static void AssertEqual(const TQPipeDelta& fact, const TQPipeDelta& expectation, const TString& tag, bool strictOrder = true)
    {
        auto tryPatch = [strictOrder](const TQPipeDelta& delta) -> TQPipeDelta {
            if (strictOrder)
                return delta;
            return delta.Sorted();
        };

        if (tryPatch(fact) != tryPatch(expectation))
            throw yexception() << "Assertion of equality qpipe deltas failed (" << tag << ")"
                               << "\n==========================================\n"
                               << "Expectation:\n" << TString(expectation)
                               << "\n==========================================\n"
                               << "Fact:\n" << TString(fact)
                               << "\n==========================================\n";
    }

private:
    TDeque<TQPipeRecord> Records;
};
