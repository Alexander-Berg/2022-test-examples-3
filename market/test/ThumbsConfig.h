#pragma once

#include <util/stream/file.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

namespace NPrivate {
    constexpr unsigned TH_50x50 = 1 << 0;
    constexpr unsigned TH_100x100 = 1 << 6;
    constexpr unsigned TH_300x300 = 1 << 13;
    constexpr unsigned TH_1x1 = 1 << 18;

    TString GetFakeThumbsConfig() {
        auto path = SRC_("./data/picrobot_thumbs.meta");
        return TUnbufferedFileInput(path.data()).ReadAll();
    }
}
