#include <mail/ymod_queuedb_worker/tests/run_loop/test_classes.h>


namespace ymod_queuedb {

struct ProcessTaskTest: public WithMockedRefreshCoroTest { };

TEST_F(ProcessTaskTest, shouldCompleteTask) {
    EXPECT_CALL(*queue, completeTaskAsync(*id, worker, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, OnExecute cb) {
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->processTask(task, handler, contextHolder, yield);
    });
}

TEST_F(ProcessTaskTest, shouldFailTaskOnError) {
    result = yamail::make_unexpected(make_error(WorkerError::unexpectedException, ""));

    EXPECT_CALL(*queue, failTaskAsync(*id, worker, _, handler.maxRetries, handler.onFail, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, auto, auto, const auto&, OnExecute cb) {
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->processTask(task, handler, contextHolder, yield);
    });
}

TEST_F(ProcessTaskTest, shouldFailTaskOnPermanentFail) {
    result = make_unexpected(TaskControl::permanentFail);

    EXPECT_CALL(*queue, failTaskAsync(*id, worker, _, Loop::MAX_TRIES_FOR_ERROR, Loop::DELAY_FOR_ERROR, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, auto, auto, const auto&, OnExecute cb) {
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->processTask(task, handler, contextHolder, yield);
    });
}

TEST_F(ProcessTaskTest, shouldDelayTaskOnSpecialValue) {
    result = make_unexpected(TaskControl::delay);

    EXPECT_CALL(*queue, delayTaskAsync(*id, worker, handler.onDelay, _, _))
    .WillOnce(Invoke([] (auto, const auto&, auto, const auto&, OnExecute cb) {
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->processTask(task, handler, contextHolder, yield);
    });
}

}
