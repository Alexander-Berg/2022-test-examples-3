#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/vdirect/hash_provider.h>

namespace {
using namespace testing;

struct UidHashProviderTest : public Test {
    UidHashProviderTest() : provider(storage,"666"){
        storage.addKey("a", "abrakadabra", true);
        storage.addKey("b", "zopazopazopa");
    }
    vdirect::KeysStorage storage;
    vdirect::UidHashProvider provider;
};

TEST_F( UidHashProviderTest, hash_withUrlAndKeyName_returnsHashWithDefaultKey ) {
    ASSERT_EQ(provider.hash("http://some.url.com?param=1&bar=foo"),
            "a,4MbeU1KiBpof6qJMmdBMlQ");
}

}
