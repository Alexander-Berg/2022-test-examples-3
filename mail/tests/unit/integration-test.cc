#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "access_mocks.h"
#include "log_mock.h"
#include "profiler_mock.h"
#include "subscription_io.h"
#include "subscription_mock.h"
#include "change_mock.h"

#include <src/access/spawn.h>
#include <macs/envelope_factory.h>
#include "envelope_cmp.h"
#include "labels.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::logic::SubscriptionData;
using State = ::doberman::SubscriptionState;

struct IntegrationTest : public Test {
    ChangeQueueAccessImplMock changes;
    SubscribedFolderAccessImplMock dst;
    SharedFolderAccessImplMock src;
    SubscriptionAccessImplMock subscription;
    SubscriptionRepositoryAccessImplMock repo;

    AccessFactoryImplMock factory;

    ChangeMock change;

    NiceMock<LogMock> log;
    NiceMock<ProfilerMock> profiler;

    boost::asio::io_service ios;
    doberman::service_control::RunStatus runStatus;

    IntegrationTest() {
        EXPECT_CALL(factory, changeQueue()).WillRepeatedly(Return(&changes));
        EXPECT_CALL(factory, subscribedFolder()).WillRepeatedly(Return(&dst));
        EXPECT_CALL(factory, sharedFolder()).WillRepeatedly(Return(&src));
        EXPECT_CALL(factory, subscription()).WillRepeatedly(Return(&subscription));
        EXPECT_CALL(factory, subscriptionRepository()).WillRepeatedly(Return(&repo));
    }

    auto envelope(macs::Mid mid) const {
        return macs::EnvelopeFactory().mid(mid).release();
    }

    auto makeEnvelopes(std::vector<macs::Mid> mids) const {
        std::vector<macs::Envelope> res;
        boost::transform(mids, std::back_inserter(res), [&](auto& mid) {
            return this->envelope(mid);
        });
        return res;
    }

    auto makeEnvelopesWithMimes(std::vector<macs::Mid> mids) const {
        std::vector<doberman::EnvelopeWithMimes> res;
        boost::transform(mids, std::back_inserter(res), [&](auto& mid) {
            return doberman::EnvelopeWithMimes(this->envelope(mid), {});
        });
        return res;
    }

    void run() {
        doberman::access::spawn(ios, 100500, doberman::access::makeAccessFactory(&factory),
                                ::doberman::make_log(logdog::none, &log), ::doberman::makeProfiler(&profiler),
                                ::doberman::LabelFilter({{}, {"user"}}), runStatus);
        ios.run();
    }
};

template <typename ... Args>
inline auto coro_yield(boost::asio::io_service& ios, Args&& ... args) {
    auto t = std::make_tuple(std::forward<Args>(args)...);
    ios.post(std::get<boost::asio::yield_context>(t));
}

template <typename T = void>
struct AsyncReturnImpl {
    boost::asio::io_service* ios;
    T retval;
    template <typename ... Args>
    T operator() (Args&& ... args) const {
        coro_yield(*ios, std::forward<Args>(args)...);
        return retval;
    }
};

template <>
struct AsyncReturnImpl<void> {
    boost::asio::io_service* ios;
    template <typename ... Args>
    void operator() (Args&& ... args) const {
        coro_yield(*ios, std::forward<Args>(args)...);
    }
};

template <typename T>
auto ReturnAsync(boost::asio::io_service& ios, T retval) {
    return Invoke(AsyncReturnImpl<T>{std::addressof(ios), std::move(retval)});
}

auto ReturnAsync(boost::asio::io_service& ios) {
    return Invoke(AsyncReturnImpl<>{std::addressof(ios)});
}

#define AGENT_START Sequence aseq

#define START_AGENT_LANE { Sequence aseq
#define END_AGENT_LANE }

#define SET_SRC_LABELS(...) \
        auto srcLabels = labels::makeSet(__VA_ARGS__);\
        EXPECT_CALL(src, labels("OwnerUid", _))\
            .WillRepeatedly(ReturnAsync(ios, labels::makeLabelsCache(srcLabels)))

#define SET_DST_LABELS(...) \
        auto dstLabels = labels::makeSet(__VA_ARGS__);\
        EXPECT_CALL(dst, labels("SubscriberUid", _))\
            .WillRepeatedly(ReturnAsync(ios, dstLabels))

#define AGENT_REQUEST_STATE_AND_GET(state_)\
    EXPECT_CALL(subscription, state(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, State::state_))

#define AGENT_SUBSCRIPTION_INIT\
    EXPECT_CALL(subscription, init(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, std::make_tuple(State::init, true)))

#define AGENT_SUBSCRIPTION_SYNC\
    EXPECT_CALL(subscription, sync(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, State::sync))

#define AGENT_SUBSCRIPTION_FAIL(state_, message_)\
    EXPECT_CALL(subscription, fail(dummySid, message_, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, State::state_))

#define AGENT_SUBSCRIPTION_FINISH\
    EXPECT_CALL(subscription, finish(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, State::terminated))

#define AGENT_SUBSCRIPTION_CLEAR\
    EXPECT_CALL(subscription, clear(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, std::make_tuple(State::clear, true)))

#define AGENT_REQUEST_SRC_ENVELOPES_AND_GET(envelopes_)\
    EXPECT_CALL(src, envelopes("OwnerUid", _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, envelopes_))

#define AGENT_REQUEST_SRC_ENVELOPES_WITH_MIMES_AND_GET(envWithMimes_)\
    EXPECT_CALL(dst, lastSyncedImapId("SubscriberUid", _, _)).InSequence(aseq)\
            .WillOnce(ReturnAsync(ios, 10));\
    EXPECT_CALL(src, envelopesWithMimes("OwnerUid", 10, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, envWithMimes_))

#define AGENT_PUT_INTO_DST_ENVELOPE(envelope_)\
    EXPECT_CALL(dst, initPut("SubscriberUid", _, envelope_, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios))

#define AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(revision_)\
    EXPECT_CALL(dst, revision("SubscriberUid", _, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, revision_))

#define AGENT_POP_CHANGE EXPECT_CALL(changes, pop(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios))

#define AGENT_REQUEST_AND_GET_CHANGE(change)\
    EXPECT_CALL(changes, top(dummySid, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios, change))

#define AGENT_RELEASES_SUBSCRIPTION\
        EXPECT_CALL(repo, release(_, _, _))\
            .InSequence(aseq)\
            .WillOnce(ReturnAsync(ios))

#define AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET\
        EXPECT_CALL(repo, release(_, _, _))\
            .InSequence(aseq)\
            .WillOnce(InvokeWithoutArgs([&]{runStatus.reset();}))

#define START_LAUNCHER_LANE { Sequence lseq
#define END_LAUNCHER_LANE }

#define LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION\
        EXPECT_CALL(repo, getReserved(_,_)).InSequence(lseq)\
            .WillOnce(ReturnAsync(ios, std::vector<SubscriptionData>{\
                        {dummySid, {{"OwnerUid"}, "Fid"}, {"SubscriberUid"}}\
                    }))

#define LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING\
        EXPECT_CALL(repo, reserve(_, _, _))\
            .InSequence(lseq)\
            .WillRepeatedly(ReturnAsync(ios, std::vector<SubscriptionData>{}))


#define AGENT_LOG_ERROR EXPECT_CALL(log, error(_)).InSequence(aseq).WillOnce(Return())

#define AGENT_CLEARS_DST_FOLDER\
    EXPECT_CALL(dst, clear("SubscriberUid", _, _)).InSequence(aseq)\
        .WillOnce(ReturnAsync(ios))


TEST_F(IntegrationTest, doberman_withNewSubscriptionProcessItThenDiscontinued) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        SET_SRC_LABELS();
        SET_DST_LABELS();

        AGENT_REQUEST_STATE_AND_GET(new_);

        AGENT_SUBSCRIPTION_INIT;

        AGENT_REQUEST_SRC_ENVELOPES_WITH_MIMES_AND_GET(makeEnvelopesWithMimes({"1", "2"}));

        AGENT_REQUEST_STATE_AND_GET(init);
        AGENT_PUT_INTO_DST_ENVELOPE((doberman::EnvelopeWithMimes(envelope("1"), {})));
        AGENT_REQUEST_STATE_AND_GET(init);
        AGENT_PUT_INTO_DST_ENVELOPE((doberman::EnvelopeWithMimes(envelope("2"), {})));
        AGENT_REQUEST_STATE_AND_GET(init);

        AGENT_SUBSCRIPTION_SYNC;

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{0});
        EXPECT_CALL(change, apply(_)).InSequence(aseq).WillOnce(Return(macs::error_code{}));
        AGENT_POP_CHANGE;

        AGENT_REQUEST_STATE_AND_GET(discontinued);
        AGENT_SUBSCRIPTION_CLEAR;
        AGENT_CLEARS_DST_FOLDER;
        AGENT_SUBSCRIPTION_FINISH;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withNewSubscriptionUnsubscribedWhileInit_stopProcessItThenDiscontinued) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        SET_SRC_LABELS();
        SET_DST_LABELS();

        AGENT_REQUEST_STATE_AND_GET(new_);

        AGENT_SUBSCRIPTION_INIT;

        AGENT_REQUEST_SRC_ENVELOPES_WITH_MIMES_AND_GET(makeEnvelopesWithMimes({"1", "2"}));

        AGENT_REQUEST_STATE_AND_GET(init);
        AGENT_PUT_INTO_DST_ENVELOPE((doberman::EnvelopeWithMimes(envelope("1"), {})));
        AGENT_REQUEST_STATE_AND_GET(discontinued);
        AGENT_REQUEST_STATE_AND_GET(discontinued);

        AGENT_SUBSCRIPTION_CLEAR;
        AGENT_CLEARS_DST_FOLDER;
        AGENT_SUBSCRIPTION_FINISH;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withInitSubscriptionError_setsItInInitFail) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        SET_SRC_LABELS();
        SET_DST_LABELS();

        AGENT_REQUEST_STATE_AND_GET(new_);

        AGENT_SUBSCRIPTION_INIT;

        EXPECT_CALL(dst, lastSyncedImapId("SubscriberUid", _, _))
            .InSequence(aseq)
            .WillOnce(ReturnAsync(ios, 0));
        EXPECT_CALL(src, envelopesWithMimes("OwnerUid", _, _))
            .InSequence(aseq)
            .WillOnce(Throw(std::logic_error("error")));

        AGENT_SUBSCRIPTION_FAIL(initFail, "error");
        AGENT_LOG_ERROR;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withSyncSubscriptionProcessItThenDiscontinued_setsItTerminated) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        AGENT_REQUEST_STATE_AND_GET(sync);

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{0});
        EXPECT_CALL(change, apply(_)).InSequence(aseq).WillOnce(Return(macs::error_code{}));
        AGENT_POP_CHANGE;

        AGENT_REQUEST_STATE_AND_GET(discontinued);
        AGENT_SUBSCRIPTION_CLEAR;
        AGENT_CLEARS_DST_FOLDER;
        AGENT_SUBSCRIPTION_FINISH;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withMigrateSubscription_processItUntilNoChangesLeftAndThenSetsItTerminated) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        AGENT_REQUEST_STATE_AND_GET(migrate);

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{0});
        EXPECT_CALL(change, apply(_)).InSequence(aseq).WillOnce(Return(macs::error_code{}));
        AGENT_POP_CHANGE;

        AGENT_REQUEST_STATE_AND_GET(migrate);
        AGENT_REQUEST_AND_GET_CHANGE(nullptr);
        AGENT_SUBSCRIPTION_FINISH;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withSyncSubscriptionError_setsItInSyncFail) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        AGENT_REQUEST_STATE_AND_GET(sync);

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{0});
        EXPECT_CALL(change, apply(_)).InSequence(aseq).WillOnce(Return(macs::error_code{macs::error::logic, "db error"}));

        AGENT_SUBSCRIPTION_FAIL(syncFail, "db error");
        AGENT_LOG_ERROR;
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withWorkerIdOutdatedOnChangeApply_stopsImmediately) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        AGENT_REQUEST_STATE_AND_GET(sync);

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{0});
        EXPECT_CALL(change, apply(_)).InSequence(aseq).WillOnce(
                InvokeWithoutArgs([] () -> mail_errors::error_code {
                    throw ::doberman::WorkerIdOutdated("workerId");
                }));
    END_AGENT_LANE;

    run();
}

TEST_F(IntegrationTest, doberman_withSyncedRevisionGreaterThanChangeRevision_ignoresChange) {

    START_LAUNCHER_LANE;
        LAUNCHER_ASKS_FOR_RESERVED_SUBSCRIPTIONS_AND_GET_SUBSCRIPTION;
        LAUNCHER_RESERVE_SUBSCRIPTION_AND_GET_NOTHING;
    END_LAUNCHER_LANE;

    START_AGENT_LANE;
        AGENT_REQUEST_STATE_AND_GET(sync);

        AGENT_REQUEST_AND_GET_CHANGE(makeChange(123, {1}, change));
        AGENT_REQUEST_DST_SYNCED_REVISION_AND_GET(doberman::Revision{2});
        AGENT_POP_CHANGE;
        AGENT_REQUEST_STATE_AND_GET(clear);
        AGENT_RELEASES_SUBSCRIPTION_THEN_RUN_STATUS_RESET;
    END_AGENT_LANE;

    run();
}

}
