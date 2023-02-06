#include <pgg/query/boundaries.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <map>

std::ostream & operator << ( std::ostream & s, const pgg::query::VariablesMap & m ) {
    for(const auto & i : m) {
        s << '{' << i.first << ',' << i.second << "},";
    }
    return s;
}

namespace {

using namespace testing;
using namespace pgg::query;
using namespace pgg::query::details;

struct ParamMock {
    MOCK_METHOD(std::string, name, (), (const));
};

struct ParameterMapperTest : public Test {
    VariablesMap map;
    ParameterMapper mapper = ParameterMapper(map);
    ParamMock param;
    ParameterMapperTest() { }
};

TEST_F(ParameterMapperTest, mapParameter1stCall_withParameter_addParameterWithIndexZero) {
    EXPECT_CALL(param, name()).WillOnce(Return("param"));
    mapper.mapParameter(param);
    const VariablesMap result = {{"param", 0}};
    EXPECT_EQ(result, map);
}

TEST_F(ParameterMapperTest, mapParameter_withTwoParameter_addTwoParameterWithIndexesZeroAndOneRespectively) {
    InSequence s;
    EXPECT_CALL(param, name()).WillOnce(Return("param"));
    EXPECT_CALL(param, name()).WillOnce(Return("param2"));
    mapper.mapParameter(param);
    mapper.mapParameter(param);
    const VariablesMap result = {{"param", 0}, {"param2", 1}};
    EXPECT_EQ(result, map);
}

TEST_F(ParameterMapperTest, mapParameter_withTwoParameterWithSameName_throwsException) {
    EXPECT_CALL(param, name()).WillRepeatedly(Return("param"));
    mapper.mapParameter(param);
    EXPECT_THROW(mapper.mapParameter(param), std::invalid_argument);
}

} // namespace

