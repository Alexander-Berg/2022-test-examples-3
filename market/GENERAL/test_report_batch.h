#pragma once
#include <market/kombat/env/env.h>
#include <market/kombat/engine/executor.h>
#include <market/kombat/handler/test_report.h>
#include <market/library/shiny/server/request.h>
#include <market/library/shiny/server/response.h>
#include <market/library/shiny/external/telegram/telegram.h>

namespace NMarket::NKombat {
    class TTestReportBatch {
    public:
        struct TRequest {
            static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

            TString Version;
            TString Ticket;
            TMaybe<TString> BaseVersion;
            TString Owner;
            TMaybe<bool> UseStableBaseReport;
            TMaybe<bool> UseStableMetaReport;
        };

        struct TResponse {
            NSc::TValue AsJson() const {
                NSc::TValue result;
                auto& battles = result.GetArrayMutable();
                for (const auto& info : Battles) {
                    auto& battle = battles.emplace_back().GetDictMutable();
                    battle["id"] = info.Id;
                    battle["name"] = info.Name;
                    battle["maxrps"].SetBool(info.MaxRps);
                    battle["only-scenarios"].SetBool(info.OnlyScenarios);
                }

                return result;
            }

            struct TBattleInfo {
                TString Id;
                TString Name;
                bool MaxRps = false;
                bool OnlyScenarios = false;
            };

            TVector<TBattleInfo> Battles;
        };

        static TStringBuf Describe();

        template <typename TEnv>
        explicit TTestReportBatch(TEnv& env)
            : BattleRunner(env)
        {
        }

        TResponse Run(const TRequest& request) const;

    private:
        TResponse::TBattleInfo RunDegrad(
            const TRequest& request,
            TBattleSpec::EType reportType,
            const TMaybe<TString>& ammoFilter = Nothing(),
            bool maxRps = false,
            bool onlyScenarios = false
        ) const;

    private:
        TTestReport BattleRunner;
    };
}
