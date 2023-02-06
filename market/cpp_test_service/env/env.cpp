#include <market/sre/services/cpp_test_service/env/env.h>

/// Global environment of your service
NMarket::CppTestService::TEnv::TEnv(const NMarket::NShiny::TInitContext<TConfig>&) {
    /// Here you can init your own services, which can be accessible on every http handler
    /// for example: PGaaS database, or custom logger
}
