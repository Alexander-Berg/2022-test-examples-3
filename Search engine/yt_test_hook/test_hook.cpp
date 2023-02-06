#include <library/cpp/testing/hook/hook.h>

#include <yt/yt/core/misc/shutdown.h>

Y_TEST_HOOK_AFTER_RUN(ShutdownYt) {
    NYT::Shutdown();
}
