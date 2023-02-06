#include "regular_mocks.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace regular::test;

struct TestRegexpSearch: public Test {
    MockRegular mockRegular;
};

TEST_F(TestRegexpSearch, for_not_inited_regexp_file_name_should_not_call_checkFileChanged_and_not_call_find_and_should_return_0) {
    std::string returning = "";
    EXPECT_CALL(mockRegular, checkFileChanged()).Times(0);
    EXPECT_CALL(mockRegular, find(_, _)).Times(0);
    EXPECT_EQ(0, mockRegular.search("test", returning));
}

TEST_F(TestRegexpSearch, for_not_changed_regexp_file_should_not_call_initRegexp_and_should_call_find) {
    std::string returning = "";
    mockRegular.initRegular("file");
    EXPECT_CALL(mockRegular, checkFileChanged()).WillOnce(Return(false));
    EXPECT_CALL(mockRegular, initRegexp()).Times(0);
    EXPECT_CALL(mockRegular, find(_, _)).WillOnce(Return(1));
    EXPECT_EQ(1, mockRegular.search("test", returning));
}

TEST_F(TestRegexpSearch, when_regexp_file_has_been_changed_for_not_inited_regexp_should_not_call_find_and_should_return_0) {
    std::string returning = "";
    mockRegular.initRegular("file");
    EXPECT_CALL(mockRegular, checkFileChanged()).WillOnce(Return(true));
    EXPECT_CALL(mockRegular, initRegexp()).WillOnce(Return(0));
    EXPECT_CALL(mockRegular, find(_, _)).Times(0);
    EXPECT_EQ(0, mockRegular.search("test", returning));
}

TEST_F(TestRegexpSearch, for_inited_regexp_should_call_find) {
    std::string returning = "";
    mockRegular.initRegular("file");
    EXPECT_CALL(mockRegular, checkFileChanged()).WillOnce(Return(true));
    EXPECT_CALL(mockRegular, initRegexp()).WillOnce(Return(1));
    EXPECT_CALL(mockRegular, find(_, _)).WillOnce(Return(1));
    EXPECT_EQ(1, mockRegular.search("test", returning));
}

}
