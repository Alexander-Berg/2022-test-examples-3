#include <market/sre/services/cpp_test_service/env/env.h>
#include <market/sre/services/cpp_test_service/handler/hello.h>
#include <market/library/shiny/server/daemon.h>

using namespace NMarket::CppTestService;

int main(int argc, char* argv[]) {
    auto daemon = NMarket::NShiny::CreateDaemon<TEnv>(argc, argv);
    daemon->Handle<THello>("/hello");   /// registering hello handler in shiny
    return daemon->Run();
}
