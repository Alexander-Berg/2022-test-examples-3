#include <pgg/query/repository/parameters_resolver.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>


namespace {

using namespace testing;
using namespace pgg::query;
using namespace pgg::query::repository;

struct ItemMock {
    MOCK_METHOD(std::string, name, (), (const));
    MOCK_METHOD(const ParameterValuesNames &, valueNames, (), (const));
    MOCK_METHOD(std::size_t, indexOfValue, (const std::string &), (const));
    MOCK_METHOD(const ParameterValues &, traits, (), (const));
    MOCK_METHOD(void, traits, (const ParameterValues &), (const));
};


struct QueryRepositoryParametersResolverTest : public Test {
    QueryRepositoryParametersResolverTest()
    : names{"val1", "val2"}, traits(2) {
    }
    ParametersResolver resolver;
    const ParameterValuesNames names;
    ItemMock item;
    ParameterValues traits;
};

TEST_F(QueryRepositoryParametersResolverTest, resolve_withParameterMapContainsAllVariables_resolvesAllVariables) {
    const ParameterMap map{{"val1", "value1"}, {"val2", "value2"}};
    EXPECT_CALL(item, valueNames()).WillRepeatedly(ReturnRef(names));
    EXPECT_CALL(item, indexOfValue("val1")).WillOnce(Return(0));
    EXPECT_CALL(item, indexOfValue("val2")).WillOnce(Return(1));
    EXPECT_CALL(item, traits()).WillOnce(ReturnRef(traits));

    EXPECT_CALL(item, traits(ElementsAre("value1", "value2"))).WillOnce(Return());
    EXPECT_FALSE(resolver.resolve(item, map));
}

TEST_F(QueryRepositoryParametersResolverTest, resolve_withParameterMapContainsUnknownVariables_returnsUnresolvedError) {
    const ParameterMap map{{"val1", "value1"}, {"val2", "value2"}, {"val3", "unknown"}};
    EXPECT_CALL(item, valueNames()).WillRepeatedly(ReturnRef(names));
    EXPECT_CALL(item, indexOfValue("val1")).WillOnce(Return(0));
    EXPECT_CALL(item, indexOfValue("val2")).WillOnce(Return(1));
    EXPECT_CALL(item, indexOfValue("val3")).WillOnce(Throw(std::out_of_range("")));
    EXPECT_CALL(item, traits()).WillOnce(ReturnRef(traits));

    EXPECT_EQ(resolver.resolve(item, map).value(), error::unresolved);
}

TEST_F(QueryRepositoryParametersResolverTest, resolve_withParameterMapContainsNotAllVariables_returnsUnusedError) {
    const ParameterMap map;
    EXPECT_CALL(item, valueNames()).WillRepeatedly(ReturnRef(names));
    EXPECT_CALL(item, traits()).WillOnce(ReturnRef(traits));

    EXPECT_EQ(resolver.resolve(item, map).value(), error::unused);
}

} // namespace
