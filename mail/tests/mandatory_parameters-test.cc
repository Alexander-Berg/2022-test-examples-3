#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <user_journal/parameters/mandatory_params.h>
#include <sstream>

namespace {
using namespace testing;
using namespace user_journal::parameters;

using MandatoryParamsTest = Test;

struct Param {};
using OptionalParam = boost::optional<Param>;

struct MandatoryParam1 {};
struct MandatoryParam2 {};
struct MandatoryParam3 {};

struct Arg1 {};
struct Arg2 {};
struct Arg3 {};
struct Arg4 {};


TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withOneMandatoryParam_forArgsWithoutIt_returnsFalse) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1>::value<Arg1, Arg2, Arg3>()), false );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withOneMandatoryParam_forArgsWithIt_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1>::value<Arg1, MandatoryParam1, Arg2>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withOptionalParam_forArgsWithIt_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<OptionalParam>::value<Arg1, Param, Arg3>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withOptionalParam_forArgsWithoutIt_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<OptionalParam>::value<Arg1, Arg2, Arg3>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMultiMandatoryParams_forArgsWithoutOneOfIt_returnsFalse) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1, MandatoryParam2, MandatoryParam3>::value<
            Arg1, MandatoryParam1, Arg2, MandatoryParam2>()), false );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMultiMandatoryParams_forArgsWithAllOfThem_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1, MandatoryParam2, MandatoryParam3>::value<
            Arg1, MandatoryParam1, Arg2, MandatoryParam2, MandatoryParam3>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withOneMandatoryParam_forArgsWithoutItAndWithTuple_returnsFalse) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1>::value<Arg1, std::tuple<Arg2, Arg3>>()), false );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMandatoryParam_forArgsWithItInTuple_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1>::value<
            Arg1, std::tuple<Arg2, MandatoryParam1, Arg3 >>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMandatoryParam_forArgsWithItInRecursiveTuple_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1>::value<
            Arg1, std::tuple<Arg2, std::tuple<MandatoryParam1, Arg3>>>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMandatoryParams_forArgsWithAllOfThemInSeveralTuples_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1, MandatoryParam2>::value<
            Arg1, std::tuple<MandatoryParam1, Arg2>, Arg3, std::tuple<MandatoryParam2, Arg4>>()), true );
}

TEST_F(MandatoryParamsTest, HasAllMandatoryParams_withMandatoryParams_forArgsWithAllOfThemInMixedPlaces_returnsTrue) {
    ASSERT_EQ( (HasAllMandatoryParams<MandatoryParam1, MandatoryParam2, MandatoryParam3>::value<
            Arg1, MandatoryParam1, std::tuple<Arg2, MandatoryParam2, std::tuple<MandatoryParam3, Arg3>>>()), true );
}

}
