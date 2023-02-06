#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "wrap_yield.h"
#include "timer.h" // must be included before all - substitute for src/access_impl/timer.h

#include <macs/envelope_factory.h>
#include <macs/label_factory.h>
#include <macs/label_set.h>
#include "envelope_cmp.h"
#include "label_cmp.h"
#include "labels.h"
#include "mailbox_mock.h"
#include "folder_cmp.h"
#include "profiler_mock.h"
#include "worker_id.h"
#include <src/access_impl/subscribed_folder.h>

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using namespace ::doberman::testing::labels;
using Coord = ::doberman::logic::SharedFolderCoordinates;
using MsgCoord = ::doberman::logic::MessageCoordinates;
using ::doberman::Uid;
using ::doberman::Fid;
using ::doberman::Mid;
using ProfilerMock = NiceMock<doberman::testing::ProfilerMock>;

using doberman::testing::WorkerId;

struct RevisionCacheMock {
    MOCK_METHOD(boost::optional<macs::Revision>, get, (const Uid&, const Coord&), (const));
    MOCK_METHOD(void, set, (const Uid&, const Coord&, macs::Revision), (const));
};

struct SubscribedFolderAccessImplTest : public Test {
    using Profiler = ::doberman::profiling::Profiler<ProfilerMock*>;
    using SubscribedFolder = ::doberman::access_impl::SubscribedFolder<
            MailboxMock*, logdog::none_t, WorkerId, RevisionCacheMock, Profiler>;

    static auto envelope(Mid mid) {
        return ::macs::EnvelopeFactory{}.mid(mid).release();
    }

    static auto envelopeWithMimes(Mid mid) {
        return doberman::EnvelopeWithMimes(envelope(mid), {});
    }

    static auto label(std::string name, std::string color, ::macs::Label::Type type) {
        return ::macs::LabelFactory().name(name).color(color).type(type).product();
    }

    const std::size_t chunkSize = 500;
    SubscribedFolder makeAccess(bool valid) {
        return SubscribedFolder(&mock, logdog::none,
                doberman::access_impl::Retries{3, 2},
            WorkerId("workerId", [valid]{return valid;}),
            revisionCache, ::doberman::makeProfiler(&prof), chunkSize);
    }

    static Coord makeCoord() { return {{"Uid"}, {"Fid"}}; }
    static MsgCoord makeMsgCoord() { return {{{"Uid"}, {"Fid"}}, {"Mid"}}; }

    StrictMock<MailboxMock> mock;
    RevisionCacheMock revisionCache;
    SubscribedFolder accessImpl = makeAccess(true);
    ProfilerMock prof;
};

TEST_F(SubscribedFolderAccessImplTest, makeContext_withUid_callsFactoryMailboxWithUid) {
    EXPECT_CALL(mock, mailbox("Uid")).WillOnce(Return(&mock));
    accessImpl.makeContext("Uid");
}

TEST_F(SubscribedFolderAccessImplTest, revision_withContext_getsValueFromCache) {
    EXPECT_CALL(revisionCache, get("Uid", makeCoord())).WillOnce(Return(boost::make_optional(::macs::Revision(100))));

    EXPECT_EQ(accessImpl.revision(std::make_tuple(&mock, "Uid"), makeCoord(), Yield()), ::macs::Revision(100));
}

TEST_F(SubscribedFolderAccessImplTest, revision_withContext_withEmptyCacheGetsFolderRevisionAndUpdatesCache) {
    EXPECT_CALL(revisionCache, get("Uid", makeCoord())).WillOnce(Return(boost::none));
    EXPECT_CALL(mock, getSyncedRevision("Uid", "Fid", _)).WillOnce(Return(boost::make_optional(::macs::Revision(100))));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);

    EXPECT_EQ(accessImpl.revision(std::make_tuple(&mock, "Uid"), makeCoord(), Yield()), ::macs::Revision(100));
}

TEST_F(SubscribedFolderAccessImplTest, revision_withContext_throwExceptionOnEmptyRevision) {
    EXPECT_CALL(revisionCache, get("Uid", makeCoord())).WillOnce(Return(boost::none));
    EXPECT_CALL(mock, getSyncedRevision("Uid", "Fid", _)).WillOnce(Return(boost::none));

    EXPECT_THROW(accessImpl.revision(std::make_tuple(&mock, "Uid"), makeCoord(), Yield()), macs::system_error);
}

TEST_F(SubscribedFolderAccessImplTest, envelopes_withContext_returnsEnvelopes) {
    EXPECT_CALL(mock, getEnvelopes("Uid", "Fid", _)).WillOnce(Return(std::vector<::macs::Envelope>{
            envelope("2"),
            envelope("1")
    }));

    auto ret = accessImpl.envelopes(std::make_tuple(&mock, "Uid"), makeCoord(), Yield());
    EXPECT_THAT(ret, UnorderedElementsAre(envelope("1"), envelope("2")));
}

TEST_F(SubscribedFolderAccessImplTest, labels_withContext_returnsLabels) {
    InSequence s;
    EXPECT_CALL(mock, getAllLabels(_)).WillOnce(Return(makeSet(
        ::label("1"), ::label("2")
    )));

    EXPECT_EQ(accessImpl.labels(std::make_tuple(&mock, "Uid"), Yield()), makeSet(::label("1"), ::label("2")));
}

TEST_F(SubscribedFolderAccessImplTest, labels_withContext_createsLabel) {
    const auto lblType = ::macs::Label::Type::user;
    const auto lbl = label("label", "blue", lblType);
    EXPECT_CALL(mock, getOrCreateLabel("label", "blue", lblType, _)).WillOnce(Return(lbl));
    EXPECT_EQ(accessImpl.createLabel(std::make_tuple(&mock, "Uid"), lbl, Yield()), lbl);
}

TEST_F(SubscribedFolderAccessImplTest, put_withContext_putMessage_updatesCache) {
    const auto env = envelope("Mid");
    EXPECT_CALL(mock, syncMessage("Uid", env, _, _, _, _)).WillOnce(Return(::macs::SyncMessage{"mid", 1, "omid", 100}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.put(std::make_tuple(&mock, "Uid"), makeCoord(),
            doberman::EnvelopeWithMimes(env, {}), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, put_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    const auto env = envelopeWithMimes("Mid");
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.put(std::make_tuple(&mock, "Uid"), makeCoord(), env, Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, initPut_withContext_putQuietMessage_updatesCache) {
    const auto env = envelope("Mid");
    EXPECT_CALL(mock, syncMessageQuiet("Uid", env, _, _, _, _)).WillOnce(Return(::macs::SyncMessage{"mid", 1, "omid", 100}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.initPut(std::make_tuple(&mock, "Uid"), makeCoord(),
            doberman::EnvelopeWithMimes(env, {}), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, initPut_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    const auto env = envelope("Mid");
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.initPut(std::make_tuple(&mock, "Uid"), makeCoord(),
            doberman::EnvelopeWithMimes(env, {}), Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, erase_withContext_erasesMessage_updatesCache) {
    ::macs::MidVec mids{"Mid"};
    EXPECT_CALL(mock, deleteMessages("Uid", "Fid", mids, ::macs::Revision(100), _))
            .WillOnce(Return(::macs::UpdateMessagesResult{100, 1}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.erase(std::make_tuple(&mock, "Uid"), makeCoord(), mids, ::macs::Revision(100), Yield());
}


TEST_F(SubscribedFolderAccessImplTest, erase_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.erase(std::make_tuple(&mock, "Uid"), makeCoord(), {}, ::macs::Revision(100), Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, mark_withContext_markMessage_updates_cache) {
    ::macs::MidVec mids{"Mid"};
    EXPECT_CALL(mock, labelMessages("Uid", "Fid", mids, ::macs::Revision(100), _, _))
            .WillOnce(Return(::macs::UpdateMessagesResult{100, 1}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.mark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
            Labels{::label("1")}, ::macs::Revision(100), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, mark_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.mark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
                    Labels{::label("1")}, ::macs::Revision(100), Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, mark_withEmptyLabels_doesNothing) {
    accessImpl.mark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
            Labels(), ::macs::Revision(100), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, unmark_withContext_unmarkMessage_updatesCache) {
    ::macs::MidVec mids{"Mid"};
    EXPECT_CALL(mock, unlabelMessages("Uid", "Fid", mids, ::macs::Revision(100), _, _))
            .WillOnce(Return(::macs::UpdateMessagesResult{100, 1}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.unmark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
            Labels{::label("1")}, ::macs::Revision(100), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, unmark_withEmptyLabels_doesNothing) {
    accessImpl.unmark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
            Labels(), ::macs::Revision(100), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, unmark_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.unmark(std::make_tuple(&mock, "Uid"), makeMsgCoord(),
                    Labels(), ::macs::Revision(100), Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, joinThreads_withContext_joinThreads_updatesCache) {
    auto tid = ::macs::ThreadId("100");
    auto joinTids = std::vector<::macs::ThreadId>{"200", "300"};
    EXPECT_CALL(mock, joinThreads("Uid", "Fid", tid, joinTids, ::macs::Revision(100), _))
            .WillOnce(Return(::macs::UpdateMessagesResult{100, 1}));
    EXPECT_CALL(revisionCache, set("Uid", makeCoord(), ::macs::Revision(100))).Times(1);
    accessImpl.joinThreads(std::make_tuple(&mock, "Uid"), makeCoord(), tid, joinTids, ::macs::Revision(100), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, joinThreads_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    auto accessImpl = makeAccess(false);
    auto tid = ::macs::ThreadId("100");
    auto joinTids = std::vector<::macs::ThreadId>{"200", "300"};
    EXPECT_THROW(
            accessImpl.joinThreads(std::make_tuple(&mock, "Uid"), makeCoord(),
                    tid, joinTids, ::macs::Revision(100), Yield()),
            doberman::WorkerIdOutdated);
}

TEST_F(SubscribedFolderAccessImplTest, clearChunk_withContext_callsGetSharedMidsWithChunkSizeAndRemoveWithMids_whileDeletedMidsNotEmpty) {
    InSequence seq;
    EXPECT_CALL(mock, getSyncedMids("Uid", "Fid", chunkSize, _))
            .WillOnce(Return(::macs::Mids{"1", "2"}));
    EXPECT_CALL(mock, remove(::macs::Mids{"1", "2"}, _)).Times(1);

    EXPECT_CALL(mock, getSyncedMids("Uid", "Fid", chunkSize, _))
            .WillOnce(Return(::macs::Mids{"3"}));
    EXPECT_CALL(mock, remove(::macs::Mids{"3"}, _)).Times(1);

    EXPECT_CALL(mock, getSyncedMids("Uid", "Fid", chunkSize, _))
            .WillOnce(Return(::macs::Mids{}));

    accessImpl.clear(std::make_tuple(&mock, "Uid"), makeCoord(), Yield());
}

TEST_F(SubscribedFolderAccessImplTest, clearChunk_withOutdatedWorkerId_throwsWorkerIdOutdated) {
    auto accessImpl = makeAccess(false);
    EXPECT_THROW(
            accessImpl.clear(std::make_tuple(&mock, "Uid"), makeCoord(), Yield()),
            doberman::WorkerIdOutdated);
}

}
