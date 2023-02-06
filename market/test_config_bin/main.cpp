#include <market/idx/datacamp/controllers/lib/utils/validate_config.h>

#include <market/library/common_proxy/lib/controllers/its_controller.h>
#include <market/library/signal_handler/Breakpad.h>
#include <market/library/libyt/YtHelpers.h>

#include <kernel/common_proxy/server/server.h>
#include <kernel/daemon/daemon.h>

#include <util/generic/hash_set.h>
#include <util/generic/maybe.h>

int main(int argc, char* argv[]) {
    NCommonProxy::WriteEnvConfig(argc, const_cast<const char**>(argv));

    NMarket::NSignalHandler::AddSignalHandler(
            {SIGSEGV, SIGILL, SIGBUS, SIGSYS, SIGFPE, SIGPIPE},
            NMarket::NSignalHandler::FaultSignalHandler);

    NYT::JoblessInitialize();

    try {
        ValidateConfig(argc, argv,
                       {"YT_STATIC_TABLE_READER", "INPUT_GATEWAY", "LOGBROKER_READER"},
                       {"UNITED_UPDATER", "SUBSCRIPTION_DISPATCHER", "MSKU_STORAGE_UPDATER"},
                       {"Initializer", "DatacampLogInitializer", "ActualServiceOffersTableCreator", "BasicOffersTableCreator",
                        "FreshBlueOffersStorageCreator", "FreshWhiteOffersStorageCreator", "ServiceOffersTableCreator"});
    } catch (const yexception& e) {
        FATAL_LOG << "Invalid config: " << e << Endl;
        return EXIT_FAILURE;
    }

    return Singleton<TDaemon<NCommonProxy::TServer, NCommonProxy::TItsController> >()->Run(argc, argv, true);
}
