#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/vdirect/hash_provider.h>

namespace {
using namespace testing;

class SmsHashValodatorMock : public vdirect::SmsHashValidator {
public:
    SmsHashValodatorMock(const vdirect::KeysStorage & keys, std::time_t linkTtl )
    : SmsHashValidator( keys, linkTtl ){}
    MOCK_METHOD(bool, validSmsSid, (const std::string &) , (const, override));
};

struct SmsHashValidatorTest : public Test {
    SmsHashValidatorTest() : validator(storage, 666){
        storage.addKey("a", "abrakadabra", true);
        storage.addKey("b", "zopazopazopa");
    }
    vdirect::KeysStorage storage;
    SmsHashValodatorMock validator;
};

TEST_F( SmsHashValidatorTest, valid_withUrlRightHash_returnsTrue ) {
    EXPECT_CALL( validator, validSmsSid("MMMMM") ).WillOnce(Return(true));
    ASSERT_TRUE(validator.valid("http://some.url.com?param=1&bar=foo", "a,MMMMM,4Oax4HZJ4hGtVMVhx_oazQ") );
}

TEST_F( SmsHashValidatorTest, valid_withOutdatedTtl_returnsFalse ) {
    EXPECT_CALL( validator, validSmsSid("MMMMM") ).WillOnce(Return(false));
    ASSERT_FALSE(validator.valid("http://some.url.com?param=1&bar=foo", "a,MMMMM,4Oax4HZJ4hGtVMVhx_oazQ") );
}

TEST_F( SmsHashValidatorTest, valid_withBadHashFormat_throwsOutOfRange ) {
    EXPECT_THROW(validator.valid("http://some.url.com?param=1&bar=foo", "BAD_FORMAT"), std::out_of_range );
}

TEST_F( SmsHashValidatorTest, valid_withBadHashKey_throwsOutOfRange ) {
    EXPECT_CALL( validator, validSmsSid(_) ).WillOnce(Return(true));
    EXPECT_THROW(validator.valid("http://some.url.com?param=1&bar=foo", "badKey,hash,smsHash"), std::out_of_range );
}

TEST_F( SmsHashValidatorTest, valid_withBadHash_returnsFalse ) {
    EXPECT_CALL( validator, validSmsSid(_) ).WillOnce(Return(true));
    ASSERT_FALSE(validator.valid("http://some.url.com?param=1&bar=foo", "a,MMMMM,BAD_HASH") );
}

}
