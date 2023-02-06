#pragma once

#include <market/kombat/env/env.h>
#include <market/library/shiny/server/request.h>
#include <market/library/shiny/server/response.h>

namespace NMarket::NKombat {
    class TTestExpBatch {
    public:
        struct TRequest {
            static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

            TString Ticket;
            TString Owner;
            TString ExperimentTicket;
            TMaybe<TString> TelegramGroupToNotify;
            TVector<TString> RearrFactors;
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
        explicit TTestExpBatch(TEnv& env)
            : Executor(*env.Executor)
            , DataModel(*env.BattleDataModel)
        {
        }

        TResponse Run(const TRequest& request) const;

    private:
        TBattleId RunDegrad(const TRequest& request, TBattleSpec::EType reportType, bool maxRps, const TMaybe<TString>& ammoFilter = Nothing()) const;

    private:
        IExecutor& Executor;
        IDataModel& DataModel;
    };
}
