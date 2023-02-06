#include <market/library/shiny/server/gen/lib/tests/app/env/env.h>
#include <market/library/shiny/server/gen/lib/tests/app/handler/hello.h>
#include <market/library/shiny/server/daemon.h>

using namespace NMarket::NApp;

int main(int argc, char* argv[]) {
    auto daemon = NMarket::NShiny::CreateDaemon<TEnv>(argc, argv);
    daemon->Handle<THello>("/hello");   /// registering hello handler in shiny
    return daemon->Run();
}
