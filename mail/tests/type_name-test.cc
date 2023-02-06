#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/type_name.h>

namespace my_ns {
struct MyStruct {};
}

namespace {

using namespace testing;
using namespace pgg::details;

struct TypeNameTest : public Test {
    TypeNameTest(){}
};

TEST_F(TypeNameTest, stripNamespaces_withEmptyString_returnsEmptyString) {
    EXPECT_EQ(stripNamespaces(""), "");
}

TEST_F(TypeNameTest, stripNamespaces_withNoNamespace_returnsArgument) {
    EXPECT_EQ(stripNamespaces("NoNamespace"), "NoNamespace");
}

TEST_F(TypeNameTest, stripNamespaces_withNamespace_returnsNameWhithoutNamespace) {
    EXPECT_EQ(stripNamespaces("namespace1::namespace2::Name"), "Name");
}

TEST_F(TypeNameTest, typeName_withType_returnsFullTypeName) {
    EXPECT_EQ(typeName(::my_ns::MyStruct()), "my_ns::MyStruct");
}

} // namespace
