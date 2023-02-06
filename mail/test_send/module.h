#pragma once

#include "polling_loop.h"
#include "settings.h"
#include <tasks/test_tasks.h>
#include <delivery/sender.h>
#include <common/types.h>
#include <ymod_lease/ymod_lease.h>
#include <yplatform/module.h>

namespace fan::test_send {

struct module : yplatform::module
{
    io_service& io;
    settings settings;
    polling_loop_ptr loop;
    shared_ptr<tasks::test_tasks> tasks;
    shared_ptr<delivery::sender> sender;
    std::shared_ptr<ymod_lease::node> lease_node;
    string my_node_id;

    module(io_service& io) : io(io)
    {
    }

    void init(const yplatform::ptree& conf)
    {
        settings = settings::from_ptree(conf);
        tasks = find_module<tasks::test_tasks>("tasks");
        sender = find_module<delivery::sender>("delivery");
        if (settings.use_lease)
        {
            auto self = shared_from(this);
            lease_node = find_module<ymod_lease::node>("lease_node");
            lease_node->bind(
                settings.test_send_resource,
                io.wrap(weak_bind(&module::on_busy, self, ph::_1, ph::_2, ph::_3, ph::_4)),
                io.wrap(weak_bind(&module::on_free, self, ph::_1)));
            my_node_id = lease_node->node_id();
        }
    }

    void start()
    {
        if (lease_node)
        {
            lease_node->start_acquire_lease(settings.test_send_resource);
        }
        else
        {
            start_loop();
        }
    }

    void start_loop()
    {
        auto ctx = boost::make_shared<task_context>();
        LINFO_(ctx) << "start polling loop for test send";
        loop = std::make_shared<polling_loop>(io, ctx, tasks, sender, settings.polling);
        yplatform::spawn(io, loop);
    }

    void stop_loop()
    {
        LINFO_(loop->task_ctx) << "stop polling loop";
        loop->stop();
        loop.reset();
    }

    void on_busy(
        const string& resource,
        const string& node_id,
        ymod_lease::ballot_t /*ballot*/,
        const string& /*value*/)
    {
        YLOG_L(info) << "resource \"" << resource << "\" is busy; owner_id=\"" << node_id << "\"";
        if (!loop && node_id == my_node_id)
        {
            start_loop();
        }
        else if (loop && node_id != my_node_id)
        {
            stop_loop();
        }
    }

    void on_free(const string& resource)
    {
        YLOG_L(info) << "resource \"" << resource << "\" is free";
        if (loop)
        {
            stop_loop();
        }
    }
};
}
