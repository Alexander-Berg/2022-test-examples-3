#include "tests.h"
#include <market/library/taxes/taxes.h>

namespace NMarket {
namespace NQPipe {

using namespace NMarket::NTaxes;

class TVatTest : public TQPipeUpdaterTest
{
private:
    const THashMap<TString, uint64_t> OfferIds = {
        {"Added",                     1},
        {"Untouched",                 2},
        {"UntouchedApiRecordPrice",   3}, //untouched API потому что для ключа feed_id, offer_id vat не применяется
        {"UntouchedApiRecordVat",     4}, //untouched API потому что для ключа feed_id, offer_id vat не применяется
        {"UntouchedBadWaremd5",       5},
        {"Changed18",                 6},
        {"Changed10",                 7},
        {"Changed18_118",             8},
        {"Changed10_110",             9},
        {"Changed0",                  10},
        {"ChangedNoVat",              11},
        {"ChangedInvalid",            12},
        {"ChangedOnlyVat",            13},
        {"ChangedTwoRecords",         14},
        {"Deleted",                   15},
        {"MarketSkuVatOnly",          16},
        {"MarketSkuVatPrice",         17},
        {"MarketSkuPriceOnly",        18},
        {"MarketSkuFeedAndApi",       19},
        {"MarketSkuApiAndFeed",       20},
        {"MarketSkuFeedApiAndRmApi",  21},
        {"MarketSkuDeletedOnly",      22}
    };

private:
    TTestIndexOffer MakeDefaultBaseOfferWithVat(uint32_t offerId, NMarket::NTaxes::EVat vat) const
    {
        TTestIndexOffer res = MakeDefaultBaseOffer(offerId);
        res.Vat = TIndexToReportVatPropsRecord(vat);
        return res;
    }

    TInputOffer MakeDefaultDeltaOfferWithVat(uint32_t offerId, const TDataSource& source, NMarket::NTaxes::EVat vat, TMarketSku marketSku = 0) const
    {
        TInputOffer res = MakeDefaultDeltaOffer(offerId, source, 100, marketSku);
        res.Fields.set_vat((int)vat);
        return res;
    }

    void AssertVatValue(THolder<ITestDataReader>& reader, size_t offerId, NMarket::NTaxes::EVat expected)
    {
        TTestIndexOffer factOffer;
        reader->GetOffer(offerId, factOffer);
        DoAssert(true, factOffer.Vat.Get().Defined(), "defined vat of offer " + ToString(offerId));
        DoAssert((int)expected, (int)*factOffer.Vat.Get(), "value vat of offer " + ToString(offerId));
    }

    void AssertVatUntouched(THolder<ITestDataReader>& reader, size_t offerId)
    {
        TTestIndexOffer factOffer;
        reader->GetOffer(offerId, factOffer);
        DoAssert(true, factOffer.Vat.Get().Defined(), "defined vat of offer " + ToString(offerId));
        DoAssert(EVat::VAT_0, *factOffer.Vat.Get(), "value vat of offer " + ToString(offerId));
    }

    void AssertVatUndefined(THolder<ITestDataReader>& reader, size_t offerId)
    {
        TTestIndexOffer factOffer;
        reader->GetOffer(offerId, factOffer);
        DoAssert(false, factOffer.Vat.Get().Defined(), "defined vat of offer " + ToString(offerId));
    }

protected:
    void PrepareIndex(const TTestIndexPaths& indexPaths) override
    {
        auto indexWriter = BuildTestIndexWriter(indexPaths);

        THashSet<TString> marketSKUOfferNames = {
            "MarketSkuVatOnly",
            "MarketSkuVatPrice",
            "MarketSkuPriceOnly",
            "MarketSkuFeedAndApi",
            "MarketSkuApiAndFeed",
            "MarketSkuFeedApiAndRmApi",
            "MarketSkuDeletedOnly"
        };

        for (size_t i = 1; i <= OfferIds.size(); ++i) {
            if (i == OfferIds.at("Added")) {
                indexWriter->AddOffer(MakeDefaultBaseOffer(i));
                continue;
            }

            //O(marketSKUOfferNames.size() * OfferIds.size()), но данных совсем немного, для теста сгодится.
            bool isMsku = false;
            for (const auto& n: marketSKUOfferNames) {
                if (i == OfferIds.at(n)) {
                    isMsku = true;
                    break;
                }
            }

            if (isMsku) {
                auto offer = MakeDefaultBaseOfferWithVat(i, EVat::VAT_0);
                offer.MarketSku = i * 100;
                indexWriter->AddOffer(offer);
                continue;
            }

            indexWriter->AddOffer(MakeDefaultBaseOfferWithVat(i, EVat::VAT_0));
        }

        indexWriter->Dump();
    }

    void PrepareDeltas(const TString& deltaListPath) override
    {
        const TString deltaPath1 = JoinFsPaths(Settings.TestWorkingDir, "delta1.pbuf.sn");
        const TString deltaPath2 = JoinFsPaths(Settings.TestWorkingDir, "delta2.pbuf.sn");

        auto delta1 = BuildDelta(deltaPath1);
        auto delta2 = BuildDelta(deltaPath2);

        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Added"), TDataSource::FEED, EVat::VAT_18));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Untouched"), TDataSource::FEED, EVat::VAT_0));

        //untouched API потому что для ключа feed_id, offer_id vat не применяется
        delta1->AddOffer(MakeDefaultDeltaOffer(OfferIds.at("UntouchedApiRecordPrice"), TDataSource::API, 100));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("UntouchedApiRecordVat"), TDataSource::API, EVat::VAT_18));

        auto untouchedBadWaremd5 = MakeDefaultDeltaOfferWithVat(OfferIds.at("UntouchedBadWaremd5"), TDataSource::FEED, EVat::VAT_18);
        untouchedBadWaremd5.BinaryWareMd5 = "1234567812345678";
        delta1->AddOffer(untouchedBadWaremd5);

        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Changed18"), TDataSource::FEED, EVat::VAT_18));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Changed10"), TDataSource::FEED, EVat::VAT_10));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Changed18_118"), TDataSource::FEED, EVat::VAT_18_118));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Changed10_110"), TDataSource::FEED, EVat::VAT_10_110));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("Changed0"), TDataSource::FEED, EVat::VAT_0));
        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("ChangedNoVat"), TDataSource::FEED, EVat::NO_VAT));

        auto changedInvalid = MakeDefaultDeltaOffer(OfferIds.at("ChangedInvalid"), TDataSource::FEED, 100);
        changedInvalid.Fields.set_vat(0);
        delta1->AddOffer(changedInvalid);

        auto changedOnlyVat = MakeDefaultDeltaOffer(OfferIds.at("ChangedOnlyVat"), TDataSource::FEED, 100);
        changedOnlyVat.Fields.Clear();
        changedOnlyVat.Fields.set_vat((int)EVat::VAT_10_110);
        delta1->AddOffer(changedOnlyVat);

        delta1->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("ChangedTwoRecords"), TDataSource::FEED, EVat::VAT_18));
        delta2->AddOffer(MakeDefaultDeltaOfferWithVat(OfferIds.at("ChangedTwoRecords"), TDataSource::FEED, EVat::VAT_10_110));

        delta1->AddOffer(MakeDefaultDeltaOffer(OfferIds.at("Deleted"), TDataSource::FEED, 100));


        //MarketSkuVatOnly: API запись без цены, только VAT. Не должно обновляться
        {
            auto offer = MakeDefaultDeltaOffer(0, TDataSource::API, 0, OfferIds.at("MarketSkuVatOnly") * 100);
            offer.Fields.set_vat((int)EVat::VAT_10_110);
            delta1->AddOffer(offer);
        }

        //MarketSkuVatPrice: API запись с ценой и VAT. Должно обновляться.
        {
            delta1->AddOffer(MakeDefaultDeltaOfferWithVat(0, TDataSource::API, EVat::VAT_10_110, OfferIds.at("MarketSkuVatPrice") * 100));
        }

        //MarketSkuPriceOnly: API запись только с ценой (ват должен обнулиться)
        {
            delta1->AddOffer(MakeDefaultDeltaOffer(0, TDataSource::API, 1234, OfferIds.at("MarketSkuPriceOnly") * 100));
        }

        //MarketSkuFeedAndApi: Сначала фидовая запись с VAT, потом API-шная
        {
            auto offer1 = MakeDefaultDeltaOfferWithVat(OfferIds.at("MarketSkuFeedAndApi"), TDataSource::FEED, EVat::VAT_18);

            auto offer2 = MakeDefaultDeltaOffer(0, TDataSource::API, 1234, OfferIds.at("MarketSkuFeedAndApi") * 100);
            offer2.Fields.set_vat((int)EVat::VAT_10_110);
            offer2.Timestamp += 1;

            delta1->AddOffer(offer1);
            delta1->AddOffer(offer2);
        }

        //MarketSkuApiAndFeed: Сначала API-шная запись с VAT, потом фидовая (Проверяем, что работает приритет API>FEED)
        {
            auto offer1 = MakeDefaultDeltaOffer(0, TDataSource::API, 1234, OfferIds.at("MarketSkuApiAndFeed") * 100);
            offer1.Fields.set_vat((int)EVat::VAT_10_110);

            auto offer2 = MakeDefaultDeltaOfferWithVat(OfferIds.at("MarketSkuApiAndFeed"), TDataSource::FEED, EVat::VAT_18);
            offer2.Timestamp += 1;

            delta1->AddOffer(offer1);
            delta1->AddOffer(offer2);
        }

        //MarketSkuFeedApiAndRmApi: Фид, далее API, далее удаление цены в API - VAT должен откатиться к фиду
        {
            //feed
            auto offer1 = MakeDefaultDeltaOfferWithVat(OfferIds.at("MarketSkuFeedApiAndRmApi"), TDataSource::FEED, EVat::VAT_18);

            //api
            auto offer2 = MakeDefaultDeltaOfferWithVat(OfferIds.at("MarketSkuFeedApiAndRmApi"), TDataSource::API, EVat::VAT_10_110);
            offer2.Timestamp += 1;

            //rm api
            auto offer3 = MakeDefaultDeltaOffer(0, TDataSource::API, 0, OfferIds.at("MarketSkuFeedApiAndRmApi") * 100);
            offer3.Fields.set_price_deleted(true);
            offer3.Timestamp += 2;

            delta1->AddOffer(offer1);
            delta1->AddOffer(offer2);
            delta1->AddOffer(offer3);
        }

        //MarketSkuDeletedOnly - пришло offer_deleted() = false от market_sku, VAT-ы не должны измениться или обнулиться
        {
            auto offer1 = MakeDefaultDeltaOffer(0, TDataSource::API, 0, OfferIds.at("MarketSkuDeletedOnly") * 100);
            offer1.Fields.set_offer_deleted(false);

            delta1->AddOffer(offer1);
        }

        delta1->Dump();
        delta2->Dump();
        WriteDeltasListFile(deltaListPath, {deltaPath1, deltaPath2}, true);
    }

    TUpdaterSettings MakeUpdaterSettings(const TTestIndexPaths& indexPaths, const TString& deltaListPath) override
    {
        TUpdaterSettings updaterSettings;

        updaterSettings.Entity = "vat";
        updaterSettings.InstructionPath = indexPaths.Instruction;
        updaterSettings.IgnoreInstructionPath = indexPaths.IgnoreInstruction;
        updaterSettings.DeltaListPath = deltaListPath;
        updaterSettings.InputDataPath = indexPaths.VatPath;
        updaterSettings.OutputDataPath = indexPaths.VatPath;
        updaterSettings.FeedSessionsPath = indexPaths.AllowedSessions;
        updaterSettings.SkuPositionsPath = indexPaths.SkuPositions;
        updaterSettings.IgnoreSkuPositionsPath = indexPaths.IgnoreSkuPositions;
        updaterSettings.IgnoreApi = false;

        updaterSettings.PricesMetaPath = "";
        updaterSettings.CurrencyRates = "";
        updaterSettings.WareMD5Path = indexPaths.WareMd5;

        return updaterSettings;
    }

    void AssertResults(const TTestIndexPaths& indexPaths) override
    {
        auto reader = BuildTestIndexReader(indexPaths);

        AssertVatUntouched(reader, OfferIds.at("Untouched"));
        //untouched API потому что для ключа feed_id, offer_id vat не применяется
        AssertVatUntouched(reader, OfferIds.at("UntouchedApiRecordPrice"));
        AssertVatUntouched(reader, OfferIds.at("UntouchedApiRecordVat"));
        AssertVatUntouched(reader, OfferIds.at("UntouchedBadWaremd5"));
        AssertVatUntouched(reader, OfferIds.at("UntouchedBadWaremd5"));
        AssertVatUntouched(reader, OfferIds.at("MarketSkuDeletedOnly"));
        AssertVatUntouched(reader, OfferIds.at("MarketSkuVatOnly"));

        AssertVatValue(reader, OfferIds.at("Added"), EVat::VAT_20);
        AssertVatValue(reader, OfferIds.at("Changed18"), EVat::VAT_20);
        AssertVatValue(reader, OfferIds.at("Changed10"), EVat::VAT_10);
        AssertVatValue(reader, OfferIds.at("Changed18_118"), EVat::VAT_20_120);
        AssertVatValue(reader, OfferIds.at("Changed10_110"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("Changed0"), EVat::VAT_0);
        AssertVatValue(reader, OfferIds.at("ChangedNoVat"), EVat::NO_VAT);
        AssertVatValue(reader, OfferIds.at("MarketSkuVatPrice"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("MarketSkuFeedAndApi"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("MarketSkuApiAndFeed"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("ChangedOnlyVat"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("ChangedTwoRecords"), EVat::VAT_10_110);
        AssertVatValue(reader, OfferIds.at("MarketSkuFeedApiAndRmApi"), EVat::VAT_20);

        AssertVatUndefined(reader, OfferIds.at("ChangedInvalid"));
        AssertVatUndefined(reader, OfferIds.at("Deleted"));
        AssertVatUndefined(reader, OfferIds.at("MarketSkuPriceOnly"));
    }

    TString Name() const override
    {
        return "TVatTest";
    }

public:
    TVatTest(const TTestSettings& settings)
        : TQPipeUpdaterTest(settings)
    {}
};

THolder<TQPipeUpdaterTest> BuildVatTest(const TTestSettings& settings)
{
    return MakeHolder<TVatTest>(settings);
}


}  // namespace NQPipe
}  // namespace NMarket
