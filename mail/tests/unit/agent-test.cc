#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "change_queue_mock.h"
#include "shared_folder_mock.h"
#include "subscribed_folder_mock.h"
#include "subscription_mock.h"
#include "envelope_copier_mock.h"
#include "change_filter_mock.h"
#include "change_mock.h"
#include "log_mock.h"
#include "profiler_mock.h"
#include <gmock/gmock.h>
#include <src/logic/agent.h>
#include <type_traits>
#include "mock_extentions.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;

using State = ::doberman::SubscriptionState;

const ::doberman::Fid dummyFid = "42";

struct AgentMocks {
    ChangeQueueAccessMock changes;
    SubscribedFolderAccessMock dst;
    SharedFolderMock src;
    EnvelopeCopierMock<SharedFolderMock, ::doberman::logic::SubscribedFolder<SubscribedFolderAccessMock*>> copier;
    ChangeFilterMock<::doberman::logic::SubscribedFolder<SubscribedFolderAccessMock*>> applicable;
    ChangeMock change;
    NiceMock<LogMock> log;
    NiceMock<ProfilerMock> profiler;
    SubscriptionMock subscription;
};

struct AgentTest : public Test {
    AgentMocks mocks;
    SubscriptionMock::SubscribedFolder dst{doberman::logic::makeSubscribedFolder({""}, {{""}, ""}, &mocks.dst,
                                                                                 ::doberman::LabelFilter({{},{}}))};
    SubscriptionMock::ChangeQueue changes{doberman::logic::makeChangeQueue(dummySid, &mocks.changes)};
    ::doberman::service_control::RunStatus runStatus;
    AgentTest() {
        EXPECT_CALL(mocks.subscription, id()).WillRepeatedly(ReturnRef(dummySid));
        EXPECT_CALL(mocks.subscription, src()).WillRepeatedly(ReturnRef(mocks.src));
        EXPECT_CALL(mocks.subscription, dst()).WillRepeatedly(ReturnRef(dst));
        EXPECT_CALL(mocks.subscription, changes()).WillRepeatedly(ReturnRef(changes));
    }
    auto makeAgent() {
        return ::doberman::logic::makeAgent(
                &mocks.subscription,
                [&](auto& a1, auto& a2, auto){ mocks.copier(a1, a2);},
                [&](auto& c, auto& sf){ return mocks.applicable(c, sf);},
                ::doberman::make_log(logdog::none, &mocks.log),
                ::doberman::makeProfiler(&mocks.profiler),
                runStatus);
    }
};

TEST_F(AgentTest, agentRun_withSubscriptionInitFail_returns) {
    auto agent = makeAgent();
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::initFail));
    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscriptionSyncFail_returns) {
    auto agent = makeAgent();
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::syncFail));
    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscriptionDiscontinued_ClearsFolder_returns) {
    auto agent = makeAgent();
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::discontinued));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, finish()).WillOnce(Return(State::terminated));
    agent.run();
}

TEST_F(AgentTest, agentRun_withExceptionDuringClearing_marksSubscriptionAsFailedAndReturns) {
    auto agent = makeAgent();
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::discontinued));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Throw(std::runtime_error("initialization error")));
    EXPECT_CALL(mocks.subscription, fail("initialization error")).WillOnce(Return(State::clearFail));
    EXPECT_CALL(mocks.log, error(_)).WillOnce(Return());

    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscriptionTerminated_returns) {
    auto agent = makeAgent();
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::terminated));
    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscribedFolderNotInitialized_copyEnvelopesIntoSubscribedFolder) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::new_));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::init, true)));
    EXPECT_CALL(mocks.copier, copy(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::init));
    EXPECT_CALL(mocks.subscription, sync()).WillOnce(Return(State::sync));
    INTERRUPT_ON(mocks.changes, top(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withSubscribedFolderWithInterruptedInitialization_copyEnvelopesIntoSubscribedFolderAndReplyChangesUntilOwnerRevision) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::init));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::init, true)));
    EXPECT_CALL(mocks.copier, copy(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::init));
    EXPECT_CALL(mocks.dst, revision(_,_)).WillOnce(Return(macs::Revision(2)));

    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(1, {1}, mocks.change));
    EXPECT_CALL(mocks.change, apply(_)).WillOnce(Return(doberman::error_code()));
    EXPECT_CALL(mocks.changes, pop(_)).WillOnce(Return());

    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(20, {2}, mocks.change));
    EXPECT_CALL(mocks.change, apply(_)).WillOnce(Return(doberman::error_code()));
    EXPECT_CALL(mocks.changes, pop(_)).WillOnce(Return());

    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(30, {3}, mocks.change));

    EXPECT_CALL(mocks.subscription, sync()).WillOnce(Return(State::sync));
    INTERRUPT_ON(mocks.changes, top(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withSubscribedFolderNotInitializedAndUnsubscribedBeforeInit_finishSubscriptionWithoutInit) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::new_));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::discontinued, true)));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, finish()).WillOnce(Return(State::terminated));

    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscribedFolderNotInitializedAndUnsubscribedWhileInit_finishSubscriptionWithoutInit) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::new_));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::init, true)));
    EXPECT_CALL(mocks.copier, copy(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::discontinued));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, finish()).WillOnce(Return(State::terminated));

    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscribedFolderNotInitializedAndUnsubscribedWhileChangeStateToSync_finishSubscriptionWithoutInit) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::new_));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::init, true)));
    EXPECT_CALL(mocks.copier, copy(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::init));
    EXPECT_CALL(mocks.subscription, sync()).WillOnce(Return(State::discontinued));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, finish()).WillOnce(Return(State::terminated));

    agent.run();
}

TEST_F(AgentTest, agentRun_withExceptionDuringInitialization_marksSubscriptionAsFailedAndReturns) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::new_));
    EXPECT_CALL(mocks.subscription, init()).WillOnce(Return(std::make_tuple(State::init, true)));
    EXPECT_CALL(mocks.copier, copy(_,_)).WillOnce(Throw(std::runtime_error("initialization error")));
    EXPECT_CALL(mocks.subscription, fail("initialization error")).WillOnce(Return(State::initFail));
    EXPECT_CALL(mocks.log, error(_)).WillOnce(Return());

    agent.run();
}

TEST_F(AgentTest, agentRun_withSubscribedFolderInitialized_doNotCopyEnvelopesAndGetsChangeFromTop) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    INTERRUPT_ON(mocks.changes, top(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withNoChangeFromTopAndSubscribedFolderNoServed_returns) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(Return(nullptr));
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::discontinued));
    EXPECT_CALL(mocks.subscription, clear()).WillOnce(Return(std::make_tuple(State::clear, true)));
    EXPECT_CALL(mocks.dst, clear(_,_)).WillOnce(Return());
    EXPECT_CALL(mocks.subscription, finish()).WillOnce(Return(State::terminated));

    agent.run();
}

TEST_F(AgentTest, agentRun_withNoChangeFromTopAndSubscribedFolderServed_getsChangeFromTop) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(Return(nullptr));
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    INTERRUPT_ON(mocks.changes, top(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withChangeNotApplicable_popChange) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _)).WillOnce(Return(std::make_tuple(false, doberman::error_code{})));
    EXPECT_CALL(mocks.changes, pop(_)).WillOnce(Return());

    INTERRUPT_ON(mocks.subscription, state());

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withChangeNotApplicable_andErrorInGetRevision_failChangeAndLogError) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _))
            .WillOnce(Return(std::make_tuple(false, doberman::error_code{macs::error::noSuchFolder})));
    EXPECT_CALL(mocks.subscription, fail(_)).WillOnce(Return(State::syncFail));
    INTERRUPT_ON(mocks.log, error(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_withChangeApplicable_applyChange) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _)).WillOnce(Return(std::make_tuple(true, doberman::error_code{})));
    INTERRUPT_ON(mocks.change, apply(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_applyChangeSucceeded_popChange) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _)).WillOnce(Return(std::make_tuple(true, doberman::error_code{})));
    EXPECT_CALL(mocks.change, apply(_)).WillOnce(Return(doberman::error_code()));
    EXPECT_CALL(mocks.changes, pop(_)).WillOnce(Return());

    INTERRUPT_ON(mocks.subscription, state());

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_applyChangeFailed_failChangeAndLogError) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _)).WillOnce(Return(std::make_tuple(true, doberman::error_code{})));
    EXPECT_CALL(mocks.change, apply(_)).WillOnce(Return(doberman::error_code(macs::error::logic, "Apply error")));
    EXPECT_CALL(mocks.subscription, fail("Apply error")).WillOnce(Return(State::syncFail));
    INTERRUPT_ON(mocks.log, error(_));

    EXPECT_INTERRUPTED(agent.run());
}

TEST_F(AgentTest, agentRun_applyChangeFailed_returns) {
    auto agent = makeAgent();
    InSequence s;
    EXPECT_CALL(mocks.subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(mocks.changes, top(_)).WillOnce(ReturnChange(mocks.change));
    EXPECT_CALL(mocks.applicable, applicable(_, _)).WillOnce(Return(std::make_tuple(true, doberman::error_code{})));
    EXPECT_CALL(mocks.change, apply(_)).WillOnce(Return(doberman::error_code(macs::error::logic)));
    EXPECT_CALL(mocks.subscription, fail(_)).WillOnce(Return(State::syncFail));
    EXPECT_CALL(mocks.log, error(_)).WillOnce(Return());

    agent.run();
}

TEST_F(AgentTest, stoppingByRunStatus) {
    NiceMock<SubscriptionMock> subscription;

    auto agent = ::doberman::logic::makeAgent(
            &subscription,
            [&](auto&, auto&, auto){ },
            [&](auto& c, auto& sf){ return mocks.applicable(c, sf);},
            ::doberman::make_log(logdog::none, &mocks.log),
            ::doberman::makeProfiler(&mocks.profiler),
            runStatus);

    auto resetStatus = [this]() {
        runStatus.reset();
        return State::sync;
    };

    EXPECT_CALL(subscription, changes()).WillRepeatedly(ReturnRef(changes));
    EXPECT_CALL(subscription, id()).WillRepeatedly(ReturnRef(dummySid));
    EXPECT_CALL(subscription, src()).WillRepeatedly(ReturnRef(mocks.src));
    EXPECT_CALL(subscription, dst()).WillRepeatedly(ReturnRef(dst));
    EXPECT_CALL(subscription, changes()).WillRepeatedly(ReturnRef(changes));

    EXPECT_CALL(mocks.changes, top(_)).WillOnce(Return(nullptr));

    InSequence s;
    EXPECT_CALL(subscription, state()).WillOnce(Return(State::sync));
    EXPECT_CALL(subscription, state()).WillOnce(Invoke(resetStatus));

    agent.run();
}

}
