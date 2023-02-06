#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <library/cpp/testing/gtest_boost_extensions/extensions.h>
#include <yamail/data/reflection/reflection.h>

namespace {

using namespace testing;
using namespace yamail::data::reflection;

struct Tag {};

struct TestVisitor : Visitor {
    template <class T>
    bool onOptional(boost::optional<T>& value, Tag) {
        return onOptionalBoost(value);
    }

    MOCK_METHOD(bool, onOptionalBoost, (boost::optional<int>), ());

#if __cplusplus >= 201703L

    template <class T>
    bool onOptional(std::optional<T>& value, Tag) {
        return onOptionalStd(value);
    }

    MOCK_METHOD(bool, onOptionalStd, (std::optional<int>), ());

#endif
};

struct OptionalTest : Test {
    StrictMock<TestVisitor> visitor;
    Tag tag;
};

TEST_F(OptionalTest, for_boost_optional) {
    boost::optional<int> reference(42);
    EXPECT_CALL(visitor, onOptionalBoost(boost::optional<int>(42))).WillOnce(Return(true));
    applyVisitor(reference, visitor, tag);
}

#if __cplusplus >= 201703L

TEST_F(OptionalTest, for_std_optional) {
    std::optional<int> reference(42);
    EXPECT_CALL(visitor, onOptionalStd(std::optional<int>(42))).WillOnce(Return(true));
    applyVisitor(reference, visitor, tag);
}

#endif

} // namespace
