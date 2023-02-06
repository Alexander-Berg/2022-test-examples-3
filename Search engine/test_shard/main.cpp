#include "generate_requests.h"
#include "kill_basesearch.h"
#include "options.h"
#include "save_requests.h"
#include "validate.h"

int main(int argc, const char* argv[]) {
    using namespace NTestShard;

    try {
        TOptions opts(argc, argv);

        switch (opts.Mode) {
        case EMode::Default:
            opts.PrintHelp();
            break;
        case EMode::SaveRequests:
            return SaveRequests(opts);
        case EMode::GenerateRequests:
            return PrintRequests(opts);
        case EMode::KillBasesearch:
            return KillBasesearch(opts);
        case EMode::Validate:
            return Validate(opts);
        }
        return EXIT_SUCCESS;
    } catch (...) {
        FATAL_LOG << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }
}
