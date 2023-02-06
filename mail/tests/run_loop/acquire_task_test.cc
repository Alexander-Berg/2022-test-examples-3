#include <mail/ymod_queuedb_worker/tests/run_loop/test_classes.h>


namespace ymod_queuedb {

struct AcquireTaskTest: public WithTaskAndTypeTest { };

TEST_F(AcquireTaskTest, shouldReturnTask) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks  cb) {
        cb({ task });
    }));

    spawn([this] (boost::asio::yield_context yield) {
        EXPECT_EQ(loop->acquireTasks(contextHolder, yield)->taskId, *id);
    });
}

TEST_F(AcquireTaskTest, shouldReturnNullIfThereIsNoTask) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([] (const auto&, auto, const auto&, OnAcquireTasks  cb) {
        cb(std::vector<Task>());
    }));

    spawn([this] (boost::asio::yield_context yield) {
        EXPECT_EQ(loop->acquireTasks(contextHolder, yield), std::nullopt);
    });
}

TEST_F(AcquireTaskTest, shouldReturnNullInCaseOfError) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([] (const auto&, auto, const auto&, OnAcquireTasks  cb) {
        cb(make_error(WorkerError::unexpectedException, ""));
    }));

    spawn([this] (boost::asio::yield_context yield) {
        EXPECT_EQ(loop->acquireTasks(contextHolder, yield), std::nullopt);
    });
}

TEST_F(AcquireTaskTest, shouldThrowAnExceptionOnStrangeNubmerOfTasks) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks  cb) {
        cb({ task, task });
    }));

    spawn([this] (boost::asio::yield_context yield) {
        EXPECT_THROW(loop->acquireTasks(contextHolder, yield), std::runtime_error);
    });
}

}
