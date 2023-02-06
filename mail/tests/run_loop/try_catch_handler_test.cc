#include <mail/ymod_queuedb_worker/tests/run_loop/test_classes.h>


namespace ymod_queuedb {

struct TryCatchHandlerTest: public BaseTest {
    yamail::expected<void> invokeInTryCatchHandler(boost::asio::yield_context yield, std::function<yamail::expected<void>()> fn) {
        return ymod_queuedb::trycatch([fn] (const auto&, bool, auto, auto) { return fn(); })
            (Task(), false, nullptr, yield);
    }

    yamail::expected<void> invokeInTryCatchWithLogHandler(boost::asio::yield_context yield, std::function<yamail::expected<void>()> fn) {
        return ymod_queuedb::trycatchWithLog(getLogger(""), [fn] (const auto&, bool, auto, auto) { return fn(); })
            (Task(), false, nullptr, yield);
    }
};

TEST_F(TryCatchHandlerTest, shouldReturnValue) {
    spawn([this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchHandler(yield, [] () { return yamail::expected<void>(); });
        EXPECT_TRUE(static_cast<bool>(result));
    });

    spawn([this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchWithLogHandler(yield, [] () { return yamail::expected<void>(); });
        EXPECT_TRUE(static_cast<bool>(result));
    });
}

TEST_F(TryCatchHandlerTest, shouldReturnError) {
    const auto fn = [] () { return yamail::make_unexpected(make_error(WorkerError::unknownTaskType, "")); };
    const auto ec = make_error(WorkerError::unknownTaskType, "");

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchWithLogHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });
}

TEST_F(TryCatchHandlerTest, shouldPassErrorInCaseOfSystemException) {
    const auto fn = [] () -> yamail::expected<void> { throw mail_errors::system_error(make_error(WorkerError::unknownTaskType, "")); };
    const auto ec = make_error(WorkerError::unknownTaskType, "");

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchWithLogHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });
}

TEST_F(TryCatchHandlerTest, shouldSetSpecialErrorInCaseOfTaskControlDelayException) {
    const auto fn = [] () -> yamail::expected<void> { throw TaskControlDelayException(); };
    const auto ec = make_error(TaskControl::delay);

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchWithLogHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });
}

TEST_F(TryCatchHandlerTest, shouldSetSpecialErrorInCaseOfException) {
    const auto fn = [] () -> yamail::expected<void> { throw std::exception(); };
    const auto ec = make_error(WorkerError::unexpectedException, "");

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });

    spawn([=, this] (boost::asio::yield_context yield) {
        const auto result = invokeInTryCatchWithLogHandler(yield, fn);
        EXPECT_EQ(result.error(), ec);
    });
}

}
