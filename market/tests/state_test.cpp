#include <market/idx/datacamp/routines/tasks/complete_commands/state_attr.h>
#include <market/idx/datacamp/routines/tasks/complete_commands/state.pb.h>

#include <market/library/libyt/YtHelpers.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NDataCamp::NCompleteCommands;

TEST(StateAttr, TestSimple) {
    auto client = NYT::NTesting::CreateTestClient();
    TString nodePath = "//home/test_path";
    NMarket::NYTHelper::CreateCypressNode(*client, nodePath);
    auto state = LoadFromNodeAttr<TMetaState>(*client, nodePath);
    EXPECT_EQ(state.last_run_ts().seconds(), 0);
    state.mutable_last_run_ts()->set_seconds(27);
    SaveToNodeAttr<TMetaState>(*client, nodePath, state);
    state = LoadFromNodeAttr<TMetaState>(*client, nodePath);
    EXPECT_EQ(state.last_run_ts().seconds(), 27);
}
