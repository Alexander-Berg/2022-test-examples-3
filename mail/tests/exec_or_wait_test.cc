#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/ymod_queuedb_worker/include/exec_or_wait.h>
#include <yplatform/application/task_context.h>


using namespace ::testing;

namespace ymod_queuedb::tests {

struct ExecOrWaitTest: public Test {
    static constexpr unsigned TRIES = 3;
    static constexpr std::chrono::seconds TIMEOUT{1};

    std::shared_ptr<yplatform::reactor> reactor;
    std::shared_ptr<ExecOrWaitFactory> factory;

    yplatform::task_context_ptr ctx;

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);
        factory = std::make_shared<ExecOrWaitFactory>(TRIES, TIMEOUT, reactor);
        ctx = boost::make_shared<yplatform::task_context>();

    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

TEST_F(ExecOrWaitTest, shouldNotRunWithCancelledTask) {
    const auto fn = [] () {
        EXPECT_FALSE(true);
        return false;
    };

    ctx->cancel();
    spawn([=, this] (boost::asio::yield_context yield) {
        factory->product(yield, ctx)(fn);
    });
}

TEST_F(ExecOrWaitTest, shouldRunExacltyOnce) {
    unsigned count = 0;
    const auto fn = [&] () {
        count++;
        return true;
    };

    spawn([=, this] (boost::asio::yield_context yield) {
        factory->product(yield, ctx)(fn);
    });

    EXPECT_EQ(count, 1u);
}

TEST_F(ExecOrWaitTest, shouldRunExactlyNTimes) {
    unsigned count = 0;
    const auto fn = [&] () {
        count++;
        return false;
    };

    spawn([=, this] (boost::asio::yield_context yield) {
        factory->product(yield, ctx)(fn);
    });

    EXPECT_EQ(count, TRIES);
}

TEST_F(ExecOrWaitTest, shouldStopOnFalseCallbackResponse) {
    std::vector<bool> responses = {false, true};
    unsigned count = 0;
    const auto fn = [&] () {
        return responses[count++];
    };

    spawn([=, this] (boost::asio::yield_context yield) {
        factory->product(yield, ctx)(fn);
    });

    EXPECT_EQ(count, responses.size());
}

TEST_F(ExecOrWaitTest, shouldStopExecutingAfterTaskIsCancelled) {
    unsigned count = 0;
    const auto fn = [&] () {
        count++;
        ctx->cancel();
        return false;
    };

    spawn([=, this] (boost::asio::yield_context yield) {
        factory->product(yield, ctx)(fn);
    });

    EXPECT_EQ(count, 1u);
}

}
