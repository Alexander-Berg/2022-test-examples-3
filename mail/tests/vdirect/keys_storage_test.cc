#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <fstream>

#include <mail_getter/vdirect/keys_storage.h>

namespace {
using namespace testing;
using namespace vdirect;

struct VdirectKeysStorageTest : public Test {
};

TEST_F(VdirectKeysStorageTest, shouldLoadTheKeysAndSetTheFirstEntryAsDefault) {
    std::stringstream buffer;
    buffer << "key"  << KeysStorage::delimiter() << "value" << std::endl
           << "key2" << KeysStorage::delimiter() << "value2" << std::endl;

    KeysStorage storage(buffer);

    EXPECT_EQ(storage.key("key"), "value");
    EXPECT_EQ(storage.key("key2"), "value2");
    EXPECT_EQ(storage.defaultKeyName(), "key");
    EXPECT_EQ(storage.defaultKey(), "value");
    EXPECT_FALSE(storage.empty());
}

TEST_F(VdirectKeysStorageTest, shouldLoadKeysFromFileWithoutLastNewLine) {
    std::stringstream buffer;
    buffer << "key"  << KeysStorage::delimiter() << "value";

    KeysStorage storage(buffer);

    EXPECT_EQ(storage.key("key"), "value");
    EXPECT_EQ(storage.defaultKeyName(), "key");
    EXPECT_EQ(storage.defaultKey(), "value");
    EXPECT_FALSE(storage.empty());
}

TEST_F(VdirectKeysStorageTest, shouldThrowAnExceptionOnDataWithoutDelimiter) {
    std::stringstream buffer;
    buffer << "key" << std::endl;

    ASSERT_THROW({KeysStorage storage(buffer);}, std::invalid_argument);
}

TEST_F(VdirectKeysStorageTest, shouldThrowAnExceptionOnIncorrectFile) {
    std::ifstream buffer("");
    EXPECT_THROW({KeysStorage storage(buffer);}, std::invalid_argument);
    EXPECT_FALSE(buffer.is_open());
}

TEST_F(VdirectKeysStorageTest, shouldBeEmptyIfNoKeysWerePassed) {
    std::stringstream buffer;
    KeysStorage storage(buffer);

    EXPECT_TRUE(storage.empty());
}

TEST_F( VdirectKeysStorageTest, defaultKey_onNoDefaultKeySpecified_throwsOutOfRange ) {
    KeysStorage storage;
    ASSERT_THROW(storage.defaultKey(), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, defaultKeyName_onNoDefaultKeySpecified_throwsOutOfRange ) {
    KeysStorage storage;
    ASSERT_THROW(storage.defaultKeyName(), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, empty_onEmptyStorage_returnsTrue ) {
    KeysStorage storage;
    ASSERT_TRUE(storage.empty());
}

TEST_F( VdirectKeysStorageTest, empty_onNonEmptyStorage_returnsFalse ) {
    KeysStorage storage;
    storage.addKey("1","2");
    ASSERT_FALSE(storage.empty());
}

TEST_F( VdirectKeysStorageTest, defaultKey_onAddKeySpecified_returnsKey ) {
    KeysStorage storage;
    storage.addKey("1", "2", true);
    ASSERT_EQ(storage.defaultKey(), "2");
}

TEST_F( VdirectKeysStorageTest, defaultKeyName_onAddKeySpecified_returnsKeyName ) {
    KeysStorage storage;
    storage.addKey("1", "2", true);
    ASSERT_EQ(storage.defaultKeyName(), "1");
}

TEST_F( VdirectKeysStorageTest, defaultKey_onAddKeyNonSpecified_throwsOutOfRange ) {
    KeysStorage storage;
    storage.addKey("1", "2");
    ASSERT_THROW(storage.defaultKeyName(), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, defaultKeyName_onAddKeyNonSpecified_throwsOutOfRange ) {
    KeysStorage storage;
    storage.addKey("1", "2");
    ASSERT_THROW(storage.defaultKeyName(), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, defaultKey_onSetDefaultKeySpecified_returnsKey ) {
    KeysStorage storage;
    storage.addKey("1", "2");
    storage.setDefaultKey("1");
    ASSERT_EQ(storage.defaultKey(), "2");
}

TEST_F( VdirectKeysStorageTest, defaultKeyName_onSetDefaultKeySpecified_returnsKeyName ) {
    KeysStorage storage;
    storage.addKey("1", "2");
    storage.setDefaultKey("1");
    ASSERT_EQ(storage.defaultKeyName(), "1");
}

TEST_F( VdirectKeysStorageTest, setDefaultKey_withNonExistentKey_throwsOutOfRange ) {
    KeysStorage storage;
    ASSERT_THROW(storage.setDefaultKey("1"), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, key_withNonExistentKey_throwsOutOfRange ) {
    KeysStorage storage;
    ASSERT_THROW(storage.key("1"), std::out_of_range);
}

TEST_F( VdirectKeysStorageTest, key_withExistentKey_returnsKey ) {
    KeysStorage storage;
    storage.addKey("1", "2");
    ASSERT_EQ(storage.key("1"), "2");
}

}
