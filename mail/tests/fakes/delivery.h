#pragma once

#include <common/mail_message.h>
#include <common/types.h>

namespace fan {

struct fake_delivery
{
    map<string, error_code> recipient_errors;
    std::multiset<string> sent_recipients;
    vector<string> sent_emls;

    void send(task_context_ptr, mail_message message, without_data_cb cb)
    {
        sent_recipients.insert(message.recipient);
        sent_emls.emplace_back(message.eml ? *message.eml : string());
        error_code err;
        if (recipient_errors.count(message.recipient))
        {
            err = recipient_errors[message.recipient];
        }
        cb(err);
    }
};

}
