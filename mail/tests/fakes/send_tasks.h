#pragma once

#include <common/campaign.h>
#include <common/recipient_data.h>
#include <common/send_stats.h>
#include <common/types.h>

namespace fan {

struct fake_send_tasks
{
    using campaigns_cb = function<void(error_code, const vector<campaign>&)>;
    using params_cb = function<void(error_code, const vector<map<string, string>>&)>;
    using recipients_cb = function<void(error_code, const vector<recipient_data>&)>;
    using strings_cb = function<void(error_code, const vector<string>&)>;

    error_code get_campaigns_err;
    error_code get_recipients_err;
    error_code get_eml_template_err;
    error_code mark_sent_err;
    error_code mark_failed_err;
    vector<campaign> campaigns;
    vector<recipient_data> recipients;
    vector<string> unsubscribe_list;
    map<string, map<string, string>> template_params;
    string eml_template;
    string state;

    void get_pending_campaigns(task_context_ptr, size_t, const campaigns_cb& cb)
    {
        cb(get_campaigns_err, campaigns);
    }

    void get_campaign_recipients(task_context_ptr, const campaign&, const recipients_cb& cb)
    {
        cb(get_recipients_err, recipients);
    }

    void get_campaign_unsubscribe_list(task_context_ptr, const campaign&, const strings_cb& cb)
    {
        cb({}, unsubscribe_list);
    }

    void get_campaign_template_params(
        task_context_ptr,
        const campaign&,
        const vector<recipient_data>& recipients,
        const recipients_cb& cb)
    {
        auto ret = recipients;
        for (auto& recipient : ret)
        {
            if (!template_params.count(recipient.email)) continue;
            recipient.template_params = template_params[recipient.email];
        }
        cb({}, ret);
    }

    void get_campaign_eml_template(task_context_ptr, const campaign&, const string_ptr_cb& cb)
    {
        cb(get_eml_template_err, make_shared<string>(eml_template));
    }

    void mark_campaign_sent(
        task_context_ptr,
        const campaign&,
        const send_stats&,
        const without_data_cb& cb)
    {
        if (!mark_sent_err) state = "sent";
        cb(mark_sent_err);
    }

    void mark_campaign_failed(
        task_context_ptr,
        const campaign&,
        const string&,
        const without_data_cb& cb)
    {
        if (!mark_failed_err) state = "failed";
        cb(mark_failed_err);
    }
};

}
