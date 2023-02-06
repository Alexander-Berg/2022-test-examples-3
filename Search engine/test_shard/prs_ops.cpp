#include "prs_ops.h"

#include <quality/query_pool/prs_ops/lib/tools.h>
#include <quality/query_pool/prs_ops/lib/simple_tasks.h>

#include <library/cpp/regex/pcre/regexp.h>

namespace NTestShard {

class TArgsBuilder {
public:
    using TCharPtr = const char*;

    TArgsBuilder() {
        Push("");
    };

    void Push(const TString& str) {
        Buffers_.push_back(str);
        Argv_.push_back(Buffers_.back().data());
    }

    void Push(const TString& key, const TString& value) {
        Push(key);
        Push(value);
    }

    TCharPtr* Data() {
        return Argv_.data();
    }

    int Size() const {
        return Buffers_.size();
    }

private:
    TVector<TString> Buffers_;
    TVector<TCharPtr> Argv_;
};

TPRSSettings FillSettings(const TPrsOptions& opts) {
    TArgsBuilder builder;
    if (opts.Stage & EStage::Search) {
        builder.Push("--save-search-sub-requests");
    }
    if (opts.Stage & EStage::Factor) {
        builder.Push("--save-factor-sub-requests");
    }
    builder.Push("--write-empty-sub-requests");
    builder.Push("--no-features");
    builder.Push("--nproc", "50");
    builder.Push("--wizard-num-threads", "50");
    builder.Push("--additional-params", opts.MiddleParams);
    builder.Push("--wizard-retries-count", "10");
    builder.Push("-i", opts.QueriesFile);
    builder.Push("-u", opts.Upper);

    TPRSSettings settings;
    ParseOptions(builder.Size(), builder.Data(), settings);
    FinalizeSettings(settings);
    TuneSettings(settings);
    settings.SubSourceRequestsOutputByTier = opts.Output;
    return settings;
}

bool IsTemporaryFailMessage(const TString& message) {
    TVector<TRegExMatch> temporaryRegEx = {
        R"(^(\(NYT::TErrorResponse\) 'Access denied: cluster is in safe mode. Check for the announces before reporting any issues')(.*)$)",
        R"(^(proxy )(.*)( is unavailable)$)",
        R"(^(\(TNetworkResolutionError\) Temporary failure in name)(.*)(can not resolve)(.*)$)",
        R"(^\(Transaction\)(.*)(has expired or was aborted at cell)(.*)$)",
        R"(^\(TSystemError\) \(Connection timed out\)(.*)(can not resolve)(.*)$)"
    };
    for (const TRegExMatch& curRegEx : temporaryRegEx) {
        if (curRegEx.Match(message.data())) {
            return true;
        }
    }
    return false;
}

int RunPRS(const TPrsOptions& opts) {
    InitGlobalLog2Console();
    if (opts.SkipPrs) {
        INFO_LOG << "Skipped prs stage" << Endl;
        return EXIT_SUCCESS;
    }
    TPRSSettings settings = FillSettings(opts);
    TRetryCounter<10> temporaryFailCounter;
    while (true) {
        try {
            for (size_t runIndex = 0; runIndex < settings.PRSDownloadCount; ++runIndex) {
                ComboMode(settings);
            }
            break;
        } catch(yexception& e) {
            const TString& msg = CurrentExceptionMessage();
            if (!IsTemporaryFailMessage(msg)) {
                throw;
            }
            if (temporaryFailCounter.IsMaximumReached()) {
                FATAL_LOG << "Fatal fail: " << CurrentExceptionMessage() << Endl;
                return EXIT_FAILURE;
            } else {
                WARNING_LOG << "Fail: " << CurrentExceptionMessage() << Endl;
            }
            Sleep(temporaryFailCounter.NextSleepDuration());
        }
    }
    return EXIT_SUCCESS;
}

}
