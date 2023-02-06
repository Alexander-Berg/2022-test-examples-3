#pragma once

#include <ymod_mdb_sharder/types.h>
#include <yplatform/log/contains_logger.h>

namespace ymod_mdb_sharder {

class fake_lock_manager : public yplatform::log::contains_logger
{
public:
    using resource_cb = std::function<void(const std::string&)>;
    using resources_cb = std::function<void(const std::vector<std::string>&)>;

    fake_lock_manager()
    {
        logger().append_log_prefix("fake_lock_manager");
    }

    void init(const resource_cb& on_acquire_resource, const resource_cb& on_release_resource)
    {
        on_acquire_resource_ = on_acquire_resource;
        on_release_resource_ = on_release_resource;
    }

    void get_resource_value(
        const std::string& resource,
        const std::function<void(const std::string& value)>& cb)
    {
        cb(resource_value_[resource]);
    }

    void on_add_resources(const std::vector<std::string>&)
    {
    }

    void on_del_resources(const std::vector<std::string>& resources)
    {
        for (auto& resource : resources)
        {
            resource_value_.erase(resource);
        }
    }

    void set_resource_value(const std::string& resource, const std::string& value)
    {
        YLOG_L(info) << "set resource value resource=" << resource << " value=" << value;
        resource_value_[resource] = value;
    }

    void acquire_resource(const std::string& resource)
    {
        YLOG_L(info) << "acquire resource " << resource;
        acquired_resources_.insert(resource);
        on_acquire_resource_(resource);
    }

    void release_resource(const std::string& resource)
    {
        YLOG_L(info) << "release resource " << resource;
        acquired_resources_.erase(resource);
        on_release_resource_(resource);
    }

    void get_acquired_resources(const resources_cb& cb)
    {
        cb({ acquired_resources_.begin(), acquired_resources_.end() });
    }

private:
    resource_cb on_acquire_resource_;
    resource_cb on_release_resource_;
    std::map<std::string, std::string> resource_value_;
    std::set<std::string> acquired_resources_;
};

}