#include "kill_basesearch.h"

#include "gunslinger.h"
#include "options.h"

namespace NTestShard {

int KillBasesearch(TOptions& opts) {
    TGunslinger killer(opts);
    for (ui32 i = 0; i < 100; ++i) {
        killer.Start();
        killer.Join();
        killer.PrintTimes();
        killer.MutateQueries();
    }
    killer.PrintTimes();
    return EXIT_SUCCESS;
}

}
