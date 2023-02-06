#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "wrap_yield.h"
#include "subscribed_folder_mock.h"
#include "shared_folder_mock.h"

#include <macs/envelope_factory.h>
#include <macs/label_factory.h>
#include <macs/label_set.h>
#include "envelope_cmp.h"
#include "label_cmp.h"
#include "folder_cmp.h"
#include "labels.h"
#include "mailbox_mock.h"
#include "profiler_mock.h"
#include <src/access_impl/shared_folder.h>

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using namespace ::doberman::testing::labels;
using MsgCoord = ::doberman::logic::MessageCoordinates;
using ::doberman::Uid;
using ::doberman::Fid;
using ::doberman::Mid;
using ::doberman::EnvelopeWithMimes;
using ProfilerMock = NiceMock<doberman::testing::ProfilerMock>;

struct SharedFolderAccessImplTest : public Test {
    using Profiler = ::doberman::profiling::Profiler<ProfilerMock*>;
    MailboxMock mock;
    MailboxWithMimesMock mockWithMimes;
    ProfilerMock prof;
    ::doberman::access_impl::SharedFolder<MailboxMock*, Profiler> accessImpl{&mock, ::doberman::makeProfiler(&prof)};

    static auto envelope(Mid mid, std::string imapId = "") {
        return ::macs::EnvelopeFactory{}.mid(mid).imapId(imapId).release();
    }
    static auto envelopeWithMimes(Mid mid) {
        return EnvelopeWithMimes(envelope(mid), {});
    };
};

TEST_F(SharedFolderAccessImplTest, makeContext_withCoordinates_callsFactoryMailboxWithOwnerUid) {
    EXPECT_CALL(mock, mailbox("OwnerUid")).WillOnce(Return(&mock));
    accessImpl.makeContext({{"OwnerUid"}, {}});
}

TEST_F(SharedFolderAccessImplTest, makeContext_withCoordinates_returnsContextWithMailboxAndFid) {
    EXPECT_CALL(mock, mailbox(_)).WillOnce(Return(&mock));
    auto ctx = accessImpl.makeContext({{{}}, "Fid"});
    EXPECT_EQ(ctx, std::make_tuple(&mock, "Fid"));
}

TEST_F(SharedFolderAccessImplTest, revision_withContext_clearsCacheAndReturnsFolderRevision) {
    InSequence s;
    EXPECT_CALL(mock, resetFoldersCache()).WillOnce(Return());
    ::macs::FoldersMap folders;
    folders.insert({"Fid", ::macs::FolderFactory{}.fid("Fid").revision(100)});
    EXPECT_CALL(mock, getAllFolders(_)).WillOnce(Return(::macs::FolderSet(folders)));

    EXPECT_EQ(accessImpl.revision(std::make_tuple(&mock, "Fid"), Yield()), ::macs::Revision(100));
}

TEST_F(SharedFolderAccessImplTest, envelopesWithMimes_withContext_returnsEnvelopesWithMimes) {
    ::macs::FoldersMap folders;
    ::macs::Folder folder = ::macs::FolderFactory{}.fid("Fid").imapUidNext(10).messages(2);
    folders.insert({"Fid", folder});

    EXPECT_CALL(mock, withMimes()).WillRepeatedly(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, count(_)).WillRepeatedly(ReturnRef(mockWithMimes));

    InSequence s;
    EXPECT_CALL(mock, resetFoldersCache()).WillOnce(Return());
    EXPECT_CALL(mock, getAllFolders(_)).WillOnce(Return(::macs::FolderSet(folders)));
    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(0)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Return(std::vector<EnvelopeWithMimes>{
        std::make_tuple(envelope("1", "1"), ::macs::MimeParts{}),
        std::make_tuple(envelope("2", "2"), ::macs::MimeParts{})
    }));

    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(2)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Return(std::vector<EnvelopeWithMimes>{}));

    auto ret = accessImpl.envelopesWithMimes(std::make_tuple(&mock, "Fid"), 0, Yield());

    std::vector<EnvelopeWithMimes> r{ret.begin(), ret.end()};
    EXPECT_THAT(r, UnorderedElementsAre(envelopeWithMimes("1"), envelopeWithMimes("2")));
}

TEST_F(SharedFolderAccessImplTest, envelopesWithMimes_withOutdatedLabelsCache_resetsCacheAndRetryQuery) {
    ::macs::FoldersMap folders;
    ::macs::Folder folder = ::macs::FolderFactory{}.fid("Fid").imapUidNext(10).messages(2);
    folders.insert({"Fid", folder});

    EXPECT_CALL(mock, withMimes()).WillRepeatedly(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, count(_)).WillRepeatedly(ReturnRef(mockWithMimes));

    InSequence s;
    EXPECT_CALL(mock, resetFoldersCache()).WillOnce(Return());
    EXPECT_CALL(mock, getAllFolders(_)).WillOnce(Return(::macs::FolderSet(folders)));
    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(0)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(0)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Return(std::vector<EnvelopeWithMimes>{}));

    accessImpl.envelopesWithMimes(std::make_tuple(&mock, "Fid"), 0, Yield());
}

TEST_F(SharedFolderAccessImplTest, envelopesWithMimes_withOutdatedLabelsCache_throwsOnUpdatedLabelsCacheMiss) {
    ::macs::FoldersMap folders;
    ::macs::Folder folder = ::macs::FolderFactory{}.fid("Fid").imapUidNext(10).messages(2);
    folders.insert({"Fid", folder});

    EXPECT_CALL(mock, withMimes()).WillRepeatedly(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, count(_)).WillRepeatedly(ReturnRef(mockWithMimes));

    InSequence s;
    EXPECT_CALL(mock, resetFoldersCache()).WillOnce(Return());
    EXPECT_CALL(mock, getAllFolders(_)).WillOnce(Return(::macs::FolderSet(folders)));
    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(0)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mockWithMimes, byImapId(folder.fid())).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, from(0)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, to(10)).WillOnce(ReturnRef(mockWithMimes));
    EXPECT_CALL(mockWithMimes, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));

    EXPECT_THROW(accessImpl.envelopesWithMimes(std::make_tuple(&mock, "Fid"), 0, Yield()),
            std::exception);
}

TEST_F(SharedFolderAccessImplTest, labels_withContext_returnsLabelsCacheWithFetchHandler) {
    InSequence s;
    EXPECT_CALL(mock, getAllLabels(_)).WillOnce(Return(makeSet(label("1"))));
    EXPECT_CALL(mock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mock, getAllLabels(_)).WillOnce(Return(makeSet(
        label("1"), label("2")
    )));

    auto cache = accessImpl.labels(std::make_tuple(&mock, "Fid"), Yield());
    EXPECT_EQ(*cache, makeSet(label("1")));
    EXPECT_EQ(*(cache.update()), makeSet(label("1"), label("2")));
}

}
