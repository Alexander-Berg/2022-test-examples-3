#pragma once

#include <common/campaign.h>
#include <common/recipient_data.h>
#include <common/mail_message.h>
#include <common/types.h>

namespace fan {

template <typename Tasks, typename Delivery>
struct fake_send_campaign_op
{
    task_context_ptr ctx;
    error_code err;
    campaign campaign;
    shared_ptr<Tasks> tasks;
    shared_ptr<Delivery> delivery;
    without_data_cb cb;

    fake_send_campaign_op(
        task_context_ptr ctx,
        const struct campaign& campaign,
        shared_ptr<Tasks> tasks,
        shared_ptr<Delivery> delivery,
        const without_data_cb& cb)
        : ctx(ctx), campaign(campaign), tasks(tasks), delivery(delivery), cb(cb)
    {
    }

    template <typename YieldContex>
    void operator()(YieldContex)
    {
        tasks->get_campaign_recipients(ctx, campaign, [this](auto err, auto recipients) {
            if (err) return cb(err);
            for (auto recipient : recipients)
            {
                delivery->send(
                    ctx, mail_message{ .recipient = recipient.email }, [](auto /*err*/) {});
            }
            cb({});
        });
    }
};
}
