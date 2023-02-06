#pragma once
#include <market/kombat/env/env.h>
#include <market/kombat/engine/executor.h>
#include <market/library/shiny/server/request.h>
#include <market/library/shiny/server/response.h>

namespace NMarket {
    namespace NKombat {
        class TTestReportResult {
        public:
            struct TRequest {
                static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

                TString BattleId;
            };

            struct TResponse {
                NSc::TValue AsJson() const;

                TString Status;
                ui32 Progress = 0;
                TString Error;
                TString BaseVersion;
                TString TestVersion;
                TString FireType;
                TMaybe<TString> AmmoFilter;
                TString KolmogorovSmirnovResult = "no_data";
                bool IsRetry = false;
                TVector<TString> DegradationReasons;
                bool Success = false;
                TMaybe<TBattleId> NextBattleId;
            };

            static TStringBuf Describe();

            template <typename TEnv>
            explicit TTestReportResult(TEnv& env)
                : Battles(*env.Battles) {
            }

            TResponse Run(const TRequest& request) const;

        private:
            IBattleStorage& Battles;
        };
    }
}
