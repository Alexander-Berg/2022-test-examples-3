#include <library/cpp/testing/unittest/registar.h>
#include <crypta/graph/rtdi-rt/lib/browserinfo.h>

Y_UNIT_TEST_SUITE(TestBrowserInfo) {
    Y_UNIT_TEST(TEST_Smoke) {
        const TString testInput{"x:100:y:hasthisvalue:t:some content:nosuchkey:"};
        TBrowserInfo browserInfo{testInput};

        UNIT_ASSERT(browserInfo["nosuchkey"] == "");
        UNIT_ASSERT(browserInfo["hasthisvalue"] == "");
        UNIT_ASSERT(browserInfo["x"] == "100");
        UNIT_ASSERT(browserInfo["y"] == "hasthisvalue");
    }
}
