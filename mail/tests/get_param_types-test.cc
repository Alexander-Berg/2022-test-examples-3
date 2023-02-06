#include "user_journal/parameters/get_param_types.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <sstream>

namespace {
using namespace testing;
using namespace user_journal::parameters::details;

using GetParamsTest = Test;

struct Param1 {};
struct Param2 {};
struct Param3 {};
struct Param4 {};

TEST_F(GetParamsTest, GetParamTypes_forSimpleParams_returnsThem) {
    ASSERT_TRUE( (std::is_same<GetParamTypes<Param1, Param2>::type, TypeList<Param1, Param2>>::value) );
}

TEST_F(GetParamsTest, GetParamTypes_forCVQualifieredType_returnsItWithoutCVQualifiers) {
    ASSERT_TRUE( (std::is_same<GetParamTypes<const Param1&>::type, TypeList<Param1>>::value) );
}

TEST_F(GetParamsTest, GetParamTypes_forParamsInRecursiveTuple_returnsThem) {
    ASSERT_TRUE( (std::is_same<GetParamTypes<Param1, std::tuple<Param2, std::tuple<Param3, Param4>>>::type,
            TypeList<Param1, Param2, Param3, Param4>>::value) );
}

TEST_F(GetParamsTest, GetParamTypes_mixedTest) {
    ASSERT_TRUE( (std::is_same<
            GetParamTypes<Param1, std::tuple<Param2&, std::tuple<const Param3&, Param4> &>>::type,
            TypeList<Param1, Param2, Param3, Param4>>::value) );
}

}
