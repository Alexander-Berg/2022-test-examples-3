#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/mail/db/register_executor.h>
#include "mocks.h"

namespace apq {

static inline std::ostream& operator<<(std::ostream& o, const query& q) {
    return o << q.text_;
}

} // namespace apq

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::db;
using namespace sharpei::mail;
using namespace sharpei::mail::db;

class TestHandler {
public:
    TestHandler(ExplainedError& res): res_(res) {}
    TestHandler(const TestHandler& other): res_(other.res_) {}

    void operator()(const ExplainedError& error) {
        res_ = error;
    }
private:
    ExplainedError& res_;
};

class SingleRowIterator {
public:
    SingleRowIterator() : size_(0) {}
    SingleRowIterator(const std::string& res) : size_(1), res_(res) {}

    const SingleRowIterator* operator->() const {
        return this;
    }

    int size() const {
        return size_;
    }

    void at(int index, std::string& value) const {
        ASSERT_EQ(size_, 1);
        ASSERT_EQ(index, 0);
        value = res_;
    }

    bool operator ==(const SingleRowIterator& other) const {
        return size_ == other.size_ && res_ == other.res_;
    }

private:
    int size_;
    std::string res_;
};

typedef std::function<void(apq::result, SingleRowIterator)> RequestHandler;
typedef apq::time_traits::duration_type time_duration;

class MockConnectionHolder {
public:
    MOCK_METHOD(void, async_request, (const apq::query&, RequestHandler, apq::result_format, time_duration), ());
};

apq::result apqOkResult() {
    return apq::result(boost::system::error_code());
}

apq::result apqErrorResult() {
    return apq::error::make_error_code(apq::error::unknown);
}

apq::result apqActiveSqlTransactionErrorResult() {
    return apq::result("25001", "");
}

MATCHER(IsBegin, "") { return arg.text_.find("BEGIN") == 0; }
MATCHER(IsDbProc, "") { return arg.text_.find("code.") != std::string::npos; }
MATCHER(IsPrepare, "") { return arg.text_.find("PREPARE") == 0; }
MATCHER(IsCommitPrepared, "") { return arg.text_.find("COMMIT PREPARED") == 0; }
MATCHER(IsRollbackPrepared, "") { return arg.text_.find("ROLLBACK PREPARED") == 0; }
MATCHER(IsRollback, "") { return arg.text_.find("ROLLBACK") == 0 && arg.text_.find("PREPARED") == std::string::npos; }

class RegisterUserExecutorTest: public Test {
protected:
    class MockConnectionHolderWrapper {
    public:
        MockConnectionHolderWrapper(apq::connection_pool& pool): pool_(pool) {}
        void async_request(yplatform::task_context_ptr ctx, const apq::query& q, RequestHandler handler, apq::result_format tf, time_duration t) {
            (void)ctx;
            if (&pool_ == &mdbConnectionPool_->pool()) {
                mdbHolderMock_->async_request(q, handler, tf, t);
            } else {
                sharddbHolderMock_->async_request(q, handler, tf, t);
            }
        }
    private:
        apq::connection_pool& pool_;
    };

    typedef std::shared_ptr<RegisterUserExecutor<MockConnectionHolderWrapper, SingleRowIterator>> ExecutorPtr;

    void SetUp() override {
        mdbConnectionPool_.reset(new ApqConnectionPool(ios_));
        mdbPool_.reset(new MockConnectionPool);
        EXPECT_CALL(*mdbPool_, get(_)).WillRepeatedly(Return(mdbConnectionPool_));

        sharddbConnectionPool_.reset(new ApqConnectionPool(ios_));
        sharddbPool_.reset(new MockConnectionPool);
        EXPECT_CALL(*sharddbPool_, get(_)).WillRepeatedly(Return(sharddbConnectionPool_));

        mdbHolderMock_.reset(new StrictMock<MockConnectionHolder>);
        sharddbHolderMock_.reset(new StrictMock<MockConnectionHolder>);

        handlerResult_ = ExplainedError(Error::unknown);
        executor_ = createExecutor();
    }

    void TearDown() override {
        mdbHolderMock_.reset();
        sharddbHolderMock_.reset();
        mdbConnectionPool_.reset();
        sharddbConnectionPool_.reset();
    }

    ExecutorPtr createExecutor() {
        const auto config = mail::makeTestConfig();
        return createConfiguredExecutor(config);
    }

    ExecutorPtr createConfiguredExecutor(mail::ConfigPtr config) {
        ProfilerPtr profiler(new NiceMock<MockProfiler>);
        Scribe scribe(profiler);

        Shard::Database::Address mdbMaster = {"xdb01m.mail.yandex.net", 6432, "mdb", "sas"};
        RegParams params = {123, "ru", "ru", true, true, true, 1, mdbMaster, "sharpei01d.mail.yandex.net",
            "REQUEST-ID", "CONNECTION-ID", "SESSION-ID"};

        TestHandler handler(handlerResult_);
        return RegisterUserExecutor<MockConnectionHolderWrapper, SingleRowIterator>::create(
            config, scribe, mdbPool_, sharddbPool_, params, handler);
    }

    inline static std::unique_ptr<StrictMock<MockConnectionHolder>> mdbHolderMock_;
    inline static std::unique_ptr<StrictMock<MockConnectionHolder>> sharddbHolderMock_;
    inline static std::shared_ptr<ApqConnectionPool> mdbConnectionPool_;
    inline static std::shared_ptr<ApqConnectionPool> sharddbConnectionPool_;

    boost::asio::io_service ios_;
    std::shared_ptr<MockConnectionPool> mdbPool_;
    std::shared_ptr<MockConnectionPool> sharddbPool_;

    ExplainedError handlerResult_;
    ExecutorPtr executor_;
};

TEST_F(RegisterUserExecutorTest, correctSequence_handlerOk) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    executor_->execute();
    EXPECT_EQ(RegistrationError::ok, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbBeginError_handlerMaildbRegistrationError) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(RegistrationError::maildbRegistrationError, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbRegisterError_handlerMaildbRegistrationError) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(RegistrationError::maildbRegistrationError, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbRegisterAlreadyRegistered_handlerAlreadyRegistered) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("already_registered")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(sharpei::RegistrationError::userAlreadyRegistered, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbRegisterInProgress_handlerInProgress) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("already_in_progress")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(sharpei::RegistrationError::registrationInProgress, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbRegisterShardIsOccupied_handlerShardIsOccupied) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("shard_is_occupied_by_user")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(sharpei::RegistrationError::shardIsOccupiedByUser, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbRegisterUnknownResult_handlerMaildbRegistrationError) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("qazwsxedc")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(RegistrationError::maildbRegistrationError, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbPrepareError_handlerMaildbRegistrationError) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
    EXPECT_EQ(RegistrationError::maildbRegistrationError, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbCommitRetryOk_handlerOk) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    executor_->execute();
    EXPECT_EQ(RegistrationError::ok, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbCommitRetriesMoreThanConfigMaxCommitTries_handlerMaildbRegistrationError) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .Times(3).WillRepeatedly(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsCommitPrepared(), _, _, _))
        .Times(0);

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    const auto config = mail::makeTestConfig();
    config->registration.maxCommitTries = 3;
    executor_ = createConfiguredExecutor(config);

    executor_->execute();
    EXPECT_EQ(RegistrationError::maildbRegistrationError, handlerResult_);
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbBeginError_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbRegisterError_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbRegisterAlreadyRegistered_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("already_registered")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbRegisterAlreadyInProgress_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("already_in_progress")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbRegisterUnknownResult_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("qazwsxedc")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbPrepareError_mdbRollbackAndShadddbRollbackPrepared) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbPrepare_andShadddbRollbackPreparedFailed_mdbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .Times(3).WillRepeatedly(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());


    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbPrepareOkAndSharddbPrepare_andShadddbRollbackPreparedFailedWithSqlStateActiveSqlTransaction_mdbRollbackAndSharddbRollback) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqActiveSqlTransactionErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqActiveSqlTransactionErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(Return());

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbRollbackError_retry) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    executor_->execute();
}

TEST_F(RegisterUserExecutorTest, mdbRollbackErrorMoreThanConfigMaxRollbackTries_retryOnlyMaxTimes) {
    InSequence s;
    EXPECT_CALL(*mdbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsDbProc(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator("success")));
    EXPECT_CALL(*mdbHolderMock_, async_request(IsPrepare(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*sharddbHolderMock_, async_request(IsBegin(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));
    EXPECT_CALL(*sharddbHolderMock_, async_request(IsRollback(), _, _, _))
        .WillOnce(InvokeArgument<1>(apqOkResult(), SingleRowIterator()));

    EXPECT_CALL(*mdbHolderMock_, async_request(IsRollbackPrepared(), _, _, _))
        .Times(3).WillRepeatedly(InvokeArgument<1>(apqErrorResult(), SingleRowIterator()));

    const auto config = mail::makeTestConfig();
    config->registration.maxRollbackTries = 3;
    executor_ = createConfiguredExecutor(config);

    executor_->execute();
}


} // namespace


