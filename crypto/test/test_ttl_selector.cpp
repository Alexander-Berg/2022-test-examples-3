#include <crypta/cm/services/mutator/lib/config/ttl_config.pb.h>
#include <crypta/cm/services/mutator/lib/handlers/ttl_selector.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

using namespace NCrypta::NCm::NMutator;

namespace {
    const TString TAG_CUSTOM = "custom";
    const auto& TTL_DEFAULT = TDuration::Seconds(10 * 86400);
    const auto& TTL_EXTENDED = TTL_DEFAULT * 3;
    const auto& TTL_CUSTOM = TDuration::Seconds(100);

    TTtlConfig GetTtlConfig() {
        TTtlConfig result;
        result.SetDefaultTtl(TTL_DEFAULT.Seconds());
        result.SetExtendedTtl(TTL_EXTENDED.Seconds());
        result.MutableCustomTagTtls()->insert({TAG_CUSTOM, TTL_CUSTOM.Seconds()});

        return result;
    }
}

TEST(TTtlSelector, All) {
    const auto& ttlConfig = GetTtlConfig();
    const TTtlSelector ttlSelector(ttlConfig);

    const TString tag = "foo";
    ASSERT_EQ(TTL_DEFAULT, ttlSelector.GetDefaultTtl(tag));
    ASSERT_EQ(TTL_EXTENDED, ttlSelector.GetExtendedTtl(tag));

    ASSERT_EQ(TTL_CUSTOM, ttlSelector.GetDefaultTtl(TAG_CUSTOM));
    ASSERT_EQ(TTL_CUSTOM, ttlSelector.GetExtendedTtl(TAG_CUSTOM));
}
