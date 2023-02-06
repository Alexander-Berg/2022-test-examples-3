#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/vdirect/hash_provider.h>

namespace {
using namespace testing;

struct UidHashValidatorTest : public Test {
    UidHashValidatorTest() : validator(storage,"666"){
        storage.addKey("a", "abrakadabra", true);
        storage.addKey("b", "zopazopazopa");
    }
    vdirect::KeysStorage storage;
    vdirect::UidHashProvider validator;
};

TEST_F( UidHashValidatorTest, valid_withUrlAndRightHash_returnsTrue ) {
    ASSERT_TRUE(validator.valid("http://some.url.com?param=1&bar=foo", "a,4MbeU1KiBpof6qJMmdBMlQ") );
}

TEST_F( UidHashValidatorTest, valid_withUrlAndBadHash_returnsFalse ) {
    ASSERT_FALSE(validator.valid("http://some.url.com?param=1&bar=foo", "a,BADHASH") );
}

TEST_F( UidHashValidatorTest, valid_withUrlAndBadKeyHash_throwsOutOfRange ) {
    EXPECT_THROW(validator.valid("http://some.url.com?param=1&bar=foo", "BADKEYHASH"), std::out_of_range );
}

}
