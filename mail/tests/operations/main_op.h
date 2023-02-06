#pragma once

#include <common/errors.h>

#include <yplatform/yield.h>

namespace mock {

struct main_op
{
    using yield_ctx = yplatform::yield_context<main_op>;

    template <typename Env>
    void operator()(yield_ctx /*ctx*/, Env&& env, error ec = {})
    {
        if (env.cache_mailbox->account().auth_data.empty())
        {
            ec = code::no_auth_data;
        }

        ++finished_iterations;

        env(ec);
    }

    inline static size_t finished_iterations = 0;
};

} // namespace mock

#include <yplatform/unyield.h>
