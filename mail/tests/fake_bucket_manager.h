#pragma once

#include <ymod_mdb_sharder/types.h>
#include <yplatform/log/contains_logger.h>

namespace ymod_mdb_sharder {

class fake_bucket_manager : public yplatform::log::contains_logger
{
public:
    using resource_cb = std::function<void(const std::string&)>;
    using buckets_resources_map = std::map<std::string, std::set<std::string>>;

    fake_bucket_manager()
    {
        logger().append_log_prefix("fake_bucket_manager");
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

    void set_resource_value(const std::string& resource, const std::string& value)
    {
        YLOG_L(info) << "set resource value resource=" << resource << " value=" << value;
        resource_value_[resource] = value;
    }

    void acquire_bucket(const bucket_id& bucket)
    {
        YLOG_L(info) << "acquire bucket " << bucket;
        acquired_buckets.insert(bucket);
        for (auto& resource : buckets[bucket])
        {
            YLOG_L(info) << "acquire resource " << resource;
            on_acquire_resource_(resource);
        }
    }

    void release_bucket(const bucket_id& bucket)
    {
        YLOG_L(info) << "release resource " << bucket;
        acquired_buckets.erase(bucket);
        for (auto& resource : buckets[bucket])
        {
            YLOG_L(info) << "release resource " << resource;
            on_release_resource_(resource);
        }
    }

    void get_acquired_buckets(const std::function<void(const std::vector<std::string>&)>& cb)
    {
        cb(std::vector<std::string>(acquired_buckets.begin(), acquired_buckets.end()));
    }

    void add_buckets(const buckets_resources_map& new_buckets)
    {
        for (auto& [bucket, resources] : new_buckets)
        {
            add_resources_to_bucket(bucket, resources);
        }
    }

    void del_buckets(const std::vector<std::string>& del_buckets)
    {
        for (auto& bucket : del_buckets)
        {
            del_resources_from_bucket(bucket, buckets[bucket]);
            acquired_buckets.erase(bucket);
            buckets.erase(bucket);
        }
    }

    void add_resources_to_bucket(const bucket_id& bucket, const std::set<std::string>& resources)
    {
        buckets[bucket].insert(resources.begin(), resources.end());
        if (acquired_buckets.count(bucket))
        {
            for (auto& resource : resources)
            {
                on_acquire_resource_(resource);
            }
        }
    }

    void del_resources_from_bucket(const bucket_id& bucket, const std::set<std::string>& resources)
    {
        if (acquired_buckets.count(bucket))
        {
            for (auto& resource : resources)
            {
                on_release_resource_(resource);
            }
        }
        for (auto& resource : resources)
        {
            buckets[bucket].erase(resource);
        }
    }

    void release_buckets_for(const std::vector<std::string>&, const time_traits::duration&)
    {
    }

private:
    resource_cb on_acquire_resource_;
    resource_cb on_release_resource_;
    std::map<std::string, std::string> resource_value_;
    buckets_resources_map buckets;
    std::set<std::string> acquired_buckets;
};

}