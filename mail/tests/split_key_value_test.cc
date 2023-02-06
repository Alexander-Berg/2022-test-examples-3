#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/split_key_value.h>

namespace {

using namespace testing;

typedef Test SplitKeyValueTest;

TEST_F( SplitKeyValueTest, withKeyEqSignValue_returnsPairFirstKeySecondValue ) {
    std::pair<std::string,std::string> res = splitKeyValue("key=value");
    ASSERT_EQ(res.first,"key");
    ASSERT_EQ(res.second,"value");
}

TEST_F( SplitKeyValueTest, withKeyAndNoValue_returnsPairFirstKeySecondEmpty ) {
    std::pair<std::string,std::string> res = splitKeyValue("key=");
    ASSERT_EQ(res.first,"key");
    ASSERT_EQ(res.second,"");
}

TEST_F( SplitKeyValueTest, withNoEqSign_returnsPairFirstKeySecondEmpty ) {
    std::pair<std::string,std::string> res = splitKeyValue("key");
    ASSERT_EQ(res.first,"key");
    ASSERT_EQ(res.second,"key");
}

}
