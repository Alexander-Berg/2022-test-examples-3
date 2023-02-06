#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/query/repository/table.h>
#include <pgg/query/repository/tableio.h>

namespace pgg {

namespace testing {
struct scope{};

struct __OrderBy{
    enum Enum {
        date,
        subject,
        from,

        maxValue = from,
        defaultValue = date,
    };
    typedef Enum2String<Enum>::Map Map;
    void fill(Map & map) const {
#define ADD_ENUM_TO_MAP(name) map.insert(Map::value_type(name, #name))
        ADD_ENUM_TO_MAP(date);
        ADD_ENUM_TO_MAP(subject);
        ADD_ENUM_TO_MAP(from);
#undef ADD_ENUM_TO_MAP
    }
    typedef __OrderBy Filler;
};

struct OrderBy : public query::Parameter<OrderBy, __OrderBy>{
    OrderBy(Enum v = defaultValue) : Inherited(v) {}
};

struct Unregistered : public query::Parameter<Unregistered, __OrderBy>{
    Unregistered(Enum v = defaultValue) : Inherited(v) {}
};

} // namespace testing
} // namespace pgg

namespace {

using namespace testing;
using pgg::testing::OrderBy;

typedef pgg::query::repository::ParametersTable Table;
typedef pgg::query::ParameterValues Values;

struct QueryRepositoryParametersTableTest : public Test {
    QueryRepositoryParametersTableTest() {
        table.add<pgg::testing::OrderBy>();
        table.begin()->traits(Values{"byDate", "bySybject", "byFrom"});
    }
    Table table;
};

TEST_F(QueryRepositoryParametersTableTest, parametersTable_size_returnsNonZero) {
    EXPECT_EQ(table.size(), 1ul);
}

TEST_F(QueryRepositoryParametersTableTest, parametersTable_valueWithParameter_returnsValue) {
    EXPECT_EQ(table.value(OrderBy(OrderBy::date)), "byDate");
}

TEST_F(QueryRepositoryParametersTableTest, indexOfValue_withAppropriateName_returnsCorrespondingValueIndex) {
    EXPECT_EQ(table.find<OrderBy>()->indexOfValue("date"), OrderBy::date);
}

TEST_F(QueryRepositoryParametersTableTest, indexOfValue_withNotExistentName_throwsException) {
    EXPECT_THROW(table.find<OrderBy>()->indexOfValue("SomeBadName"), std::out_of_range);
}

TEST_F(QueryRepositoryParametersTableTest, parametersTable_findTypeNotRegistered_returnsEnd) {
    EXPECT_TRUE(table.find<pgg::testing::Unregistered>() == table.end());
}

TEST_F(QueryRepositoryParametersTableTest, parametersTable_findTypeRegistered_returnsIterator) {
    EXPECT_TRUE(table.find<pgg::testing::OrderBy>() == table.begin());
}


TEST_F(QueryRepositoryParametersTableTest, parametersTable_idWithRegisteredName_returnsIdForName) {
    EXPECT_TRUE(table.find("OrderBy") == table.begin());
}

TEST_F(QueryRepositoryParametersTableTest, parametersTable_idWithNonRegisteredName_returnsNullId) {
    EXPECT_TRUE(table.find("Unregistered") == table.end());
}

} // namespace
