#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/reflection.h>
#include <yamail/data/serialization/yajl.h>
#include <yamail/data/deserialization/yajl.h>

using namespace testing;
using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

enum class TestEnum {
    First,
    Second
};

struct EnumTestData {
    TestEnum enumField = TestEnum::First;

    EnumTestData() = default;

    EnumTestData(const TestEnum enumValue)
        : enumField(enumValue)
    {}

    bool operator== (const EnumTestData& that) const noexcept {
        return enumField == that.enumField;
    }
};

YREFLECTION_ADAPT_ADT(EnumTestData,
    YREFLECTION_ENUM_MEMBER(TestEnum, enumField)
)

static const std::string jsonValue =
"{"
    "\"enumField\":1"
"}";

TEST(EnumClassTest, testObjectSerialization) {
    EnumTestData data(TestEnum::Second);
    EXPECT_EQ(jsonValue, toJson(data).str());
}

TEST(EnumClassTest, testObjectDeserialization) {
    EnumTestData expected(TestEnum::Second);
    const auto result = fromJson<EnumTestData>(jsonValue);
    EXPECT_EQ(expected, result);
}

namespace test {
YREFLECTION_DEFINE_ENUM_INLINE(OperationType,
    spam,
    unspam,
    purge
)

enum Zzzz {
    xxx,
    yyy,
    zzz
};

} // namespace test

YREFLECTION_ADAPT_ENUM(test::Zzzz,
    xxx,
    yyy,
    zzz
)

BOOST_FUSION_DEFINE_STRUCT((test), TestStruct,
    (test::OperationType, op)
    (test::Zzzz, z)
)

namespace yr = yamail::data::reflection;

using namespace std::literals;

TEST(enum_items, with_inline_defined_enum_should_contains_all_the_elements) {
    EXPECT_THAT(yr::enum_items<test::OperationType>, ElementsAre(
        test::OperationType::spam, test::OperationType::unspam, test::OperationType::purge));
}

TEST(enum_items, with_adapted_enum_should_contains_all_the_elements) {
    EXPECT_THAT(yr::enum_items<test::Zzzz>, ElementsAre(
        test::Zzzz::xxx, test::Zzzz::yyy, test::Zzzz::zzz));
}

TEST(to_string, with_inline_defined_enum_should_return_string_representation_of_an_item) {
    EXPECT_EQ(yr::to_string(test::OperationType::spam), "spam"s);
}

TEST(to_string, with_adapted_enum_should_return_string_representation_of_an_item) {
    EXPECT_EQ(yr::to_string(test::Zzzz::yyy), "yyy"s);
}

TEST(from_string, with_inline_defined_enum_and_valid_string_should_change_enum_argument_and_return_true) {
    test::OperationType tmp{test::OperationType::purge};
    yr::from_string("spam", tmp);
    EXPECT_EQ(tmp, test::OperationType::spam);
}

TEST(from_string, with_adapted_enum_and_valid_string_should_change_enum_argument_and_return_true) {
    test::Zzzz tmp{test::Zzzz::xxx};
    EXPECT_TRUE(yr::from_string("yyy", tmp));
    EXPECT_EQ(tmp, test::Zzzz::yyy);
}

TEST(from_string, with_inline_defined_enum_and_valid_string_should_not_change_enum_argument_and_return_false) {
    test::OperationType tmp{test::OperationType::purge};
    EXPECT_FALSE(yr::from_string("ZHOPA", tmp));
    EXPECT_EQ(tmp, test::OperationType::purge);
}

TEST(from_string, with_adapted_enum_and_valid_string_should_not_change_enum_argument_and_return_false) {
    test::Zzzz tmp{test::Zzzz::xxx};
    EXPECT_FALSE(yr::from_string("zhopa", tmp));
    EXPECT_EQ(tmp, test::Zzzz::xxx);
}

TEST(from_string, with_inline_defined_enum_and_valid_string_should_return_value) {
    EXPECT_EQ(yr::from_string<test::OperationType>("spam"), test::OperationType::spam);
}

TEST(from_string, with_adapted_enum_and_valid_string_should_return_value) {
    EXPECT_EQ(yr::from_string<test::Zzzz>("yyy"), test::Zzzz::yyy);
}

TEST(from_string, with_inline_defined_enum_and_invalid_string_should_throw) {
    EXPECT_THROW(yr::from_string<test::OperationType>("ZhOpA"), std::invalid_argument);
}

TEST(from_string, with_adapted_enum_and_valid_string_should_throw) {
    EXPECT_THROW(yr::from_string<test::Zzzz>("zHoPa"), std::invalid_argument);
}

TEST(toJson, should_generate_json_with_enum_string_representation) {
    using namespace std::literals;
    test::TestStruct obj;
    obj.op = test::OperationType::spam;
    obj.z = test::Zzzz::yyy;
    EXPECT_EQ(R"({"op":"spam","z":"yyy"})"s, toJson(obj).str());
}

TEST(toJson, should_deserialize_from_json_with_enum_string_representation) {
    const auto result = fromJson<test::TestStruct>(R"({"op":"spam","z":"yyy"})");
    EXPECT_EQ(result.op, test::OperationType::spam);
    EXPECT_EQ(result.z, test::Zzzz::yyy);
}
