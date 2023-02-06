#pragma once

#include <messenger/gate.h>
#include <common/types.h>

namespace botserver::messenger {

struct fake_gate
    : messenger::gate
    , yplatform::module
{
    vector<pair<botpeer, string>> sent_messages;
    vector<pair<botpeer, string>> downloaded_files;
    string file_to_download;

    void set_message_handler(message_handler /*handler*/) override
    {
    }

    future<void> send_message(task_context_ptr /*ctx*/, botpeer peer, string text) override
    {
        sent_messages.emplace_back(peer, text);
        promise<void> prom;
        prom.set();
        return prom;
    }

    future<void> send_message(task_context_ptr ctx, botpeer peer, markdown_string text) override
    {
        return send_message(ctx, peer, (string)text);
    }

    future<void> send_message(task_context_ptr ctx, botpeer peer, html_string text) override
    {
        return send_message(ctx, peer, (string)text);
    }

    future<string_ptr> download_file(task_context_ptr /*ctx*/, botpeer peer, string file_id)
        override
    {
        downloaded_files.emplace_back(peer, file_id);
        promise<string_ptr> prom;
        prom.set(make_shared<string>(file_to_download));
        return prom;
    }

    void disable_processing(task_context_ptr /*ctx*/, duration /*ttl*/) override
    {
        throw runtime_error("not implemented");
    }
};

}
