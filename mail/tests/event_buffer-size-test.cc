#include <mail/alabay/ymod_logbroker/tests/include/mock-event_buffer.h>
#include <mail/alabay/service/include/error.h>

namespace {

using namespace ymod_logbroker;
using namespace ::testing;

struct TestEventBufferWithSize : public Test {
    std::shared_ptr<StrictMock<MockEventBuffer>> realBuffer;
    EventBufferPtr withSize;
    const size_t sz = 10;
    const mail_errors::error_code testError = mail_errors::error_code(alabay::ConsumerError::internalError);

    void SetUp() override {
        realBuffer = std::make_shared<StrictMock<MockEventBuffer>>();
        withSize = std::make_shared<WithSize>(realBuffer, sz);
    }
    void checkAddEvents(const mail_errors::error_code& expectedError, bool expectFlushed) {
        withSize->addEvents("", {}, nullptr, [=](mail_errors::error_code& ec, bool flushed) {
            EXPECT_EQ(flushed, expectFlushed);
            EXPECT_EQ(ec, expectedError);
        });
    }
};

TEST_F(TestEventBufferWithSize, shouldFlushBufferInCaseBufferIsFilled) {

    InSequence s;
    EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
        .WillOnce(WithArg<3>(
            Invoke([=] (OnProbablyFlush cb) {
                cb(mail_errors::error_code(), false);
    })));
    EXPECT_CALL(*realBuffer, size()).WillOnce(Return(sz));
    EXPECT_CALL(*realBuffer, asyncFlush(_, _))
        .WillOnce(WithArg<1>(
            Invoke([=](OnFlush cb) {
                cb(mail_errors::error_code());
    })));

    checkAddEvents(mail_errors::error_code(), true);
}

TEST_F(TestEventBufferWithSize, shouldNotFlushBufferInCaseBufferIsNotFilled) {

    InSequence s;
    EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
        .WillOnce(WithArg<3>(
            Invoke([=] (OnProbablyFlush cb) {
                cb(mail_errors::error_code(), false);
    })));
    EXPECT_CALL(*realBuffer, size()).WillOnce(Return(sz-1));

    checkAddEvents(mail_errors::error_code(), false);
}

TEST_F(TestEventBufferWithSize, shouldReturnFalseAndProxyErrorCodeOnFlushError) {

    InSequence s;
    EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
        .WillOnce(WithArg<3>(
            Invoke([=] (OnProbablyFlush cb) {
                cb(mail_errors::error_code(), false);
    })));
    EXPECT_CALL(*realBuffer, size()).WillOnce(Return(sz));
    EXPECT_CALL(*realBuffer, asyncFlush(_, _))
        .WillOnce(WithArg<1>(
            Invoke([=](OnFlush cb) {
                cb(testError);
    })));

    checkAddEvents(testError, false);
}

TEST_F(TestEventBufferWithSize, shouldNotFlushOnAddEventError) {

    EXPECT_CALL(*realBuffer, asyncAddEvents(_, _, _, _))
        .WillOnce(WithArg<3>(
            Invoke([=] (OnProbablyFlush cb) {
                cb(testError, false);
    })));

    checkAddEvents(testError, false);
}

}
