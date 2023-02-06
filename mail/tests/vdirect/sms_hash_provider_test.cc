#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/vdirect/hash_provider.h>

namespace {
using namespace testing;

struct SmsHashProviderTest : public Test {
    SmsHashProviderTest() : provider(storage,"MMMMM"){
        storage.addKey("a", "abrakadabra", true);
        storage.addKey("b", "zopazopazopa");
    }
    vdirect::KeysStorage storage;
    vdirect::SmsHashProvider provider;
};

TEST_F( SmsHashProviderTest, hash_withUrlAndKeyName_returnsHashWithDefaultKey ) {
    ASSERT_EQ(provider.hash("http://some.url.com?param=1&bar=foo"),
            "a,MMMMM,4Oax4HZJ4hGtVMVhx_oazQ");
}

}
