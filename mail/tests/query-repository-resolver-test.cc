#include <pgg/query/repository/resolver.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace pgg::query;
using namespace pgg::query::repository;

struct QueryRepositoryResolverTest : public Test {
    QueryRepositoryResolverTest()
    : vars {{"var1", 1}, {"var2", 2}},
      params {{"param1", 1}} {
    }
    VariablesMap vars;
    VariablesMap params;
    Resolver resolver;
};

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsNotAllParameters_returnsUnusedError) {
    Body body;
    body.add<Body::Variable>("var1");
    body.add<Body::Variable>("var2");
    EXPECT_EQ(resolver.resolve(vars, params, body).value(), error::unused);
}

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsAllVariablesAndParameters_returnsNoError) {
    Body body;
    body.add<Body::Variable>("var1");
    body.add<Body::Variable>("var2");
    body.add<Body::Parameter>("param1");
    EXPECT_FALSE(resolver.resolve(vars, params, body));
}

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsUnknownVariables_returnsUnresolvedError) {
    Body body;
    body.add<Body::Variable>("var1");
    body.add<Body::Variable>("var2");
    body.add<Body::Variable>("var3");
    EXPECT_EQ(resolver.resolve(vars, params, body).value(), error::unresolved);
}

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsNotAllVariables_returnsUnusedError) {
    Body body;
    EXPECT_EQ(resolver.resolve(vars, params, body).value(), error::unused);
}

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsAllVariables_returnsResolvedAndMergedBody) {
    Body body;
    body.add<Body::Text>("SELECT * FROM table WHERE id=");
    body.add<Body::Variable>("var1");
    body.add<Body::Text>(" AND val=");
    body.add<Body::Variable>("var2");
    body.add<Body::Text>(" ORDER BY id");
    EXPECT_FALSE(resolver.resolve(vars, VariablesMap(), body));
    std::string s;
    body.stream(s);
    EXPECT_EQ(s, "SELECT * FROM table WHERE id=$1 AND val=$2 ORDER BY id");
}

TEST_F(QueryRepositoryResolverTest, resolve_withBodyContainsParameters_setsParametersIds) {
    Body body;
    body.add<Body::Text>("SELECT * FROM table WHERE id=");
    body.add<Body::Parameter>("param1");
    const VariablesMap vars;
    EXPECT_FALSE(resolver.resolve(vars, params, body));
    EXPECT_EQ(body.size(), 2ul);
    Body::iterator i(body.begin());
    typedef const Body::ParameterResolved Resolved;
    Resolved & p1 = dynamic_cast<Resolved &>(*(++i));
    EXPECT_EQ(p1.id(), 1ul);
}

} // namespace
