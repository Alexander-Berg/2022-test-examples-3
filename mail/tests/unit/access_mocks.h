#ifndef DOBERMAN_TESTS_ACCESS_MOCKS_H_
#define DOBERMAN_TESTS_ACCESS_MOCKS_H_

#include <gmock/gmock.h>
#include <src/access/change_queue.h>
#include <src/access/shared_folder.h>
#include <src/access/subscribed_folder.h>
#include <src/access/subscription.h>
#include <src/access/subscription_repository.h>
#include <src/logic/envelope_copier.h>

#include "change_mock.h"
#include "shared_folder_mock.h"

namespace doberman {
namespace testing {

using namespace ::testing;

using YieldCtx = boost::asio::yield_context;

struct ChangeQueueAccessImplMock {
    auto makeContext(const SubscriptionId& id) const { return id; }
    MOCK_METHOD(std::shared_ptr<const ::doberman::logic::Change>, top, (SubscriptionId, YieldCtx), (const));
    MOCK_METHOD(void, pop, (SubscriptionId, YieldCtx), ());
};

struct SubscribedFolderAccessImplMock {
    using Coord = ::doberman::logic::SharedFolderCoordinates;
    using MsgCoord = ::doberman::logic::MessageCoordinates;
    auto makeContext(Uid uid) const { return uid; }
    MOCK_METHOD(Revision, revision, (Uid, Coord, YieldCtx), (const));
    MOCK_METHOD(void, put, (Uid, Coord, EnvelopeWithMimes, YieldCtx), (const));
    MOCK_METHOD(void, initPut, (Uid, Coord, EnvelopeWithMimes, YieldCtx), (const));
    MOCK_METHOD(void, erase, (Uid, Coord, macs::MidVec, Revision, YieldCtx), (const));
    MOCK_METHOD(void, mark, (Uid, MsgCoord, std::vector<Label>, Revision, YieldCtx), (const));
    MOCK_METHOD(void, unmark, (Uid, MsgCoord, std::vector<Label>, Revision, YieldCtx), (const));
    MOCK_METHOD(void, joinThreads, (Uid, Coord, ThreadId, std::vector<ThreadId>, Revision, YieldCtx), (const));
    MOCK_METHOD(std::vector<Envelope>, envelopes, (Uid, Fid, YieldCtx), (const));
    MOCK_METHOD(LabelSet, labels, (Uid, YieldCtx), (const));
    MOCK_METHOD(Label, createLabel, (Uid, Label, YieldCtx), (const));
    MOCK_METHOD(void, clear, (Uid, Coord, YieldCtx), (const));
    MOCK_METHOD(int64_t, lastSyncedImapId, (Uid, Coord, YieldCtx), (const));
};

struct SharedFolderAccessImplMock {
    auto makeContext(doberman::logic::SharedFolderCoordinates c) const { return c.owner.uid; }
    MOCK_METHOD(Revision, revision, (Uid, YieldCtx), (const));
    MOCK_METHOD(std::vector<Envelope>, envelopes, (Uid, YieldCtx), (const));
    MOCK_METHOD(std::vector<EnvelopeWithMimes>, envelopesWithMimes, (Uid, int64_t, YieldCtx), (const));
    MOCK_METHOD(meta::labels::LabelsCache, labels, (Uid, YieldCtx), (const));
};

struct SubscriptionAccessImplMock {
    using State = ::doberman::SubscriptionState;
    auto makeContext(const SubscriptionId& id, const logic::Subscriber&) const { return id; }
    MOCK_METHOD(State, state, (SubscriptionId, YieldCtx), (const));
    MOCK_METHOD((std::tuple<State, bool>), init, (SubscriptionId, YieldCtx), (const));
    MOCK_METHOD(State, sync, (SubscriptionId, YieldCtx), (const));
    MOCK_METHOD(State, fail, (SubscriptionId, std::string, YieldCtx), (const));
    MOCK_METHOD(State, finish, (SubscriptionId, YieldCtx), (const));
    MOCK_METHOD((std::tuple<State, bool>), clear, (SubscriptionId, YieldCtx), (const));
};

struct SubscriptionRepositoryAccessImplMock{
    struct Ctx {};
    auto makeContext() const { return Ctx{}; }
    MOCK_METHOD(std::vector<::doberman::logic::SubscriptionData>, reserve, (Ctx, int, YieldCtx), (const));
    MOCK_METHOD(std::vector<::doberman::logic::SubscriptionData>, getReserved, (Ctx, YieldCtx), (const));
    MOCK_METHOD(void, release, (Ctx, SubscriptionId, YieldCtx), (const));
    MOCK_METHOD(void, decline, (Ctx, SubscriptionId, YieldCtx), (const));
};

struct EnvelopeCopierProxy {
    template <typename T1, typename T2, typename Yield, typename Condition>
    void operator()(const logic::SharedFolder<T1>& src, logic::SubscribedFolder<T2>& dst,
                    Condition&& stopReading, Yield) const {
        logic::TrivialEnvelopeCopier{}(src, dst, std::forward<Condition>(stopReading));
    }
};


struct AccessFactoryImplMock {
    MOCK_METHOD(SharedFolderAccessImplMock*, sharedFolder, (), (const));
    MOCK_METHOD(SubscribedFolderAccessImplMock*, subscribedFolder, (), (const));
    MOCK_METHOD(ChangeQueueAccessImplMock*, changeQueue, (), (const));
    MOCK_METHOD(SubscriptionAccessImplMock*, subscription, (), (const));
    MOCK_METHOD(SubscriptionRepositoryAccessImplMock*, subscriptionRepository, (), (const));
    EnvelopeCopierProxy envelopeCopier() const {return {};}
};

} // namespace test
} // namespace doberman

#endif /* DOBERMAN_TESTS_ACCESS_MOCKS_H_ */
