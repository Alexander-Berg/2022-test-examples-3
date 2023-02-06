#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "subscribed_folder_mock.h"
#include "shared_folder_mock.h"

#include <macs/envelope_factory.h>
#include <macs/label_factory.h>
#include <macs/label_set.h>
#include "envelope_cmp.h"
#include "label_cmp.h"
#include "labels.h"

namespace doberman {
namespace logic {

bool operator == (const MessageCoordinates& lhs, const MessageCoordinates& rhs) {
    return lhs.folder == lhs.folder && lhs.mid == rhs.mid;
}

} // namespace logic
} // namespace doberman

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using namespace ::doberman::testing::labels;
using MsgCoord = ::doberman::logic::MessageCoordinates;
using Coord = ::doberman::logic::SharedFolderCoordinates;

struct LabelsCacheMock {
    MOCK_METHOD(::macs::LabelSet, fetch, (), (const));
};

struct SubscribedFolderTest : public Test {
    SubscribedFolderAccessMock mock;
    LabelsCacheMock labelsCache;
    auto makeFolder() {
        return doberman::logic::makeSubscribedFolder({""}, {{"owner"}, "fid"}, &mock,
                                                     doberman::LabelFilter({{},{}}));
    }

    auto makeLabelsCache(const ::macs::LabelSet& labels) {
        return ::doberman::meta::labels::LabelsCache(
                    labels,
                    [&]{ return this->labelsCache.fetch(); });
    }
};

TEST_F(SubscribedFolderTest, put_withEnvelope_callsLabelsFetchAndImplWithEnvelope) {
    auto folder = makeFolder();
    auto envelope = ::macs::EnvelopeFactory().mid("12345").release();
    auto envWithMimes = doberman::EnvelopeWithMimes(envelope, {});
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(makeSet()));
    EXPECT_CALL(mock, put(_, _, envWithMimes)).WillOnce(Return());
    folder.put(envWithMimes, makeLabelsCache(makeSet()));
}

TEST_F(SubscribedFolderTest, put_withEnvelope_resetsLabelsCacheIfLidNotFound) {
    auto folder = makeFolder();
    auto envelope = ::macs::EnvelopeFactory().mid("12345").addLabelIDs({"11"}).release();
    auto envWithMimes = doberman::EnvelopeWithMimes(envelope, {});
    InSequence seq;
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(makeSet(label("11"))));
    EXPECT_CALL(labelsCache, fetch()).WillOnce(Return(makeSet(label("11"))));
    EXPECT_CALL(mock, put(_, _, envWithMimes)).WillOnce(Return());
    folder.put(envWithMimes, makeLabelsCache(makeSet()));
}

TEST_F(SubscribedFolderTest, putQuiet_withEnvelope_callsLabelsFetchAndImplWithEnvelope) {
    auto folder = makeFolder();
    auto envelope = ::macs::EnvelopeFactory().mid("12345").release();
    auto envWithMimes = doberman::EnvelopeWithMimes(envelope, {});
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(makeSet()));
    EXPECT_CALL(mock, initPut(_, _, envWithMimes)).WillOnce(Return());
    folder.initPut(envWithMimes, makeLabelsCache(makeSet()));
}

TEST_F(SubscribedFolderTest, putQuiet_withEnvelope_resetsLabelsCacheIfLidNotFound) {
    auto folder = makeFolder();
    auto envelope = ::macs::EnvelopeFactory().mid("12345").addLabelIDs({"11"}).release();
    auto envWithMimes = doberman::EnvelopeWithMimes(envelope, {});
    InSequence seq;
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(makeSet(label("11"))));
    EXPECT_CALL(labelsCache, fetch()).WillOnce(Return(makeSet(label("11"))));
    EXPECT_CALL(mock, initPut(_, _, envWithMimes)).WillOnce(Return());
    folder.initPut(envWithMimes, makeLabelsCache(makeSet()));
}

TEST_F(SubscribedFolderTest, erase_withMids_callsImplWithMids) {
    auto folder = makeFolder();
    auto mids = ::macs::MidVec{"12345"};
    EXPECT_CALL(mock, erase(_, Coord{{"owner"}, "fid"}, mids, ::macs::Revision(100))).WillOnce(Return());
    folder.erase(mids, macs::Revision(100));
}

TEST_F(SubscribedFolderTest, mark_withMidAndLabels_callsImplWithMidAndLabels) {
    auto folder = makeFolder();
    auto mid = ::macs::Mid("12345");

    const auto labelSet = makeSet(
        label("11", Symbol::seen_label),
        label("13", "dummy")
    );

    EXPECT_CALL(mock, labels(_)).WillOnce(Return(labelSet));
    EXPECT_CALL(mock, mark(_, MsgCoord{{{"owner"}, "fid"}, mid},
                           Labels{label("11", Symbol::seen_label)},
                           ::macs::Revision(100)))
            .WillOnce(Return());
    folder.mark(mid, {label("1", Symbol::seen_label)}, ::macs::Revision(100));
}

TEST_F(SubscribedFolderTest, unmark_withMidAndLabels_callsImplWithMidAndLabels) {
    auto folder = makeFolder();
    auto mid = ::macs::Mid("12345");
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(makeSet(label("10"))));
    EXPECT_CALL(mock, unmark(_, MsgCoord{{{"owner"}, "fid"}, mid}, Labels{label("10")},
                             ::macs::Revision(100)))
            .WillOnce(Return());
    folder.unmark(mid, {label("10")}, ::macs::Revision(100));
}

TEST_F(SubscribedFolderTest, joinThreads_withTids_callsImplWithTids) {
    auto folder = makeFolder();
    auto tid = ::macs::ThreadId("100");
    auto joinTids = std::vector<::macs::ThreadId>{"100", "200"};

    EXPECT_CALL(mock, joinThreads(_, Coord{{"owner"}, "fid"}, tid, joinTids, ::macs::Revision(100)))
            .WillOnce(Return());
    folder.joinThreads(tid, joinTids, ::macs::Revision(100));
}

TEST_F(SubscribedFolderTest, envelopes_withImpl_returnsEnvelopesFromImpl) {
    auto folder = makeFolder();
    auto envelope = ::macs::EnvelopeFactory().mid("12345").release();
    EXPECT_CALL(mock, envelopes(_, _)).WillOnce(Return(std::vector<::macs::Envelope>{envelope}));
    EXPECT_EQ(folder.envelopes(), std::vector<::macs::Envelope>{envelope});
}

TEST_F(SubscribedFolderTest, labels_withImpl_returnsLabelsFromImpl) {
    auto folder = makeFolder();
    const auto label = ::macs::LabelFactory{}.lid("10").product();
    auto labels = ::macs::LabelSet{};
    labels["10"] = label;
    EXPECT_CALL(mock, labels(_)).WillOnce(Return(labels));
    EXPECT_EQ(folder.labels(), labels);
}

TEST_F(SubscribedFolderTest, clear_callsImplClear) {
    auto folder = makeFolder();
    EXPECT_CALL(mock, clear(_, Coord{{"owner"}, "fid"})).WillOnce(Return());
    folder.clear();
}

TEST_F(SubscribedFolderTest, lastSyncedImapId_callsImplLastSyncedImapId) {
    auto folder = makeFolder();
    EXPECT_CALL(mock, lastSyncedImapId(_, Coord{{"owner"}, "fid"})).WillOnce(Return(0));
    folder.lastSyncedImapId();
}

}
