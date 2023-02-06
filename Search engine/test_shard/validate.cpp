#include "validate.h"

#include "gunslinger.h"
#include "options.h"

namespace NTestShard {

int Validate(TOptions& opts) {
    TGunslinger killer(opts);
    killer.Start();
    killer.Join();
    killer.PrintTimes();
    killer.Validate();
    return EXIT_SUCCESS;
}

}
