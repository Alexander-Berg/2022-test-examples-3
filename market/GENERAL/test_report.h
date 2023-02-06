#pragma once
#include <market/kombat/env/env.h>
#include <market/kombat/engine/executor.h>
#include <market/library/shiny/server/request.h>
#include <market/library/shiny/server/response.h>
#include <market/library/shiny/external/telegram/telegram.h>

namespace NMarket {
    namespace NKombat {
        class TTestReport {
        public:
            struct TRequest {
                static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

                TString Version;
                TString Ticket;
                TBattleSpec::EType FireType;
                TMaybe<TString> BaseVersion;
                bool UseStableBaseReport;
                bool UseStableMetaReport;
                bool MaxRps = false;
                TMaybe<ui32> AutoRepeatLimit;
                bool AllRevisions = false;
                TMaybe<TString> AmmoFilter;
                TString Owner;
                TMaybe<TString> TelegramGroup;
                bool OnlyScenarios = false;
            };

            struct TResponse {
                NSc::TValue AsJson() const {
                    NSc::TValue result;
                    result["battle_id"] = Id;
                    return result;
                }

                TString Id;
            };

            static TStringBuf Describe();

            template <typename TEnv>
            explicit TTestReport(TEnv& env)
                : Executor(*env.Executor)
                , BaseReports(*env.BaseReports)
                , MetaReports(*env.MetaReports)
                , FireCount(env.Statistics.RegisterAccumulatingMetric("test_report_release")) {
            }

            TResponse Run(const TRequest& request) const;

        private:
            IExecutor& Executor;
            IReportStorage& BaseReports;
            IReportStorage& MetaReports;
            NShiny::IMetric& FireCount;
        };
    }
}
