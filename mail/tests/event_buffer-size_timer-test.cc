#include <mail/alabay/ymod_logbroker/tests/include/mock-event_buffer.h>

namespace {

using namespace ymod_logbroker;
using namespace ::testing;
using namespace std::chrono_literals;
using namespace std::chrono;

struct WithSizeAndTimer : public Test {
    std::shared_ptr<StrictMock<MockEventBuffer>> realBuffer;
    std::shared_ptr<StrictMock<MockWithTimer>> withTimer;
    EventBufferPtr eventBuffer;

    const std::size_t chunkSize = 10;
    const milliseconds flushInterval = 10ms;
    const milliseconds timerInitTime = 0ms;
    const milliseconds initFlushTime = timerInitTime + flushInterval;

    void SetUp() override {
        realBuffer = std::make_shared<StrictMock<MockEventBuffer>>();
        withTimer = std::make_shared<StrictMock<MockWithTimer>>(realBuffer, flushInterval, getTimePointMs(initFlushTime));
        eventBuffer = std::make_shared<WithSize>(withTimer, chunkSize);
    }

    inline time_point<steady_clock, milliseconds> getTimePointMs(milliseconds time) const {
        return time_point<steady_clock, milliseconds>(time);
    }
    void checkAddEvents(const mail_errors::error_code& expectedError, bool excpectedFlush) {
        eventBuffer->addEvents({}, {}, {}, [=](mail_errors::error_code& ec, bool flushed) {
            EXPECT_EQ(flushed, excpectedFlush);
            EXPECT_EQ(ec, expectedError);
        });
    }
    void checkProbablyFlush(const mail_errors::error_code& expectedError, bool excpectedFlush) {
        eventBuffer->probablyFlush({}, [=](mail_errors::error_code& ec, bool flushed) {
            EXPECT_EQ(flushed, excpectedFlush);
            EXPECT_EQ(ec, expectedError);
        });
    }
};

TEST_F(WithSizeAndTimer, shouldFlushBySizeInAddEventsAndResetTimer) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (OnProbablyFlush cb) {
                    cb(mail_errors::error_code(), false);
        })));
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize));

        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkAddEvents(mail_errors::error_code(), true);
}

TEST_F(WithSizeAndTimer, shouldFlushByTimerInAddEvents) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (OnProbablyFlush cb) {
                    cb(mail_errors::error_code(), false);
        })));
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkAddEvents(mail_errors::error_code(), true);
}

TEST_F(WithSizeAndTimer, shouldNotFlushBySizeAndTimerInAddEvents) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (OnProbablyFlush cb) {
                    cb(mail_errors::error_code(), false);
        })));
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));

        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime-1ms)));
    }

    checkAddEvents(mail_errors::error_code(), false);
}



TEST_F(WithSizeAndTimer, shouldFlushBySizeInProbablyFlushAndResetTimer) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkProbablyFlush(mail_errors::error_code(), true);
}

TEST_F(WithSizeAndTimer, shouldFlushByTimerInProbablyFlush) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));
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

TEST_F(WithSizeAndTimer, shouldNotFlushBySizeAndTimerInProbablyFlush) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime-1ms)));
    }

    checkProbablyFlush(mail_errors::error_code(), false);
}


TEST_F(WithSizeAndTimer, shouldFlushBySizeAndFlushByTime) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (OnProbablyFlush cb) {
                    cb(mail_errors::error_code(), false);
        })));
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));

        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime + flushInterval)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkAddEvents(mail_errors::error_code(), true);
    checkProbablyFlush(mail_errors::error_code(), true);
}

TEST_F(WithSizeAndTimer, shouldNotFlushBySizeAndNotFlushByTime) {

}

TEST_F(WithSizeAndTimer, shouldNotFlushInAddEventsButFlushByTimerInProbablyFlush) {
    {
        InSequence s;
        EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (OnProbablyFlush cb) {
                    cb(mail_errors::error_code(), false);
        })));
        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime-1ms)));

        EXPECT_CALL(*realBuffer, size).WillOnce(Return(chunkSize-1));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(initFlushTime)));
        EXPECT_CALL(*realBuffer, asyncFlush(_, _))
            .WillOnce(WithArg<1>(
                Invoke([&](OnFlush cb) {
                    cb(mail_errors::error_code());
        })));
        EXPECT_CALL(*withTimer, now).WillOnce(Return(getTimePointMs(123ms)));
    }

    checkAddEvents(mail_errors::error_code(), false);
    checkProbablyFlush(mail_errors::error_code(), true);
}

}
