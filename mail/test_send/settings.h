#pragma once

#include <common/types.h>

namespace fan::test_send {

struct polling_settings
{
    duration interval;
    size_t max_retries;
    double backoff_multiplier;

    static polling_settings from_ptree(const ptree& conf)
    {
        polling_settings ret;
        ret.interval = conf.get<duration>("interval");
        ret.max_retries = conf.get<size_t>("max_retries");
        ret.backoff_multiplier = conf.get<double>("backoff_multiplier");
        return ret;
    }
};

struct settings
{
    polling_settings polling;
    bool use_lease;
    string test_send_resource;

    static settings from_ptree(const ptree& conf)
    {
        settings ret;
        ret.polling = polling_settings::from_ptree(conf.get_child("polling"));
        ret.use_lease = conf.get<bool>("use_lease");
        ret.test_send_resource = conf.get<string>("test_send_resource");
        return ret;
    }
};

}
