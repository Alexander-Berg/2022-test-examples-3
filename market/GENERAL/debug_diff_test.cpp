#include "tests.h"

#include <market/qpipe/qindex/test_util/common.h>
#include <util/generic/yexception.h>

namespace NMarket {
namespace NQPipe {

class TDebugDiffTest : public TQPipeUpdaterTest
{
protected:
    void PrepareIndex(const TTestIndexPaths& indexPaths) override
    {
        const size_t Cnt = 200;

        TTestIndexPaths indexPaths1 = indexPaths;
        auto indexWriter1 = BuildTestIndexWriter(indexPaths1);

        TTestIndexPaths indexPaths2 = indexPaths;
        auto indexWriter2 = BuildTestIndexWriter(indexPaths2);

        for (size_t i = 1; i <= Cnt; ++i) {
            auto offer1 = MakeDefaultBaseOffer(i);
            indexWriter1->AddOffer(offer1);

            auto offer2 = MakeDefaultBaseOffer(i);
            indexWriter2->AddOffer(offer2);
        }

        indexWriter1->Dump();
        indexWriter2->Dump();
    }

    void PrepareDeltas(const TString& /*deltaListPath*/) override
    {
        //Nothing
    }

    TUpdaterSettings MakeUpdaterSettings(const TTestIndexPaths& /*indexPaths*/, const TString& /*deltaListPath*/) override
    {
        //Nothing
        return TUpdaterSettings();
    }

    void RunUpdater(const TTestIndexPaths& indexPaths, const TString& /*deltaListPath*/) override
    {
        TList<TString> args;
        args.push_back("--debug-diff");
        args.push_back("--bad-records");          args.push_back("5");
        args.push_back("--instruction");          args.push_back(indexPaths.Instruction);

        RunCmd(Settings.UpdaterBinary, args);
    }

    void AssertResults(const TTestIndexPaths& /*indexPaths*/) override
    {
        //Nothing
    }

    TString Name() const override
    {
        return "TDebugDiffTest";
    }

public:
    TDebugDiffTest(const TTestSettings& settings)
        : TQPipeUpdaterTest(settings)
    {}
};

THolder<TQPipeUpdaterTest> BuildDebugDiffTest(const TTestSettings& settings)
{
    return MakeHolder<TDebugDiffTest>(settings);
}

}  // namespace NQPipe
}  // namespace NMarket
