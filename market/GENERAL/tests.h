#pragma once

#include "index.h"
#include "delta.h"

#include "../common_types.h"

#include <util/generic/bt_exception.h>
#include <util/folder/path.h>

#include <market/qpipe/qindex/test_util/common.h>

namespace NMarket {
namespace NQPipe {

struct TTestSettings
{
    TString UpdaterBinary;
    TString CommonDataDir;
    TString TestWorkingDir;
};

class TQPipeUpdaterTest
{
protected:
    const TTestSettings Settings;

    const TFeedId DefaultFeedId = 1069;
    const TTimestamp DefaultTimestamp = 1600000000;
    const TString DefaultWaremd5 = "0123456789123456";

protected:
    template <typename T>
    void DoAssert(const T& expected, const T& fact, const TString& comment) const
    {
        if (expected != fact)
            ythrow TWithBackTrace<yexception>() << "Assert failed (" << comment << "). Expected: " << expected << " Fact: " << fact;
    }

    TTestIndexOffer MakeDefaultBaseOffer(uint64_t offerId) const
    {
        TTestIndexOffer res;
        res.FeedId = DefaultFeedId;
        res.OfferId = offerId;
        res.WareMd5 = DefaultWaremd5;
        res.Price = offerId * 10000000;
        res.CurrencyId = 2; //RUR
        return res;
    }

    NMarket::NQPipe::TInputOffer MakeDefaultDeltaOffer(uint64_t offerId,
                                                       TDataSource dataSource,
                                                       uint64_t price,
                                                       TMarketSku marketSku = 0,
                                                       TVersion version = 0,
                                                       TMarketColor color = TMarketColor::WHITE) const
    {
        NMarket::NQPipe::TInputOffer res;
        res.FeedId = DefaultFeedId;
        res.OfferId = ToString(offerId);
        res.MarketSku = marketSku;
        res.DataSource = dataSource;
        res.EntityType = TEntityType::PRICES;
        res.Version = version;
        res.MarketColor = color;
        if (price != 0) {
            res.Fields.mutable_binary_price()->set_price(price);
        }
        res.Timestamp = DefaultTimestamp;

        if (dataSource == TDataSource::FEED)
            res.BinaryWareMd5 = DefaultWaremd5;

        return res;
    }

    virtual void RunUpdater(const TTestIndexPaths& indexPaths, const TString& deltaListPath)
    {
        TUpdaterSettings updaterSettings = MakeUpdaterSettings(indexPaths, deltaListPath);

        TList<TString> args;
        args.push_back("--entity");              args.push_back(updaterSettings.Entity);
        args.push_back("--instruction");         args.push_back(updaterSettings.InstructionPath);
        args.push_back("--input-deltas-list");   args.push_back(updaterSettings.DeltaListPath);
        args.push_back("--input-data");          args.push_back(updaterSettings.InputDataPath);
        args.push_back("--output-data");         args.push_back(updaterSettings.OutputDataPath);

        if (updaterSettings.SkuPositionsPath) {
            args.push_back("--sku-positions");
            args.push_back(updaterSettings.SkuPositionsPath);
        }

        if (updaterSettings.IgnoreSkuPositionsPath) {
            args.push_back("--ignore-sku-positions");
            args.push_back(updaterSettings.IgnoreSkuPositionsPath);
        }

        if (updaterSettings.IgnoreInstructionPath) {
            args.push_back("--ignore-instruction");
            args.push_back(updaterSettings.IgnoreInstructionPath);
        }

        if (updaterSettings.FeedSessionsPath) {
            args.push_back("--feed-sessions");
            args.push_back(updaterSettings.FeedSessionsPath);
        }

        if (updaterSettings.IgnoreApi) {
            args.push_back("--no-api");
        }

        if (updaterSettings.PricesMetaPath) {
            args.push_back("--prices-meta");
            args.push_back(updaterSettings.PricesMetaPath);
        }

        if (updaterSettings.CurrencyRates) {
            args.push_back("--currency-rates");
            args.push_back(updaterSettings.CurrencyRates);
        }

        if (updaterSettings.WareMD5Path) {
            args.push_back("--ware-md5");
            args.push_back(updaterSettings.WareMD5Path);
        }

        RunCmd(Settings.UpdaterBinary, args);
    }

protected:
    virtual TString Name() const = 0;
    virtual void PrepareIndex(const TTestIndexPaths& indexPaths) = 0;
    virtual void PrepareDeltas(const TString& deltaListPath) = 0;
    virtual TUpdaterSettings MakeUpdaterSettings(const TTestIndexPaths& indexPaths, const TString& deltaListPath) = 0;
    virtual void AssertResults(const TTestIndexPaths& indexPaths) = 0;

public:
    TQPipeUpdaterTest(const TTestSettings& settings)
        : Settings(settings)
    {}

    virtual ~TQPipeUpdaterTest() = default;

    void Test()
    {
        Cout << "\n\n===================================================" << Endl;
        Cout << "Starting test: " << Name() << Endl;

        TTestIndexPaths indexPaths(Settings.TestWorkingDir, Settings.CommonDataDir);
        const TString deltaListPath = JoinFsPaths(Settings.TestWorkingDir, "deltas.list");

        Cout << "prepare index..." << Endl;
        PrepareIndex(indexPaths);

        Cout << "prepare deltas..." << Endl;
        PrepareDeltas(deltaListPath);

        Cout << "run updater..." << Endl;
        RunUpdater(indexPaths, deltaListPath);

        Cout << "assert result..." << Endl;
        AssertResults(indexPaths);

        Cout << "Success test: " << Name() << Endl;
    }

};

THolder<TQPipeUpdaterTest> BuildBookNowTest(const TTestSettings& settings);
THolder<TQPipeUpdaterTest> BuildVatTest(const TTestSettings& settings);
THolder<TQPipeUpdaterTest> BuildDebugDiffTest(const TTestSettings& settings);

}  // namespace NQPipe
}  // namespace NMarket
