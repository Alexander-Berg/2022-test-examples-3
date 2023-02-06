#include "tests.h"
#include <market/library/book_now/common.h>

namespace NMarket {
namespace NQPipe {

using namespace Market::NBookNow;

class TBookNowTest : public TQPipeUpdaterTest
{
private:
    const THashMap<TString, uint64_t> OfferIds = {
        {"Added",                     1},
        {"Untouched",                 2},
        {"UntouchedInvalidRecord",    3},
        {"UntouchedApiRecordPrice",   4},
        {"UntouchedApiRecordBooknow", 5},
        {"UntouchedBadWaremd5",       6},
        {"Changed",                   7},
        {"ChangedOnlyBookNow",        8},
        {"ChangedTwoRecords",         9},
        {"ChangedWithYmlDate",        10},
        {"ChangedOnlyYmlDate",        11},
        {"Deleted",                   12},
    };

private:
    TTestIndexOffer MakeDefaultBaseOfferWithOutlets(uint32_t offerId, size_t outletsCount) const
    {
        TTestIndexOffer res = MakeDefaultBaseOffer(offerId);

        for (size_t i = 0; i < outletsCount; ++i ) {
            Market::NBookNow::TOnStockInfo info;
            info.OutletId = offerId * 100 + i;
            info.Amount = offerId * 1000 + i;
            info.YmlDate = offerId * 1000000;

            res.Outlets.push_back(info);
        }
        return res;
    }

    void OutletsAssert(const TOfferOutlets& expected, const TOfferOutlets& fact, const TString& comment)
    {
        DoAssert(expected.size(), fact.size(), comment + " (size)");
        for (size_t i = 0; i < expected.size(); ++i) {
            DoAssert(expected[i].OutletId, fact[i].OutletId, comment + " (OutletId)");
            DoAssert(expected[i].Amount, fact[i].Amount, comment + " (Amount)");
            DoAssert(expected[i].YmlDate, fact[i].YmlDate, comment + " (YmlDate)");
        }
    }

    void AssertOutletsUntouched(THolder<ITestDataReader>& reader, size_t offerId)
    {
        TTestIndexOffer factOffer;
        reader->GetOffer(offerId, factOffer);
        TTestIndexOffer defaultOffer = MakeDefaultBaseOfferWithOutlets(offerId, 2);

        OutletsAssert(defaultOffer.Outlets,
                      factOffer.Outlets,
                      "Untouched outlets of offer " + ToString(offerId));
    }

    void AssertOutletsChanged(THolder<ITestDataReader>& reader, size_t offerId, const TOfferOutlets& expectedOutlets)
    {
        TTestIndexOffer factOffer;
        reader->GetOffer(offerId, factOffer);

        OutletsAssert(expectedOutlets,
                      factOffer.Outlets,
                      "Changed outlets of offer " + ToString(offerId));
    }

protected:
    void PrepareIndex(const TTestIndexPaths& indexPaths) override
    {
        auto indexWriter = BuildTestIndexWriter(indexPaths);

        for (size_t i = 1; i <= OfferIds.size(); ++i) {
            if (i != OfferIds.at("Added"))
                indexWriter->AddOffer(MakeDefaultBaseOfferWithOutlets(i, 2));
            else
                indexWriter->AddOffer(MakeDefaultBaseOffer(i));
        }

        indexWriter->Dump();
    }

    void PrepareDeltas(const TString& deltaListPath) override
    {
        const TString deltaPath1 = JoinFsPaths(Settings.TestWorkingDir, "delta1.pbuf.sn");
        const TString deltaPath2 = JoinFsPaths(Settings.TestWorkingDir, "delta2.pbuf.sn");

        auto delta1 = BuildDelta(deltaPath1);
        auto delta2 = BuildDelta(deltaPath2);

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("Added"), TDataSource::FEED, 100);
            offer.Fields.add_outlets_data(11);     // OutletPointID
            offer.Fields.add_outlets_data(213);    // RegionID
            offer.Fields.add_outlets_data(1111);   // Amount(InStock)
            delta1->AddOffer(offer);
        }

        {
            uint64_t id = OfferIds.at("Untouched");
            auto offer = MakeDefaultDeltaOffer(id, TDataSource::FEED, 100);
            offer.Fields.add_outlets_data(id * 100);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(id * 1000);

            offer.Fields.add_outlets_data(id * 100 + 1);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(id * 1000 + 1);

            offer.Fields.set_yml_date(id * 1000000);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("UntouchedInvalidRecord"), TDataSource::FEED, 100);
            offer.Fields.add_outlets_data(123);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("UntouchedApiRecordPrice"), TDataSource::API, 100);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("UntouchedApiRecordBooknow"), TDataSource::API, 100);
            offer.Fields.add_outlets_data(22);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(2222);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("UntouchedBadWaremd5"), TDataSource::API, 100);
            offer.Fields.add_outlets_data(23);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(2223);
            offer.BinaryWareMd5 = "1234567812345678";
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("Changed"), TDataSource::FEED, 100);
            offer.Fields.add_outlets_data(104); // OutletPointID
            offer.Fields.add_outlets_data(213); // RegionID
            offer.Fields.add_outlets_data(2);   // Amount(InStock)

            offer.Fields.add_outlets_data(105);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(4);

            offer.Fields.add_outlets_data(106);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(6);

            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("ChangedOnlyBookNow"), TDataSource::FEED, 100);
            offer.Fields.Clear();
            offer.Fields.add_outlets_data(33);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(3333);
            delta1->AddOffer(offer);
        }

        {
            auto offer1 = MakeDefaultDeltaOffer(OfferIds.at("ChangedTwoRecords"), TDataSource::FEED, 100);
            offer1.Fields.add_outlets_data(104); // OutletPointID
            offer1.Fields.add_outlets_data(213); // RegionID
            offer1.Fields.add_outlets_data(2);   // Amount(InStock)

            offer1.Fields.add_outlets_data(105);
            offer1.Fields.add_outlets_data(213);
            offer1.Fields.add_outlets_data(4);

            offer1.Fields.add_outlets_data(106);
            offer1.Fields.add_outlets_data(213);
            offer1.Fields.add_outlets_data(6);

            delta1->AddOffer(offer1);

            auto offer2 = MakeDefaultDeltaOffer(OfferIds.at("ChangedTwoRecords"), TDataSource::FEED, 100);
            offer2.Fields.add_outlets_data(44);
            offer2.Fields.add_outlets_data(213);
            offer2.Fields.add_outlets_data(4444);

            delta2->AddOffer(offer2);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("ChangedWithYmlDate"), TDataSource::FEED, 100);
            offer.Fields.add_outlets_data(55);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(5555);

            offer.Fields.add_outlets_data(56);
            offer.Fields.add_outlets_data(213);
            offer.Fields.add_outlets_data(5556);

            offer.Fields.set_yml_date(123456);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("ChangedOnlyYmlDate"), TDataSource::FEED, 100);
            offer.Fields.set_yml_date(123456);
            delta1->AddOffer(offer);
        }

        {
            auto offer = MakeDefaultDeltaOffer(OfferIds.at("Deleted"), TDataSource::FEED, 100);
            delta1->AddOffer(offer);
        }

        delta1->Dump();
        delta2->Dump();
        WriteDeltasListFile(deltaListPath, {deltaPath1, deltaPath2}, true);
    }

    TUpdaterSettings MakeUpdaterSettings(const TTestIndexPaths& indexPaths, const TString& deltaListPath) override
    {
        TUpdaterSettings updaterSettings;

        updaterSettings.Entity = "booknow";
        updaterSettings.InstructionPath = indexPaths.Instruction;
        updaterSettings.IgnoreInstructionPath = indexPaths.IgnoreInstruction;
        updaterSettings.DeltaListPath = deltaListPath;
        updaterSettings.InputDataPath = indexPaths.BookNowPath;
        updaterSettings.OutputDataPath = indexPaths.BookNowPath;
        updaterSettings.FeedSessionsPath = indexPaths.AllowedSessions;
        updaterSettings.IgnoreApi = false;

        updaterSettings.PricesMetaPath = "";
        updaterSettings.CurrencyRates = "";
        updaterSettings.WareMD5Path = indexPaths.WareMd5;

        return updaterSettings;
    }

    void AssertResults(const TTestIndexPaths& indexPaths) override
    {
        auto reader = BuildTestIndexReader(indexPaths);

        AssertOutletsChanged(reader, OfferIds.at("Added"), {{11, 1111, 0}});
        AssertOutletsUntouched(reader, OfferIds.at("Untouched"));
        AssertOutletsUntouched(reader, OfferIds.at("UntouchedInvalidRecord"));
        AssertOutletsUntouched(reader, OfferIds.at("UntouchedApiRecordPrice"));
        AssertOutletsUntouched(reader, OfferIds.at("UntouchedApiRecordBooknow"));
        AssertOutletsUntouched(reader, OfferIds.at("UntouchedBadWaremd5"));

        AssertOutletsChanged(reader, OfferIds.at("Changed"), {{104, 2, 0}, {105, 4, 0}, {106, 6, 0}});
        AssertOutletsChanged(reader, OfferIds.at("ChangedOnlyBookNow"), {{33, 3333, 0}});
        AssertOutletsChanged(reader, OfferIds.at("ChangedTwoRecords"), {{44, 4444, 0}});
        AssertOutletsChanged(reader, OfferIds.at("ChangedWithYmlDate"), {{55, 5555, 123456}, {56, 5556, 123456}});

        AssertOutletsChanged(reader, OfferIds.at("ChangedOnlyYmlDate"), {});
        AssertOutletsChanged(reader, OfferIds.at("Deleted"), {});
    }

    TString Name() const override
    {
        return "TBookNowTest";
    }

public:
    TBookNowTest(const TTestSettings& settings)
        : TQPipeUpdaterTest(settings)
    {}
};

THolder<TQPipeUpdaterTest> BuildBookNowTest(const TTestSettings& settings)
{
    return MakeHolder<TBookNowTest>(settings);
}


}  // namespace NQPipe
}  // namespace NMarket
