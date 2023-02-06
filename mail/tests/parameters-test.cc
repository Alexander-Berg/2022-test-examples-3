#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <user_journal/parameters/operation_parameters.h>
#include <sstream>

namespace {
using namespace testing;
using namespace user_journal;
using namespace user_journal::parameters;

struct MockMapper : public Mapper {
    MOCK_METHOD(void, mapValue, (const Operation & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Target & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::string & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Date & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (bool v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (int v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (size_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (std::time_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::vector<std::string> & v, const std::string & name) , (const, override));
};

struct ParametersTest : public Test {
    MockMapper mapper;
};

TEST_F(ParametersTest, parameters_declaredAndConstructedWithMandatoryParam_mapIt) {
    EXPECT_CALL( mapper, mapValue(std::string("10"), "mid") );
    Parameters<id::mid>(id::mid("10")).map(mapper);
}

TEST_F(ParametersTest, parameters_declaredAndConstructedWithOptionalParam_mapIt) {
    EXPECT_CALL( mapper, mapValue(std::string("10"), "mid") );
    Parameters<boost::optional<id::mid>>(id::mid("10")).map(mapper);
}

TEST_F(ParametersTest, parameters_declaredWithOptionalParamButConstructedWithoutIt_dontMapIt) {
    Parameters<boost::optional<id::mid>>().map(mapper);
}

TEST_F(ParametersTest, parameters_constructedWithParamInRecursiveTuple_mapIt) {
    EXPECT_CALL(mapper, mapValue(std::string("1"), "fid"));
    EXPECT_CALL(mapper, mapValue(std::string("2"), "lid"));
    EXPECT_CALL(mapper, mapValue(std::string("10"), "mid"));
    Parameters<id::mid, id::lid, id::fid>(id::fid("1"),
            std::make_tuple(id::lid("2"), std::make_tuple(id::mid("10")))).map(mapper);
}

TEST_F(ParametersTest, parameters_constructedWithSeveralCopiesOfSameParam_mapLastCopyOfIt) {
    EXPECT_CALL(mapper, mapValue(std::string("11"), "mid"));
    Parameters<id::mid>(id::mid("10"), id::mid("11")).map(mapper);
}

TEST_F(ParametersTest, simpleOperationParameters_declaredWithoutAdditionalParams_mapBaseParams) {
    EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
    EXPECT_CALL(mapper, mapValue(std::string("state"), "state"));
    EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(std::size_t(1)), "affected"));
    EXPECT_CALL(mapper, mapValue(TypedEq<bool>(true), "hidden"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::mailbox), "target"));
    EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::send), "operation"));
    EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
    EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
    OperationParameters<Target::mailbox, Operation::send>(
            id::mdb("pg"), id::state("state"), id::affected(1ul), id::hidden(true)).map(mapper);
}

}
