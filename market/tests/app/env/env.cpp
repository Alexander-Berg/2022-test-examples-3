#include <market/library/shiny/server/gen/lib/tests/app/env/env.h>

/// Global environment of your service
NMarket::NApp::TEnv::TEnv(const NMarket::NShiny::TInitContext<TConfig>&) {
    /// Here you can init your own services, which can be accessible on every http handler
    /// for example: PGaaS database, or custom logger
}
