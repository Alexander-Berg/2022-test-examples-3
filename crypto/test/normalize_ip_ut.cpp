#include <crypta/graph/rtmr/lib/common/normalize_ip.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NormalizeIp) {
    using namespace NCrypta::NGraph;

    Y_UNIT_TEST(NormalizeIp) {
        const TString v4{"1.2.3.4"};
        const TString v4inv6{"::ffff:1.2.3.4"};
        const TString v6{"2a02::da82:1"};

        UNIT_ASSERT_EQUAL(NormalizeIp(v4), TString{v4});
        UNIT_ASSERT_EQUAL(NormalizeIp(v4inv6), TString{v4});
        UNIT_ASSERT_EQUAL(NormalizeIp(v6), TString{v6});
    }
}
