#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/query/repository.h>
#include <pgg/query/repository/table.h>
#include <pgg/query/body.h>
#include <pgg/query/repository/tableio.h>
#include <pgg/query/ids.h>
#include <pgg/query/boundaries.h>

namespace pgg {

namespace testing {

struct TestIdTag {
    typedef std::string Value;
};

typedef query::ValueWrapper<TestIdTag> TestId;
}// namespace testing

namespace query {

template<typename Base>
struct Helper<Base, pgg::testing::TestId> {
    template <typename MapperT>
    void map(const MapperT & m) const {
        m.mapValue(v_.value, "testId");
    }

    Base& set(const pgg::testing::TestId & v) {
        v_ = v;
        return static_cast<Base&>(*this);
    }

    Base& idName(const std::string & v) {
        v_ = v;
        return static_cast<Base&>(*this);
    }
private:
    pgg::testing::TestId v_;
};

} // namespace query

namespace testing {

struct TestQuery : public query::QueryImpl<TestQuery, TestId> {
    template<typename... ArgsT>
    TestQuery( const query::Traits & t, const ArgsT& ... args )
            : Inherited( t, args... ) {}
};

struct UnregisteredTestQuery : public query::QueryImpl<
    UnregisteredTestQuery, TestId> {
    template<typename... ArgsT>
    UnregisteredTestQuery( const query::Traits & t, const ArgsT& ... args )
            : Inherited( t, args... ) {}
};

} // namespace testing
} // namespace pgg

namespace {

using namespace testing;

typedef pgg::query::repository::QueryTable Table;

struct QueryRepositoryTableTest : public Test {
    QueryRepositoryTableTest() {
        table.add<pgg::testing::TestQuery>();
    }
    Table table;
};

TEST_F(QueryRepositoryTableTest, queryTable_size_returnsNonZero) {
    EXPECT_EQ(table.size(), 1ul);
}

TEST_F(QueryRepositoryTableTest, queryTable_findTypeNotRegistered_returnsEnd) {
    EXPECT_TRUE(table.find<pgg::testing::UnregisteredTestQuery>() == table.end());
}

TEST_F(QueryRepositoryTableTest, queryTable_findTypeRegistered_returnsIterator) {
    EXPECT_TRUE(table.find<pgg::testing::TestQuery>() == table.begin());
}

TEST_F(QueryRepositoryTableTest, queryTable_idWithRegisteredName_returnsIdForName) {
    EXPECT_TRUE(table.find("TestQuery") == table.begin());
}

TEST_F(QueryRepositoryTableTest, queryTable_idWithNonRegisteredName_returnsNullId) {
    EXPECT_TRUE(table.find("UnregisteredTestQuery") == table.end());
}

} // namespace
