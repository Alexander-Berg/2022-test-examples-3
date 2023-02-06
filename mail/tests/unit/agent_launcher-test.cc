#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "agent_launcher_mock.h"
#include "subscription_repository_mock.h"
#include "log_mock.h"
#include "profiler_mock.h"
#include "subscription_io.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;

struct AgentLauncherTest : public Test {
    AccessMock access;
    SpawnerMock launch;
    NiceMock<ProfilerMock> profiler;
    SubscriptionRepositoryMock repo;
    ::doberman::service_control::RunStatus runStatus;
    std::function<void()> resetStatus = [this]{ runStatus.reset();};

    auto makeAgentLauncher() {
        return ::doberman::logic::makeAgentLauncher(
                &repo,
                [&](auto work) mutable {
                    launch.launch([&](AccessMock& access) mutable { work(&access);});
                },
                ::logdog::none,
                ::doberman::makeProfiler(&profiler),
                ::doberman::LabelFilter({{},{}}),
                runStatus);
    }
};

auto createData() {
    return doberman::logic::SubscriptionData{ dummySid, {{"Owner"}, "fid"}, {"subscriber"} };
}

TEST_F(AgentLauncherTest, run_withNullResultFromRepoReserve_callsReserve) {
    auto launcher = makeAgentLauncher();
    InSequence s;
    EXPECT_CALL(repo, get()).WillOnce(Return(boost::none));
    EXPECT_CALL(repo, get()).WillOnce(Return(createData()));
    EXPECT_CALL(launch, launch(_)).WillOnce(InvokeWithoutArgs(resetStatus));
    launcher.run();
}

TEST_F(AgentLauncherTest, run_withSubscriptionFromRepoReserve_callsLaunch) {
    auto launcher = makeAgentLauncher();
    const auto data = createData();
    InSequence s;
    EXPECT_CALL(repo, get()).WillOnce(Return(data));
    EXPECT_CALL(launch, launch(_)).WillOnce(InvokeWithoutArgs(resetStatus));
    launcher.run();
}

TEST_F(AgentLauncherTest, stoppingByRunStatus) {
    auto launcher = makeAgentLauncher();
    const auto data = createData();
    EXPECT_CALL(repo, get()).WillRepeatedly(Return(data));

    InSequence s;
    EXPECT_CALL(launch, launch(_)).WillOnce(Return());
    EXPECT_CALL(launch, launch(_)).WillOnce(InvokeWithoutArgs(resetStatus));

    launcher.run();
}

}
