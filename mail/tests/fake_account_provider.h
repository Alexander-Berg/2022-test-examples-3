#pragma once

#include <auth/account_provider.h>
#include <yplatform/module.h>

namespace botserver::auth {

struct fake_account_provider
    : account_provider
    , yplatform::module
{
    map<string, mail_account> accounts;

    future<optional<mail_account>> get_mail_account(task_context_ptr, botpeer, string email)
        override
    {
        ;
        promise<optional<mail_account>> promise;
        if (auto it = accounts.find(email); it != accounts.end())
        {
            promise.set(it->second);
        }
        else
        {
            promise.set({});
        }
        return promise;
    }
};

}
