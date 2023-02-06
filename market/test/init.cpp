#include <library/cpp/testing/unittest/env.h>
#include <util/system/env.h>

namespace {
    struct TIniter {
        inline TIniter() {
            SetEnv("REPORT_CONFIG", ArcadiaSourceRoot() + "/market/report/test/test-report.cfg");
        }
    } INITER;
}
