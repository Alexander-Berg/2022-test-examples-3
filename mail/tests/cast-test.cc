#include <unordered_map>
#include <unordered_set>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/optional.hpp>
#include <boost/fusion/adapted/struct/define_struct.hpp>
#include <pgg/cast.h>
#include <yamail/data/serialization/json_writer.h>

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithOneField,
    (int, field)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithStringField,
    (std::string, field)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithVectorField,
    (std::vector<int>, field)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithOptionalField,
    (boost::optional<int>, field)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithTwoFieldsOfSameType,
    (int, field1)
    (int, field2)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithTwoFieldsOfDiffType,
    (int, field1)
    (std::string, field2)
)

BOOST_FUSION_DEFINE_STRUCT((cast_test), WithVectorOfCompositesField,
    (std::vector<cast_test::WithTwoFieldsOfDiffType>, field)
)

#define DEFINE_OPERATOR_EQ_1(Type) \
    bool operator ==(const Type& lhs, const Type& rhs) { \
        return lhs.field == rhs.field; \
    }

#define DEFINE_OPERATOR_EQ_2(Type) \
    bool operator ==(const Type& lhs, const Type& rhs) { \
        return lhs.field1 == rhs.field1 && lhs.field2 == rhs.field2; \
    }

#define DEFINE_OPERATOR_OSTREAM_LSHIFT(Type) \
    std::ostream& operator <<(std::ostream& stream, const Type& value) { \
        using namespace yamail::data::serialization; \
        return stream << toJson<Type>(value).str(); \
    }

namespace boost {

template <class T>
std::ostream& operator <<(std::ostream& stream, const boost::optional<T>& value) {
    if (value) {
        return stream << "(" << *value << ")";
    } else {
        return stream << "null";
    }
}

}

namespace cast_test {

DEFINE_OPERATOR_EQ_1(WithOneField)
DEFINE_OPERATOR_EQ_1(WithStringField)
DEFINE_OPERATOR_EQ_1(WithVectorField)
DEFINE_OPERATOR_EQ_1(WithOptionalField)
DEFINE_OPERATOR_EQ_2(WithTwoFieldsOfSameType)
DEFINE_OPERATOR_EQ_2(WithTwoFieldsOfDiffType)
DEFINE_OPERATOR_EQ_1(WithVectorOfCompositesField)

DEFINE_OPERATOR_OSTREAM_LSHIFT(WithOneField)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithStringField)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithVectorField)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithOptionalField)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithTwoFieldsOfSameType)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithTwoFieldsOfDiffType)
DEFINE_OPERATOR_OSTREAM_LSHIFT(WithVectorOfCompositesField)

} // namespace cast_test

#undef DEFINE_OPERATOR_EQ_1
#undef DEFINE_OPERATOR_EQ_2
#undef DEFINE_OPERATOR_OSTREAM_LSHIFT

namespace {

using namespace testing;
using namespace cast_test;

template <class T>
class MakeMapVisitor {
public:
    using Map = std::unordered_map<std::string, T>;

    MakeMapVisitor(Map& map) : map(map) {}

    template<class Value>
    typename std::enable_if<std::is_same<T, Value>::value, void>::type
    field(Value& value, const std::string& name) {
        map.emplace(name, value);
    }

    template<class Value>
    typename std::enable_if<!std::is_same<T, Value>::value, void>::type
    field(Value&, const std::string&) {}

    template <class Value>
    void apply(Value& value) {
        pgg::Struct<Value, MakeMapVisitor>::visit(value, *this);
    }

private:
    Map& map;
};

template <class T>
MakeMapVisitor<T> makeMakeMapVisitor(std::unordered_map<std::string, T>& map) {
    return MakeMapVisitor<T>(map);
}

class MakeFieldsNamesSetVisitor {
public:
    using Set = std::unordered_set<std::string>;

    MakeFieldsNamesSetVisitor(Set& set) : set(set) {}

    template<class Value>
    void field(Value&, const std::string& name) {
        set.insert(name);
    }

    template <class Value>
    void apply(Value& value) {
         pgg::Struct<Value, MakeFieldsNamesSetVisitor>::visit(value, *this);
    }

private:
    Set& set;
};

template <class Data>
class Row {
public:
    Row(Data& data) : data(data) {}

    template <class Value>
    void at(const std::string& name, Value& value) const {
        std::unordered_map<std::string, Value> map;
        makeMakeMapVisitor(map).apply(data);
        value = map.at(name);
    }

    bool has_column(const std::string& name) const {
        std::unordered_set<std::string> set;
        MakeFieldsNamesSetVisitor(set).apply(data);
        return set.count(name);
    }

private:
    Data& data;
};

template <class Data>
Row<Data> makeRow(Data& data) {
    return Row<Data>(data);
}

TEST(CastTest, cast_from_row_with_one_field_should_succeed) {
    WithOneField data {42};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithOneField>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_string_field_should_succeed) {
    WithStringField data {"text"};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithStringField>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_vector_should_succeed) {
    WithVectorField data {{1, 2, 3}};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithVectorField>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_existed_optional_should_set_value) {
    WithOptionalField data {42};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithOptionalField>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_uninitialized_optional_should_not_set_value) {
    WithOptionalField data {boost::optional<int>()};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithOptionalField>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_unexisted_optional_should_not_set_value) {
    WithTwoFieldsOfSameType data {42, 24};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithOptionalField>(row);
    EXPECT_FALSE(reflection.field.is_initialized());
}

TEST(CastTest, cast_from_row_with_two_fields_of_same_types_should_succeed) {
    WithTwoFieldsOfSameType data {42, 24};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithTwoFieldsOfSameType>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_two_fields_of_different_types_should_succeed) {
    WithTwoFieldsOfDiffType data {42, "text"};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithTwoFieldsOfDiffType>(row);
    EXPECT_EQ(reflection, data);
}

TEST(CastTest, cast_from_row_with_vector_of_composites_should_succeed) {
    using C = WithTwoFieldsOfDiffType;
    WithVectorOfCompositesField data {{C {1, "2"}, C {3, "4"}, C {5, "6"}}};
    const auto row = makeRow(data);
    const auto reflection = pgg::cast<WithVectorOfCompositesField>(row);
    EXPECT_EQ(reflection, data);
}

} // namespace
