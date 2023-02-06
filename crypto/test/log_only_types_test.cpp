#include <crypta/cm/services/api/lib/logic/upload/log_only_types.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <util/generic/vector.h>

using namespace NCrypta::NCm::NApi;

TEST(TLogOnlyTypes, ValidRegexp) {
    const TLogOnlyTypes logOnlyTypes(TVector<TString>({"abc", "dmp.*"}));

    ASSERT_TRUE(logOnlyTypes.IsLogOnly("abc"));
    ASSERT_TRUE(logOnlyTypes.IsLogOnly("dmpxxx"));
    ASSERT_TRUE(logOnlyTypes.IsLogOnly("dmp"));

    ASSERT_FALSE(logOnlyTypes.IsLogOnly("abcd"));
    ASSERT_FALSE(logOnlyTypes.IsLogOnly("admpxxx"));
}

TEST(TLogOnlyTypes, InvalidRegexp) {
    ASSERT_THROW(TLogOnlyTypes(TVector<TString>({"(abc"})), yexception);
}
