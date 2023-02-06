#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/reflection/reflection.h>

namespace {

using namespace testing;
using namespace yamail::data::reflection;

struct Tag {};

struct TestVisitor : Visitor {
    template <class Value>
    void onValue(Value&& value, Tag) {
        return onValueImpl(std::forward<Value>(value));
    }

    MOCK_METHOD(void, onValueImpl, (int), ());
};

struct ReflectionReferenceWrapperTest : Test {
    StrictMock<TestVisitor> visitor;
    Tag tag;
};

TEST_F(ReflectionReferenceWrapperTest, for_not_const) {
    int value = 42;
    std::reference_wrapper<int> reference = std::ref(value);
    EXPECT_CALL(visitor, onValueImpl(42)).WillOnce(Return());
    applyVisitor(reference, visitor, tag);
}

TEST_F(ReflectionReferenceWrapperTest, for_const) {
    const int value = 42;
    std::reference_wrapper<const int> reference = std::ref(value);
    EXPECT_CALL(visitor, onValueImpl(42)).WillOnce(Return());
    applyVisitor(reference, visitor, tag);
}

} // namespace
