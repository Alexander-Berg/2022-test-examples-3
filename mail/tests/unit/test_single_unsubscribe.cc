#include "wrap_mock.h"

#include <gtest/gtest.h>

#include "helper_macs.h"
#include "test_mocks.h"
#include "error.h"
#include <internal/unsubscribe_worker/single_unsubscribe.h>
#include <macs/io.h>
#include <macs_pg/subscription/unsubscribe_task_factory.h>

using namespace testing;

namespace york {
namespace tests {

struct SingleUnsubscribeTest: public Test {
    MacsMock<sync> macsOwner;
    MacsMock<sync> macsSubscriber;
    NiceMock<LoggerMock> logger;
    worker::UnsubscribeTask task;

    SingleUnsubscribeTest() {
        task = macs::UnsubscribeTaskFactory()
                .taskId(1)
                .taskRequestId("requestId")
                .ownerUid("owner_uid")
                .ownerFids({"1"})
                .subscriberUid("sub_uid")
                .rootSubscriberFid("fid")
                .release();
    }

    auto makeUnsubscribe() {
        return worker::operations::makeUnsubscribe(
                    macsOwner, macsSubscriber, task, log::make_log(log::none, &logger));
    }
};

TEST_F(SingleUnsubscribeTest, run_ifSubscriberFoldersEmpty_removesEverything_logsInfo) {
    macs::Folder folder = macs::FolderFactory().fid(task.rootSubscriberFid()).name("bbs").messages(0);
    macs::FoldersMap fs;
    fs.insert({folder.fid(), folder});

    InSequence seq;
    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{fs}));
    EXPECT_CALL(macsSubscriber, eraseCascade(folder, _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsOwner, removeSubscriptionsAndTask(task, _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    makeUnsubscribe().run(macs::io::use_sync);
}

TEST_F(SingleUnsubscribeTest, run_ifSubscriberFoldersNotEmpty_logsWarning_removesEverythingButFolders_logsInfo) {
    macs::Folder folder = macs::FolderFactory().fid(task.rootSubscriberFid()).name("bbs").messages(1);
    macs::FoldersMap fs;
    fs.insert({folder.fid(), folder});

    InSequence seq;
    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{fs}));
    EXPECT_CALL(logger, warning(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsOwner, removeSubscriptionsAndTask(task, _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    makeUnsubscribe().run(macs::io::use_sync);
}

TEST_F(SingleUnsubscribeTest, run_ifSubscriberFoldersNotFound_removesEverythingButFolders_logsInfo) {
    InSequence seq;
    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(logger, notice(_)).Times(1);

    EXPECT_CALL(logger, notice(_)).Times(1);
    EXPECT_CALL(macsOwner, removeSubscriptionsAndTask(task, _)).Times(1);
    EXPECT_CALL(logger, notice(_)).Times(1);

    makeUnsubscribe().run(macs::io::use_sync);
}

TEST_F(SingleUnsubscribeTest, run_withErrorInRemoveFolders_trows) {
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeUnsubscribe().run(macs::io::use_sync), boost::system::system_error);
}

TEST_F(SingleUnsubscribeTest, run_withErrorInGetAllFolders_trows) {
    InSequence seq;
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);

    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeUnsubscribe().run(macs::io::use_sync), boost::system::system_error);
}

TEST_F(SingleUnsubscribeTest, run_withErrorInEraseCascade_trows) {
    macs::Folder folder = macs::FolderFactory().fid(task.rootSubscriberFid()).name("bbs").messages(0);
    macs::FoldersMap fs;
    fs.insert({folder.fid(), folder});

    InSequence seq;
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);

    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{fs}));
    EXPECT_CALL(macsSubscriber, eraseCascade(folder, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeUnsubscribe().run(macs::io::use_sync), boost::system::system_error);
}

TEST_F(SingleUnsubscribeTest, run_withErrorInRemoveSubscriptionsAndTask_trows) {
    macs::Folder folder = macs::FolderFactory().fid(task.rootSubscriberFid()).name("bbs").messages(0);
    macs::FoldersMap fs;
    fs.insert({folder.fid(), folder});

    InSequence seq;
    EXPECT_CALL(macsSubscriber, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);

    EXPECT_CALL(macsSubscriber, resetFoldersCache()).Times(1);
    EXPECT_CALL(macsSubscriber, getAllFolders(_)).WillOnce(Return(macs::FolderSet{fs}));
    EXPECT_CALL(macsSubscriber, eraseCascade(folder, _)).Times(1);

    EXPECT_CALL(macsOwner, removeSubscriptionsAndTask(task, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_THROW(makeUnsubscribe().run(macs::io::use_sync), boost::system::system_error);
}

} // namespace tests
} // namespace york
