#include <gtest/gtest.h>

#include <src/logic/interface/types/reflection/existing_tag.hpp>

namespace {

using namespace testing;

using collie::logic::TagType;
using collie::logic::toString;
using collie::logic::makeTagType;

struct TestLogicTypesReflectionToStringTagType : Test {};

TEST_F(TestLogicTypesReflectionToStringTagType, for_user_should_return_user) {
    EXPECT_EQ(toString(TagType::user), "user");
}

TEST_F(TestLogicTypesReflectionToStringTagType, for_system_should_return_system) {
    EXPECT_EQ(toString(TagType::system), "system");
}

struct TestLogicTypesReflectionFromStringTagType : Test {};

TEST_F(TestLogicTypesReflectionFromStringTagType, for_user_should_return_user) {
    EXPECT_EQ(makeTagType("user"), TagType::user);
}

TEST_F(TestLogicTypesReflectionFromStringTagType, for_system_should_return_system) {
    EXPECT_EQ(makeTagType("system"), TagType::system);
}

} // namespace
