#include <ymod_messenger/ymod_messenger.h>
#include <ymod_lease/ymod_lease.h>
#include <ymod_webserver/server.h>
#include <yplatform/module.h>
#include <yplatform/find.h>
#include <yplatform/module_registration.h>
#include <yplatform/loader.h>

#include <iostream>

namespace xeno {

namespace ph = std::placeholders;

struct locker : public yplatform::module
{
    void init(const yplatform::ptree& conf)
    {
        auto reactor = yplatform::find_reactor(conf.get<std::string>("reactor"));
        for (size_t i = 0; i < reactor->size(); ++i)
        {
            if ((*reactor)[i]->size() != 1)
                throw std::runtime_error("locker is optimized for single-thread reactors - set "
                                         "pool_count=N and io_threads=1");
        }
        io = reactor->io();

        auto webserver = yplatform::find<ymod_webserver::server, std::shared_ptr>("web_server");
        webserver->bind(
            "",
            { "/check_lock" },
            io->wrap(std::bind(&locker::check_lock, shared_from(this), ph::_1, ph::_2)),
            ymod_webserver::transformer(ymod_webserver::argument<std::string>("resource")));

        std::set<std::string> hosts;
        auto lease_cluster = conf.equal_range("lease_cluster");
        for (auto& node : boost::make_iterator_range(lease_cluster))
        {
            hosts.insert(node.second.data());
        }
        auto messenger = yplatform::find<ymod_messenger::module, std::shared_ptr>("messenger");
        messenger->connect_to_cluster(hosts);

        auto target_resources = conf.equal_range("resources");
        for (auto& resource_item : boost::make_iterator_range(target_resources))
        {
            auto& resource = resource_item.second.data();
            resource_owners[resource] = "";
        }

        lease_node = yplatform::find<ymod_lease::node, std::shared_ptr>("lease_node");
        my_node_id = lease_node->node_id();
    }

    void start()
    {
        io->post([this, self = shared_from_this()] {
            for (auto& pair : resource_owners)
            {
                auto& resource = pair.first;
                lease_node->bind(
                    resource,
                    io->wrap(std::bind(
                        &locker::on_busy, shared_from(this), ph::_1, ph::_2, ph::_3, ph::_4)),
                    io->wrap(std::bind(&locker::on_free, shared_from(this), ph::_1)));
                lease_node->start_acquire_lease(resource);
            }
        });
    }

    void on_busy(
        const std::string& resource,
        const std::string& node_id,
        ymod_lease::ballot_t,
        const std::string& value)
    {
        YLOG_L(info) << "resource \"" << resource << "\" is busy; owner_id=\"" << node_id
                     << "\", value=\"" << value << "\"";
        resource_owners[resource] = node_id;
    }

    void on_free(const std::string& resource)
    {
        YLOG_L(info) << "resource \"" << resource << "\" is free";
        resource_owners[resource] = "";
    }

    void check_lock(ymod_webserver::http::stream_ptr stream, const std::string& resource)
    {
        auto it = resource_owners.find(resource);
        if (it != resource_owners.end())
        {
            auto& owner = it->second;
            stream->set_code(
                owner == my_node_id ? ymod_webserver::codes::ok : ymod_webserver::codes::forbidden);
            stream->result_body(owner);
        }
        else
        {
            stream->set_code(ymod_webserver::codes::not_found);
        }
    }

    boost::asio::io_service* io;
    std::string my_node_id;
    std::shared_ptr<ymod_lease::node> lease_node;
    std::map<std::string, std::string> resource_owners;
};

}

DEFINE_SERVICE_OBJECT(xeno::locker);

int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        std::cout << "usage " << argv[0] << " <config>\n";
        return 1;
    }

    return yplatform_start(argv[1]);
}
