#ifndef DOBERMAN_TESTS_SUBSCRIBED_FOLDER_MOCK_H_
#define DOBERMAN_TESTS_SUBSCRIBED_FOLDER_MOCK_H_

#include <src/logic/subscribed_folder.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

struct SubscribedFolderAccessMock {
    using Coord = ::doberman::logic::SharedFolderCoordinates;
    using MsgCoord = ::doberman::logic::MessageCoordinates;

    auto makeContext(const Uid& uid) { return uid; }
    MOCK_METHOD(Revision, revision, (Uid, Coord), (const));
    MOCK_METHOD(void, put, (Uid, Coord, EnvelopeWithMimes), ());
    MOCK_METHOD(void, initPut, (Uid, Coord, EnvelopeWithMimes), ());
    MOCK_METHOD(void, erase, (Uid, Coord, macs::MidVec, Revision), ());
    MOCK_METHOD(void, mark, (Uid, MsgCoord, std::vector<Label>, Revision), ());
    MOCK_METHOD(void, unmark, (Uid, MsgCoord, std::vector<Label>, Revision), ());
    MOCK_METHOD(void, joinThreads, (Uid, Coord, ThreadId, std::vector<ThreadId>, Revision), ());
    MOCK_METHOD(void, clear, (Uid, Coord), ());
    MOCK_METHOD(std::vector<Envelope>, envelopes, (Uid, Coord), (const));
    MOCK_METHOD(LabelSet, labels, (Uid), (const));
    MOCK_METHOD(Label, createLabel, (Uid, Label), (const));
    MOCK_METHOD(int64_t, lastSyncedImapId, (Uid, Coord), (const));
};

struct SubscribedFolderMock {
    MOCK_METHOD(const Uid&, uid, (), (const));
    MOCK_METHOD(const Fid&, fid, (), (const));
    MOCK_METHOD(Revision, revision, (), (const));
    MOCK_METHOD(void, put, (Envelope), ());
    MOCK_METHOD(void, initPut, (Envelope), ());
    MOCK_METHOD(void, erase, (macs::MidVec), ());
    MOCK_METHOD(void, mark, (Mid, std::vector<Label>), ());
    MOCK_METHOD(void, unmark, (Mid, std::vector<Label>), ());
    MOCK_METHOD(void, joinThreads, (ThreadId, std::vector<ThreadId>), ());
    MOCK_METHOD(void, clear, (), ());
    MOCK_METHOD(std::vector<Envelope>, envelopes, (), (const));
    MOCK_METHOD(LabelSet, labels, (), (const));
    MOCK_METHOD(int64_t, lastSyncedImapId, (), (const));
    void push_back(Envelope e) { put(std::move(e)); }
};

} // namespace test
} // namespace doberman




#endif /* DOBERMAN_TESTS_SUBSCRIBED_FOLDER_MOCK_H_ */
