#include <mail/ymod_queuedb_worker/tests/run_loop/test_classes.h>


namespace ymod_queuedb {

struct CreateRefreshTaskCoroTest: public RunLoopBaseTest { };

TEST_F(CreateRefreshTaskCoroTest, shouldNotRunOnCancelledTask) {
    EXPECT_CALL(*queue, refreshTaskAsync(*id, worker, _, _)).Times(0);
    contextHolder.cancel();
    spawn([this] (boost::asio::yield_context yield) {
        loop->createRefreshTaskCoro(std::weak_ptr<TaskId>(), contextHolder, refreshTimeout, requestId)(yield);
    });
}

TEST_F(CreateRefreshTaskCoroTest, shouldStopRefreshingOnCancelledTask) {
    EXPECT_CALL(*queue, refreshTaskAsync(*id, worker, _, _))
    .WillOnce(Invoke([this] (auto, const auto&, const auto&, OnExecute cb) {
        contextHolder.cancel();
        cb();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->createRefreshTaskCoro(id, contextHolder, refreshTimeout, requestId)(yield);
    });

    EXPECT_TRUE(contextHolder.cancelled());
}

TEST_F(CreateRefreshTaskCoroTest, shouldCancelTaskOnEmptyWeakTaskId) {
    spawn([this] (boost::asio::yield_context yield) {
        loop->createRefreshTaskCoro(std::weak_ptr<TaskId>(), contextHolder, refreshTimeout, requestId)(yield);
    });

    EXPECT_TRUE(contextHolder.cancelled());
}

TEST_F(CreateRefreshTaskCoroTest, shouldCancelTaskOnException) {
    EXPECT_CALL(*queue, refreshTaskAsync(*id, worker, _, _))
    .WillOnce(Invoke([] (auto, const auto&, const auto&, OnExecute ) {
        throw std::exception();
    }));

    spawn([this] (boost::asio::yield_context yield) {
        loop->createRefreshTaskCoro(id, contextHolder, refreshTimeout, requestId)(yield);
    });

    EXPECT_TRUE(contextHolder.cancelled());
}

}
