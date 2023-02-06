#include <internal/query/comment.h>
#include <internal/query/ids.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <map>

namespace {

using namespace testing;
using namespace macs::pg::query;

struct TestQuery : public pgg::query::QueryImpl<TestQuery, UserId> {
    template<typename ... Args>
    TestQuery(Args && ... args) : Inherited(std::forward<Args>(args)...) {}
};

struct QueryCommentTest : public Test {
    using Body = pgg::query::Body;
    using Traits = pgg::query::Traits;
    static Body makeBody(std::string txt) {
        auto body = Body{};
        body.add<Body::Text>(txt);
        return body;
    }
    void setTraits(std::string txt) {
        traits = Traits{{}, makeBody(txt)};
    }
    auto makeQuery() {
        return TestQuery(traits, parametersTable);
    }
    template <typename T, typename ... Args>
    auto query(Args && ... args) const {
        return T(traits, parametersTable, args...);
    }
    Traits traits;
    TestQuery::ParametersTable parametersTable;
};

TEST_F(QueryCommentTest, queryText_withNoCommentsSetUp_returnsTextFromBody) {
    setTraits("SELECT 123");
    EXPECT_EQ(makeQuery().text(), "SELECT 123");
}

TEST_F(QueryCommentTest, queryText_withCommentsSetUp_returnsTextWithCommentsBefore) {
    setTraits("SELECT 123");
    auto query = makeQuery();
    query.comment("ZZZ");
    EXPECT_EQ(query.text(), "/* ZZZ */SELECT 123");
}

TEST_F(QueryCommentTest, queryText_withBadComments_returnsTextWithOmmitedBadSymbols) {
    setTraits("SELECT 123");
    auto query = makeQuery();
    query.comment("A*B\\C/*D-EF\'GHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz 1234567890 [:_,!;=. ]+");
    EXPECT_EQ(query.text(), "/* ABCDEFGHIJKLMNOPQRSTUVWXYZ"\
            "abcdefghijklmnopqrstuvwxyz 1234567890 [:_,!;=. ]+ */SELECT 123");
}

TEST_F(QueryCommentTest, makeQueryComment_withQueryAndUid_returnsTextWithQueryNameAndUid) {
    setTraits("");
    EXPECT_EQ(makeQueryWithComment<TestQuery>(*this, "777").text(), "/* sql: TestQuery, uid: 777 */");
}

} // namespace

