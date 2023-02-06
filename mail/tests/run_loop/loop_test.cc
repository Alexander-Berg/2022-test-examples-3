#include <mail/ymod_queuedb_worker/tests/run_loop/test_classes.h>


namespace ymod_queuedb {

struct LoopTest: public WithHandlersMapTest { };

TEST_F(LoopTest, shouldProcessSeveralTasks) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks cb) {
        cb({ task });
    }))
    .WillOnce(Invoke([] (const auto&, auto, const auto&, OnAcquireTasks cb) {
        cb(std::vector<Task>());
    }))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks cb) {
        cb({ task });
    }));

    EXPECT_CALL(*queue, completeTaskAsync(*id, worker, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, OnExecute cb) {
        cb();
    }))
    .WillOnce(Invoke([this] (auto, const auto&, const auto&, OnExecute cb) {
        contextHolder.cancel();
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->run(yield);
    });
}

TEST_F(LoopTest, shouldFailTaskWithUnknownType) {
    task.task = TaskType("");
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks cb) {
        cb({ task });
    }));

    EXPECT_CALL(*queue, failTaskAsync(*id, worker, _, Loop::MAX_TRIES_FOR_ERROR, Loop::DELAY_FOR_ERROR, _, _))
    .WillOnce(Invoke([this] (auto, const auto&, const auto&, auto, auto, const auto&, OnExecute cb) {
        contextHolder.cancel();
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->run(yield);
    });
}

TEST_F(LoopTest, shouldCatchExceptionWhenTaskFailed) {
    EXPECT_CALL(*queue, acquireTasksAsync(worker, tasksLimit, _, _))
    .WillOnce(Invoke([this] (const auto&, auto, const auto&, OnAcquireTasks cb) {
        cb({ task });
    }));

    EXPECT_CALL(*queue, completeTaskAsync(*id, worker, _, _))
    .WillOnce(Invoke([this] (auto, const auto&, const auto&, OnExecute) {
        contextHolder.cancel();
        throw std::exception();
    }));

    EXPECT_CALL(*queue, failTaskAsync(*id, worker, _, _, _, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, auto, auto, const auto&, OnExecute) {
        throw std::exception();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->run(yield);
    });
}

}
