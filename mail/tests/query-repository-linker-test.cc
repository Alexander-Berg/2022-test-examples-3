#include <pgg/query/repository/linker.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <map>

namespace {

using namespace testing;
using namespace pgg::query;
using namespace pgg::query::repository;

struct ResolverMock {
    typedef int VariablesMap;
    MOCK_METHOD(ErrorCode, resolve, ( const VariablesMap & vars, Body & body ), (const));
};

struct ResolverWrap {
    template <typename Item>
    ErrorCode resolve( Item & item, const Traits & in ) const {
        auto out = in;
        return resolve( item.variablesMap(), out.body );
    }
    ErrorCode resolve( const ResolverMock::VariablesMap & vars, Body & body ) const {
        return mock.resolve(vars, body);
    }
    ResolverWrap(ResolverMock & mock) : mock(mock) {}
    ResolverMock & mock;
};

struct TableMock {
    struct Item {
        const std::string & name() const { return name_;}
        ResolverMock::VariablesMap variablesMap() const { return 0; }
        Item(const std::string & name_) : name_(name_) {}
        std::string name_;
    };
    typedef std::map<std::size_t,Traits> TraitsVector;
    typedef pgg::query::TraitsMap TraitsMap;
    typedef std::vector<Item> Items;
    typedef Items::const_iterator const_iterator;
    typedef const_iterator iterator;
    MOCK_METHOD(std::size_t, size, (), (const));
    MOCK_METHOD(iterator, begin, (), (const));
    MOCK_METHOD(iterator, end, (), (const));
    MOCK_METHOD(iterator, find, (const std::string &), (const));
};

typedef TableMock::Item Item;

struct QueryRepositoryLinkerTest : public Test {
    QueryRepositoryLinkerTest()
    : wrap(resolver), linker(wrap),
      items{ Item("Query"), Item("UnspecifiedQuery") } {
        traitsMap["Query"] = Traits();
    }
    TableMock table;
    ResolverMock resolver;
    ResolverWrap wrap;
    TraitsMap traitsMap;
    Linker<ResolverWrap> linker;
    TableMock::Items items;
};

TEST_F(QueryRepositoryLinkerTest, link_withRegistredQuery_callsResolveAndReturnsNoError) {
    InSequence s;
    EXPECT_CALL(table, size()).WillOnce(Return(1));
    EXPECT_CALL(table, find("Query")).WillOnce(Return(items.begin()));
    EXPECT_CALL(table, end()).WillOnce(Return(items.end()));
    EXPECT_CALL(resolver, resolve(_,_)).WillOnce(Return(ErrorCode()));
    EXPECT_FALSE(linker.link(table, traitsMap));
}

TEST_F(QueryRepositoryLinkerTest, link_withInsufficientTraitsMapSize_returnsUnspecified) {
    EXPECT_CALL(table, size()).WillRepeatedly(Return(items.size()));
    EXPECT_CALL(table, begin()).WillRepeatedly(Return(items.begin()));
    EXPECT_CALL(table, end()).WillRepeatedly(Return(items.end()));

    EXPECT_EQ(linker.link(table, traitsMap).value(), error::unspecified);
}

TEST_F(QueryRepositoryLinkerTest, link_withInsufficientTraitsMapSize_returnsUnspecifiedListInMessage) {
    EXPECT_CALL(table, size()).WillRepeatedly(Return(items.size()));
    EXPECT_CALL(table, begin()).WillRepeatedly(Return(items.begin()));
    EXPECT_CALL(table, end()).WillRepeatedly(Return(items.end()));

    EXPECT_EQ(linker.link(table, traitsMap).message(), "UnspecifiedQuery");
}

TEST_F(QueryRepositoryLinkerTest, link_withUnRegisteredQuery_returnsUnregistered) {
    InSequence s;
    EXPECT_CALL(table, size()).WillOnce(Return(1));
    EXPECT_CALL(table, find("Query")).WillOnce(Return(items.end()));
    EXPECT_CALL(table, end()).WillOnce(Return(items.end()));

    EXPECT_EQ(linker.link(table, traitsMap).value(), error::unregistered);
}

TEST_F(QueryRepositoryLinkerTest, link_withRegistredQueryAndResolverError_returnsLinkerError) {
    InSequence s;
    EXPECT_CALL(table, size()).WillOnce(Return(1));
    EXPECT_CALL(table, find("Query")).WillOnce(Return(items.begin()));
    EXPECT_CALL(table, end()).WillOnce(Return(items.end()));
    EXPECT_CALL(resolver, resolve(_,_)).WillOnce(Return(pgg::error_code(error::unresolved, "")));

    EXPECT_EQ(linker.link(table, traitsMap).value(), error::unresolved);
}

} // namespace
