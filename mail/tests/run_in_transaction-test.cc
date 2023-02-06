#include "mocks/transactional_mock.h"
#include <mail/macs_pg/include/internal/service/run_in_transaction.h>

namespace {

using namespace testing;
using namespace tests;
using namespace macs::pg;

template <typename Arg>
struct CallableMock {
    struct Impl {
        MOCK_METHOD(void, call, (Arg), ());
    };

    void operator() (Arg arg) const {
        impl->call(std::move(arg));
    }

    std::shared_ptr<Impl> impl = std::make_shared<Impl>();
};

using OnExecuteMock = CallableMock<error_code>;

struct CallbackMock {
    struct Impl {
        MOCK_METHOD(void, call, (macs::ServicePtr, std::function<void(error_code)>), ());
    };

    void operator() (macs::ServicePtr s, std::function<void(error_code)> f) {
        impl->call(s, f);
    }

    std::shared_ptr<Impl> impl = std::make_shared<Impl>();
};


struct ServiceCreatorMock {
    MOCK_METHOD(macs::ServicePtr, call, (pgg::ConnectionPtr), ());

    auto get() {
        return [this](auto v) {
            return this->call(std::move(v));
        };
    }
};

struct RunInTransactionTest : public Test {
    OnExecuteMock hook;
    const Milliseconds timeout {13};
    Coroutine coroutine;
    boost::shared_ptr<StrictMock<TransactionalMock>> transactional = boost::make_shared<StrictMock<TransactionalMock>>();
    CallbackMock callback;

    std::shared_ptr<StrictMock<ServiceCreatorMock>> serviceCreator = std::make_shared<StrictMock<ServiceCreatorMock>>();
    RunInTransaction operation{ timeout, callback, hook, serviceCreator->get() };
};

TEST_F(RunInTransactionTest, when_error_on_begin_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(*transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted)));

    operation(coroutine, *transactional);
}

TEST_F(RunInTransactionTest, when_error_on_commit_should_call_hook_with_same_error) {
    const InSequence s;

    EXPECT_CALL(*transactional, beginImpl(_, timeout)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    EXPECT_CALL(*serviceCreator, call(_)).WillOnce(Return(nullptr));
    operation(coroutine, *transactional);

    EXPECT_CALL(*callback.impl, call(_, _)).WillOnce(InvokeArgument<1>(pgg_error_code{}));
    EXPECT_CALL(*transactional, commitImpl(_)).WillOnce(InvokeArgument<0>(operationAborted));
    EXPECT_CALL(*hook.impl, call(operationAborted));
    operation(coroutine, *transactional);
}

}