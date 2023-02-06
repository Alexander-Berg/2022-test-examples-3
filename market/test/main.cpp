#include <library/cpp/testing/unittest/env.h>

#include <util/system/env.h>


namespace {
    struct TInit {
        inline TInit() {
            chdir(SRC_("").data());
            SetEnv("YANDEX_CONFIG", "etc/marketcataloger.cfg");
        }
    } INITER;
}
