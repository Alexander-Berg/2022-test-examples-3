#pragma once

#include <mail/sender.h>
#include <common/types.h>

namespace botserver::mail {

struct fake_sender
    : sender
    , yplatform::module
{
    vector<mail_message_ptr> sent_emails;

    future<void> send(task_context_ptr, mail_message_ptr msg) override
    {
        sent_emails.push_back(msg);
        promise<void> prom;
        prom.set();
        return prom;
    }
};

}
