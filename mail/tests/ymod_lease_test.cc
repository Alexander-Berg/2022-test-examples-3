#include <yplatform/module_registration.h>

#include <string>
#include <list>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/bind.hpp>

#include <yplatform/module.h>
#include <yplatform/module_registration.h>

#include <ymod_webserver/server.h>

#include "../src/arbiter_impl.h"
#include "../src/messages.h"
#include "../src/node_impl.h"
#include "../src/types_priv.h"

namespace ylease {

class test_up : public yplatform::module
{
public:
    void process(ymod_webserver::http::stream_ptr stream, string res, bool active)
    {
        active ? lease_node_->start_acquire_lease(res) : lease_node_->stop_acquire_lease(res);
        stream->result(ymod_webserver::codes::ok, "");
    }

    void handle_busy(const std::string& resource_name, const node_id& node)
    {
        string winner = lease_node_->node_id() == node ? "me" : node;
        YLOG_L(info) << "resource \""
                     << "\" is busy, winner is " << winner;
    }

    void handle_free(const std::string& resource_name)
    {
        YLOG_L(info) << "resource \""
                     << "\" is free";
    }

    void handle_peers_count(size_t count)
    {
        YLOG_L(info) << "peers count: " << count;
    }

    void init(const yplatform::ptree& xml)
    {
        netch_ = yplatform::find<netch>("netch");
        lease_node_ = yplatform::find<node::impl>("node");

        lease_node_->bind(
            "a",
            boost::bind(&test_up::handle_busy, this, _1, _2),
            boost::bind(&test_up::handle_free, this, _1));

        lease_node_->subscribe_peers_count(boost::bind(&test_up::handle_peers_count, this, _1));

        auto webserver = yplatform::find<ymod_webserver::server>("webserver");
        webserver->bind(
            "",
            { "/acquire" },
            boost::bind(&test_up::process, shared_from(this), _1, _2, _3),
            ymod_webserver::transformer(
                ymod_webserver::argument<string>("res"), ymod_webserver::argument<bool>("active")));

        typedef yplatform::ptree::const_iterator Iterator;
        for (Iterator i = xml.begin(), e = xml.end(); i != e; ++i)
        {
            if (i->first == "peer")
            {
                L_(notice) << "testup adding peer " << i->second.data();
                peers_.push_back(i->second.data());
            }
        }

        L_(notice) << "testup inited";
    }

    void start()
    {
        stop_ = false;
        for (const std::string& peer : peers_)
        {
            netch_->connect(peer);
        }
    }

    void stop()
    {
        stop_ = true;
    }

private:
    boost::shared_ptr<netch> netch_;

    boost::shared_ptr<ymod_lease::node::impl> lease_node_;

    std::vector<string> peers_;

    bool stop_;
};

REGISTER_MODULE(ymod_lease::test_up)

}
