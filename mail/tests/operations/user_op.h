#pragma once

#include <common/errors.h>

namespace mock {

struct user_op
{
    template <typename Env>
    void operator()(Env&& env, error ec = {})
    {
        env(ec);
    }
};

} // namespace mock
