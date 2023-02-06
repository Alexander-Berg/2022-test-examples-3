#pragma once

#include "mocks.h"
#include "yxiva/core/authorizer.h"

#define TEST_UID "TEST"

using namespace yxiva;
using namespace yxiva::processor;

class mock_authorizer : public authorizer
{
public:
    mock_authorizer()
    {
        EXPECT_CALL(*this, authenticate(An<task_context_ptr>(), An<const service_user_id&>()))
            .Times(0);

        set_good(); /// default behaviour
    }

    MOCK_METHOD(
        user_info_future_t,
        authenticate,
        (task_context_ptr ctx, const user_id& uid),
        (override));
    MOCK_METHOD(
        user_info_future_t,
        authenticate,
        (task_context_ptr ctx, const service_user_id& suid),
        (override));

    void set_good()
    {
        ON_CALL(*this, authenticate(An<task_context_ptr>(), An<const user_id&>()))
            .WillByDefault(Invoke(this, &mock_authorizer::authenticate_good));
    }

    void set_bad()
    {
        ON_CALL(*this, authenticate(An<task_context_ptr>(), An<const user_id&>()))
            .WillByDefault(Invoke(this, &mock_authorizer::authenticate_bad));
    }

    void set_manual()
    {
        ON_CALL(*this, authenticate(An<task_context_ptr>(), An<const user_id&>()))
            .WillByDefault(Invoke(this, &mock_authorizer::return_manual));
    }

    user_info_future_t authenticate_good(task_context_ptr ctx, const user_id& uid)
    {
        user_info_promise_t result;
        user_info response;
        response.uid = uid;
        result.set(response);
        return result;
    }

    user_info_future_t authenticate_bad(task_context_ptr ctx, const user_id& uid)
    {
        user_info_promise_t result;
        result.set_exception(std::domain_error("bad_test_authorizer: authenticate failed"));
        return result;
    }

    user_info_future_t return_manual(task_context_ptr ctx, const user_id& uid)
    {
        promise_ = user_info_promise_t();
        response_.uid = uid;
        return promise_;
    }

    void manual_set_good()
    {
        promise_.set(response_);
    }

    user_info_promise_t promise_;
    user_info response_;
};
