#pragma once

#include <ymod_lease/types.h>

#include <boost/asio.hpp>

struct fake_lease_node
{
    using busy_cb = ylease::busy_callback;
    using free_cb = ylease::free_callback;
    using peers_count_cb = ylease::peers_count_callback;

    enum resource_state
    {
        read_only,
        acquiring,
        stopped
    };

    struct resource_data
    {
        std::string owner;
        std::string value;
        busy_cb on_busy;
        free_cb on_free;
        resource_state state = stopped;
        bool notified = false;
    };

    fake_lease_node(boost::asio::io_service* io) : io_(io)
    {
    }

    const std::string& node_id()
    {
        return my_node_id_;
    }

    void bind(const std::string& name, const busy_cb& on_busy, const free_cb& on_free)
    {
        resources_[name].on_busy = on_busy;
        resources_[name].on_free = on_free;
    }

    void start_acquire_lease(const std::string& name)
    {
        io_->post([this, name] {
            resources_[name].state = acquiring;
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void start_read_only(const std::string& name)
    {
        io_->post([this, name] {
            resources_[name].state = read_only;
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void update_acquire_value(const std::string& name, const std::string& value)
    {
        io_->post([this, name, value] {
            auto& resource = resources_[name];
            if (resource.owner == my_node_id_ && resource.value != value)
            {
                resource.value = value;
                resource.notified = false;
            }
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void stop_acquire_lease(const std::string& name)
    {
        io_->post([this, name] {
            auto& resource = resources_[name];
            resource.state = stopped;
            resource.notified = false;
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void subscribe_peers_count(const peers_count_cb& cb)
    {
        peers_count_cb_ = cb;
    }

    void free_resource(const std::string& name)
    {
        io_->post([this, name] {
            auto& resource = resources_[name];
            if (resource.owner.size())
            {
                resource.owner = "";
                resource.notified = false;
            }
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void acquire_resource(const std::string& name, const std::string& new_owner)
    {
        io_->post([this, name, new_owner] {
            auto& resource = resources_[name];
            if (resource.owner != new_owner)
            {
                resource.owner = new_owner;
                resource.notified = false;
            }
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    void set_resource_value(const std::string& name, const std::string& value)
    {
        io_->post([this, name, value] {
            auto& resource = resources_[name];
            if (resource.value != value)
            {
                resource.value = value;
                resource.notified = false;
            }
            io_->post(std::bind(&fake_lease_node::update, this));
        });
    }

    std::size_t acquiring_count()
    {
        std::size_t count = 0;
        for (auto& [name, resource] : resources_)
        {
            if (resource.state == acquiring)
            {
                ++count;
            }
        }
        return count;
    }

    void update()
    {
        if (update_enable_)
        {
            for (auto& [name, resource] : resources_)
            {
                if ((resource.state == read_only || resource.state == stopped) &&
                    resource.owner == my_node_id_)
                {
                    resource.owner = "";
                    resource.notified = false;
                }
                else if (resource.state == acquiring && resource.owner == "")
                {
                    resource.owner = my_node_id_;
                    resource.notified = false;
                }
            }
        }
        for (auto& [name, resource] : resources_)
        {
            if (resource.notified) continue;
            if (resource.owner.size())
            {
                if (resource.on_busy)
                {
                    resource.on_busy(name, resource.owner, 0, resource.value);
                    resource.notified = true;
                }
            }
            else
            {
                if (resource.on_free)
                {
                    resource.on_free(name);
                    resource.notified = true;
                }
            }
        }
    }

    void update_peers_count(size_t count)
    {
        peers_count_cb_(count);
    }

    const std::string my_node_id_ = "this_node";
    std::map<std::string, resource_data> resources_;
    boost::asio::io_service* io_;
    bool update_enable_ = true;
    peers_count_cb peers_count_cb_;
};
