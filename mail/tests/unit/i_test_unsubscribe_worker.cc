#include "timer_mock.h"
#include "wrap_mock.h"

#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include "error.h"
#include <internal/unsubscribe_worker/worker.h>
#include <macs_pg/subscription/unsubscribe_task_factory.h>
#include <macs/io.h>

using namespace testing;

namespace york {
namespace tests {

using Task = worker::UnsubscribeTask;

struct TestConfig {
    UnsubscribeWorkerCfg worker;
    PgCfg pg;
};

struct UnsubscribeWorkerIntegrationTest: public Test {
    boost::asio::io_service io;
    RunStatusMock running;
    SharpeiClientMock<coro> sharpei;
    ShardMock<coro> shard;
    MacsMock<coro> mailbox;

    ShardMap shardMap;
    Task task;
    macs::Folder folder;
    macs::FolderSet userFolders;

    UnsubscribeWorkerIntegrationTest() {
        shardMap["1"] = Shard();

        task = macs::UnsubscribeTaskFactory()
                .taskId(1)
                .taskRequestId("requestId")
                .ownerUid("ownerUid")
                .ownerFids({"1"})
                .subscriberUid("subscriberUid")
                .rootSubscriberFid("fid")
                .release();

        folder = macs::FolderFactory().fid(task.rootSubscriberFid()).name("bbs").messages(0);

        macs::FoldersMap fs;
        fs.insert({folder.fid(), folder});
        userFolders = macs::FolderSet{fs};
    }

    void run() {
        TestConfig cfg {{0, 10}, {}};
        runUnsubscribeWorker(io, &cfg,
                [&](auto, auto){ return &sharpei; },
                [&](macs::Uid, auto, auto){ return &mailbox; },
                [&](ShardId, auto, auto){ return &shard; },
                running
        );
        io.run();
    }
};

TEST_F(UnsubscribeWorkerIntegrationTest, ifRunningFalse_doesNothing) {
    EXPECT_CALL(running, check()).WillOnce(Return(false));
    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInSharpeiStat_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInGetUnsubscribeTasks_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInRemoveFolders_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Return(std::vector<Task>{task}));

    EXPECT_CALL(mailbox, removeFolders(task.ownerUid(), task.ownerFids(), _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInGetAllFolders_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Return(std::vector<Task>{task}));

    EXPECT_CALL(mailbox, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(mailbox, resetFoldersCache()).Times(1);
    EXPECT_CALL(mailbox, getAllFolders(_))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInEraseCascade_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Return(std::vector<Task>{task}));

    EXPECT_CALL(mailbox, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(mailbox, resetFoldersCache()).Times(1);
    EXPECT_CALL(mailbox, getAllFolders(_)).WillOnce(Return(userFolders));
    EXPECT_CALL(mailbox, eraseCascade(folder, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, withErrorInRemoveSubscriptionsAndTask_runsAgain) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Return(std::vector<Task>{task}));

    EXPECT_CALL(mailbox, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(mailbox, resetFoldersCache()).Times(1);
    EXPECT_CALL(mailbox, getAllFolders(_)).WillOnce(Return(userFolders));
    EXPECT_CALL(mailbox, eraseCascade(folder, _)).Times(1);
    EXPECT_CALL(mailbox, removeSubscriptionsAndTask(task, _))
            .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

TEST_F(UnsubscribeWorkerIntegrationTest, positiveCase) {
    InSequence seq;
    EXPECT_CALL(running, check()).WillOnce(Return(true));

    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(shardMap));
    EXPECT_CALL(shard, getUnsubscribeTasks(_, _, _))
            .WillOnce(Return(std::vector<Task>{task}));

    EXPECT_CALL(mailbox, removeFolders(task.ownerUid(), task.ownerFids(), _)).Times(1);
    EXPECT_CALL(mailbox, resetFoldersCache()).Times(1);
    EXPECT_CALL(mailbox, getAllFolders(_)).WillOnce(Return(userFolders));
    EXPECT_CALL(mailbox, eraseCascade(folder, _)).Times(1);
    EXPECT_CALL(mailbox, removeSubscriptionsAndTask(task, _)).Times(1);

    EXPECT_CALL(running, check()).WillOnce(Return(false));

    run();
}

} // namespace tests
} // namespace york
