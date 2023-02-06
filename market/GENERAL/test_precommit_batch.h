#pragma once

#include <market/kombat/env/env.h>
#include <market/library/shiny/server/request.h>
#include <market/library/shiny/server/response.h>

namespace NMarket::NKombat {
    class TTestPrecommitBatch {
    public:
        struct TRequest {
            static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

            TString Owner;
            TString Ticket;
            TString BaseRevision;
            ui32 PullRequestId;
            ui32 Priority = 100;
        };

        struct TResponse {
            NSc::TValue AsJson() const {
                NSc::TValue result;
                auto& battles = result.GetArrayMutable();
                battles.AppendAll(std::begin(Battles), std::end(Battles));
                return result;
            }

            TVector<TString> Battles;
        };

        static TStringBuf Describe();

        template <typename TEnv>
        explicit TTestPrecommitBatch(TEnv& env)
            : Executor(*env.Executor)
        {
        }

        TResponse Run(const TRequest& request) const;

    private:
        TBattleId RunDegrad(
            const TRequest& request,
            TBattleSpec::EType reportType,
            const TMaybe<TString>& ammoFilter = Nothing()
        ) const;

    private:
        IExecutor& Executor;
    };
}
