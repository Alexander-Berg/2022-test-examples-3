#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <user_journal/enumeration.h>
#include <sstream>

namespace {
using namespace testing;

struct __Enum{
    enum Enum {
        unknown,
        element1,
        element2
    };
    typedef user_journal::Enum2String<Enum>::Map Map;
    void fill(Map & map) const{
#define ADD_ENUM_TO_MAP(name) map.insert(Map::value_type(name, #name))
        ADD_ENUM_TO_MAP(unknown);
        ADD_ENUM_TO_MAP(element1);
        ADD_ENUM_TO_MAP(element2);
#undef ADD_ENUM_TO_MAP
    }
    typedef __Enum Filler;
};

typedef user_journal::Enumeration<__Enum> Enum;

typedef Test EnumerationTest;

TEST_F(EnumerationTest, defaultConstructor_setsValue_toUnknown) {
    ASSERT_EQ( Enum().value(), Enum::unknown );
}

TEST_F(EnumerationTest, operatorEq_thenEq_returnsTrue) {
    Enum e;
    ASSERT_TRUE( e == Enum::unknown );
}

TEST_F(EnumerationTest, operatorEq_thenNotEq_returnsFalse) {
    Enum e;
    ASSERT_FALSE( e == Enum::element1 );
}

TEST_F(EnumerationTest, operatorAssignment_setUpValue_right) {
    Enum e;
    e = Enum::element1;
    ASSERT_EQ( e, Enum::element1 );
}

TEST_F(EnumerationTest, toString_element1Value_returnsElement1) {
    Enum e(Enum::element1);
    ASSERT_EQ( e.toString(), "element1" );
}

TEST_F(EnumerationTest, fromString_withElement2string_returnsElement2) {
    ASSERT_EQ( Enum::fromString("element2"), Enum::element2 );
}

TEST_F(EnumerationTest, fromString_withUndefinedStringValue_throwsException) {
    ASSERT_THROW( Enum::fromString("ZZZZ"), std::out_of_range );
}

TEST_F(EnumerationTest, fromString_withUndefinedStringValueAndNoThrow_returnsDefaultValue) {
    ASSERT_EQ( Enum::fromString("ZZZZ", std::nothrow), Enum::unknown );
}

TEST_F(EnumerationTest, outputOperator_withElement1Value_returnsElement1) {
    std::ostringstream s;
    s << Enum(Enum::element1);
    ASSERT_EQ( s.str(), "element1" );
}

TEST_F(EnumerationTest, inputOperator_withElement2string_returnsElement2) {
    std::istringstream s("element2");
    Enum e;
    s >> e;
    ASSERT_EQ( e, Enum::element2 );
}

TEST_F(EnumerationTest, inputOperator_withUndefinedStringValue_returnsUnknown) {
    std::istringstream s("ZZZZZ");
    Enum e;
    s >> e;
    ASSERT_EQ( e, Enum::unknown );
}

}
