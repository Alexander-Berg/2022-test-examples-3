
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/MessageAccessDefinitions.h>

namespace {

TEST(MetaAttributes, outputOperator_emptyAttrs_onlyBraces) {
    MetaAttributes attrs;
    std::ostringstream ss;
    ss << attrs;
    ASSERT_EQ("{}", ss.str());
}

TEST(MetaAttributes, outputOperator_singleAttr_singleKeyValue) {
    MetaAttributes attrs;
    attrs["name"] = "value";
    std::ostringstream ss;
    ss << attrs;
    ASSERT_EQ("{\"name\": \"value\"}", ss.str());
}

TEST(MetaAttributes, outputOperator_manyAttrs_keyValuePairsCommaDelimited) {
    MetaAttributes attrs;
    attrs["first_name"] = "first_value";
    attrs["second_name"] = "second_value";
    attrs["third_name"] = "third_value";
    std::ostringstream ss;
    ss << attrs;
    ASSERT_EQ("{\"first_name\": \"first_value\", \"second_name\": \"second_value\", \"third_name\": \"third_value\"}", ss.str());
}

TEST(MetaLevel, outputOperator_empty_onlyBraces) {
    MetaLevel level;
    std::ostringstream ss;
    ss << level;
    ASSERT_EQ("[]", ss.str());
}

TEST(MetaLevel, outputOperator_singleItem_outputSingleItem) {
    MetaLevel level;
    level.push_back("first");
    std::ostringstream ss;
    ss << level;
    ASSERT_EQ("[\"first\"]", ss.str());
}

TEST(MetaLevel, outputOperator_manyItems_outputItemsCommaDelimited) {
    MetaLevel level;
    level.push_back("first");
    level.push_back("second");
    level.push_back("third");
    std::ostringstream ss;
    ss << level;
    ASSERT_EQ("[\"first\", \"second\", \"third\"]", ss.str());
}

TEST(getStidWithoutPrefix, getStidWithoutPrefix_stidWithPrefix_removePrefix) {
    ASSERT_EQ("1234.5678.90", getStidWithoutPrefix("mulca:2:1234.5678.90"));
}

TEST(getStidWithoutPrefix, getStidWithoutPrefix_stidWithoutPrefix_noChange) {
    ASSERT_EQ("1234.5678.90", getStidWithoutPrefix("1234.5678.90"));
}

TEST(getStidWithoutPrefix, getStidWithoutPrefix_emptyStid_returnEmpty) {
    ASSERT_EQ("", getStidWithoutPrefix(""));
}


}
