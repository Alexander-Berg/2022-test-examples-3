#include <mail/alabay/ymod_logbroker/tests/include/mock-event_buffer.h>
#include <mail/alabay/service/include/error.h>

namespace {

using namespace ymod_logbroker;
using namespace ::testing;
using namespace std::chrono_literals;
using namespace std::chrono;

struct TestEventBufferWithTimer : public Test {

    std::shared_ptr<StrictMock<MockEventBuffer>> realBuffer;
    std::shared_ptr<StrictMock<MockWithTimer>> withTimer;

    const mail_errors::error_code testError = mail_errors::error_code(alabay::ConsumerError::internalError);
    const milliseconds flushInterval = 10ms;
    const milliseconds timerInitTime = 0ms;
    const milliseconds initFlushTime = timerInitTime + flushInterval;

    void SetUp() override {
        realBuffer = std::make_shared<StrictMock<MockEventBuffer>>();
        withTimer = std::make_shared<StrictMock<MockWithTimer>>(realBuffer, flushInterval, getTimePointMs(initFlushTime));
    }
    inline void checkProbablyFlush(const mail_errors::error_code& expectedError, bool expectedFlush) {
        withTimer->probablyFlush(nullptr, [=](mail_errors::error_code& ec, bool flushed) {
            EXPECT_EQ(flushed, expectedFlush);
            EXPECT_EQ(ec, expectedError);
        });
    }
    inline time_point<steady_clock, milliseconds> getTimePointMs(milliseconds time) const {
        return time_point<steady_clock, milliseconds>(time);
    }
};

TEST_F(TestEventBufferWithTimer, shouldFlushBufferWhenTimeIsOut) {

    {
        InSequence s;

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkProbablyFlush(mail_errors::error_code(), true);
}

TEST_F(TestEventBufferWithTimer, shouldNotFlushBufferWhenTimeIsNotCome) {

    EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime - 1ms)));
    checkProbablyFlush(mail_errors::error_code(), false);
}

TEST_F(TestEventBufferWithTimer, shouldNotResetTimerOnFlushErrorAndReturnFalse) {

    {
        InSequence s;

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(testError);
        })));
    }

    checkProbablyFlush(testError, false);
}

TEST_F(TestEventBufferWithTimer, shouldWorkAfterFlushError) {

    {
        InSequence s;

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(testError);
        })));

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkProbablyFlush(testError, false);
    checkProbablyFlush(mail_errors::error_code(), true);
}


TEST_F(TestEventBufferWithTimer, shouldFlushBufferByTimerAfterForceFlush) {

    {
        InSequence s;

        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime + flushInterval)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    withTimer->flush(nullptr, [](auto) {});
    checkProbablyFlush(mail_errors::error_code(), true);
}

}
