#pragma once
#include <market/library/shiny/server/env.h>
#include <market/sre/services/cpp_test_service/proto/config.pb.h>

namespace NMarket::CppTestService {
    /// Global environment of your service
    struct TEnv {
        using TConfig = TConfig;

        TEnv(const NShiny::TInitContext<TConfig>&);
    };
}
