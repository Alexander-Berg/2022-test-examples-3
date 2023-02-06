#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/ymod_queuedb_worker/include/task_control.h>


namespace ymod_queuedb {

TEST(DelayOnCancelledTaskTest, shouldThrowOnCancelledTask) {
    yplatform::task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    ctx->cancel();

    EXPECT_THROW(delayOnCancelledTask(ctx), ymod_queuedb::TaskControlDelayException);
}

TEST(DelayOnCancelledTaskTest, shouldNotThrowOnRunningTask) {
    yplatform::task_context_ptr ctx = boost::make_shared<yplatform::task_context>();

    EXPECT_NO_THROW(delayOnCancelledTask(ctx));
}

}
