#pragma once
#include <market/library/shiny/server/env.h>
#include <market/library/shiny/server/gen/lib/tests/app/proto/config.pb.h>

namespace NMarket::NApp {
    /// Global environment of your service
    struct TEnv {
        using TConfig = TConfig;

        TEnv(const NShiny::TInitContext<TConfig>&);
    };
}
