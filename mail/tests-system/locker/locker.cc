#include <yplatform/loader.h>
#include <ymod_lease/ymod_lease.h>
#include <ymod_webserver/server.h>
#include <yplatform/module.h>
#include <yplatform/find.h>
#include <yplatform/module_registration.h>

#include <set>
#include <unordered_map>
#include <memory>
#include <stdexcept>

namespace collectors {

namespace ph = std::placeholders;
namespace codes = ymod_webserver::codes;

using ymod_webserver::transformer;
using ymod_webserver::argument;
using ymod_webserver::http::stream_ptr;

class locker : public yplatform::module
{
public:
    void init(const yplatform::ptree& conf)
    {
        init_reactor(conf);
        init_lease();
        init_bindings();
    }

    void start()
    {
    }

private:
    void init_reactor(const yplatform::ptree& conf)
    {
        auto reactor = yplatform::find_reactor(conf.get<std::string>("reactor"));
        if (!reactor->plain())
            throw std::runtime_error("locker is optimized for single-thread reactors - set "
                                     "pool_count=N and io_threads=1");

        io = reactor->io();
    }

    void init_lease()
    {
        lease_node = yplatform::find<ymod_lease::node, std::shared_ptr>("lease_node");
        my_node_id = lease_node->node_id();
    }

    void init_bindings()
    {
        auto webserver = yplatform::find<ymod_webserver::server, std::shared_ptr>("web_server");

        webserver->bind(
            "", { "/ping" }, io->wrap(std::bind(&locker::ping, shared_from(this), ph::_1)));

        webserver->bind(
            "",
            { "/check_lock" },
            io->wrap(std::bind(&locker::check_lock, shared_from(this), ph::_1, ph::_2)),
            transformer(argument<std::string>("resource")));

        webserver->bind(
            "",
            { "/lock" },
            io->wrap(std::bind(&locker::lock, shared_from(this), ph::_1, ph::_2)),
            transformer(argument<std::string>("resource")));

        webserver->bind(
            "",
            { "/unlock" },
            io->wrap(std::bind(&locker::unlock, shared_from(this), ph::_1, ph::_2)),
            transformer(argument<std::string>("resource")));
    }

    void ping(stream_ptr stream)
    {
        stream->result(ymod_webserver::codes::ok, "pong");
    }

    void check_lock(stream_ptr stream, const std::string& resource)
    {
        if (resource_owners.contains(resource))
        {
            auto& owner = resource_owners[resource];
            stream->set_code(owner == my_node_id ? codes::ok : codes::forbidden);
            stream->result_body(owner);
        }
        else
        {
            stream->set_code(codes::not_found);
        }
    }

    void lock(stream_ptr stream, const std::string& resource)
    {
        bind_resource_callbacks(resource);
        lease_node->start_acquire_lease(resource);
        stream->set_code(codes::ok);
        YLOG_L(info) << "started acquiring resource " + resource;
    }

    void unlock(stream_ptr stream, const std::string& resource)
    {
        lease_node->stop_acquire_lease(resource);
        stream->set_code(codes::ok);
        YLOG_L(info) << "stopped acquiring resource " + resource;
    }

    void bind_resource_callbacks(const std::string& resource)
    {
        if (!resource_owners.contains(resource))
        {
            lease_node->bind(
                resource,
                io->wrap(
                    std::bind(&locker::on_busy, shared_from(this), ph::_1, ph::_2, ph::_3, ph::_4)),
                io->wrap(std::bind(&locker::on_free, shared_from(this), ph::_1)));
        }
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

    boost::asio::io_service* io;
    std::shared_ptr<ymod_lease::node> lease_node;
    std::string my_node_id;
    std::unordered_map<std::string, std::string> resource_owners;
};

}

DEFINE_SERVICE_OBJECT(collectors::locker);

int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        std::cout << "usage " << argv[0] << " <config>\n";
        return 1;
    }

    return yplatform_start(argv[1]);
}
