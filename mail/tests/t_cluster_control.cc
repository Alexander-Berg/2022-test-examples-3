#include "../src/control_module.h"
#include "../src/state.h"
#include <ymod_messenger/module.h>
#include <ymod_lease/node.h>
#include <catch.hpp>

using std::string;
using yplatform::ptree;

#define NO_IMPL throw std::runtime_error("not implemented");

namespace yxiva { namespace hub {

struct fake_xtasks_service : xtasks_service
{
    fake_xtasks_service() : xtasks_service(nullptr, nullptr)
    {
    }
    void start() override
    {
        running = true;
    }
    void stop() override
    {
        running = false;
    }
    bool running = false;
};

struct fake_netch : ymod_messenger::module
{
    const string& my_address() const override
    {
        static const string address = "host:1111";
        return address;
    }
    void connect_to_cluster(const std::set<string>& peers) override
    {
        ++connects;
        last_peers = peers;
    }
    unsigned connects = 0;
    std::set<string> last_peers;
};

struct fake_paxos_node : ymod_paxos::node
{
    void perform(
        const ymod_paxos::task_context_ptr&,
        ymod_paxos::operation,
        std::shared_ptr<ymod_paxos::icaller>) override
    {
        NO_IMPL
    }
    bool is_master() override
    {
        NO_IMPL
    }
    void reset_replica() override{ NO_IMPL } status get_status() override
    {
        NO_IMPL
    }
    void set_id(unsigned /* id */) override
    {
    }
};

struct fake_lease_node : ylease::node
{
    const ylease::node_id& node_id() override
    {
        static const string id = "node_id";
        return id;
    }
    void bind(
        const std::string& res,
        const ylease::busy_callback& busy_callback_,
        const ylease::free_callback& free_callback_) override
    {
        resource = res;
        busy_callback = busy_callback_;
        free_callback = free_callback_;
    }
    void subscribe_peers_count(const ylease::peers_count_callback&) override
    {
        NO_IMPL
    }
    void start_acquire_lease(const std::string& res) override
    {
        REQUIRE(resource == res);
        REQUIRE(acquiring == 0);
        ++acquiring;
    }
    void start_read_only(const std::string& /* res */) override
    {
        NO_IMPL
    }
    void update_acquire_value(const std::string& res, const ylease::value& new_value) override
    {
        REQUIRE(acquiring);
        REQUIRE(resource == res);
        value = new_value;
    }
    void stop_acquire_lease(const std::string&) override
    {
        resource = "";
        --acquiring;
    }
    void call_busy(string node, string value)
    {
        busy_callback(resource, node, 0 /*fake_ballot*/, value);
    }
    void call_free()
    {
        free_callback(resource);
    }
    ylease::busy_callback busy_callback;
    ylease::free_callback free_callback;
    unsigned acquiring = 0;
    string resource;
    string value;
};

struct cluster_control_fixture_base
{
    cluster_control_fixture_base()
    {
        reactor.init(1, 1);
        state->xtasks_service = xtasks_service;
        state->settings = boost::make_shared<settings_t>();
        state->sync_netch = state->lease_netch = state->netch = netch;
        state->paxos_node = paxos_node;
        state->leasemeta = state->lease_node = lease_node;
        state->settings->cluster.name = "test_cluster";
    }

    yplatform::reactor reactor;
    std::shared_ptr<fake_xtasks_service> xtasks_service{ new fake_xtasks_service() };
    std::shared_ptr<hub::state> state{ new hub::state() };
    std::shared_ptr<fake_netch> netch{ new fake_netch() };
    boost::shared_ptr<fake_paxos_node> paxos_node{ new fake_paxos_node() };
    boost::shared_ptr<fake_lease_node> lease_node{ new fake_lease_node() };
    std::shared_ptr<control_module> control;
};

// ====== static metadata

struct cluster_control_static_metadata : cluster_control_fixture_base
{
    cluster_control_static_metadata() : cluster_control_fixture_base()
    {
        state->settings->cluster.use_static_metadata = true;
        state->settings->cluster.static_metadata.valid = true;
        state->settings->cluster.static_metadata.peers = { peer_info{ 1, "host:1111" },
                                                           peer_info{ 2, "host:2222" },
                                                           peer_info{ 3, "host:3333" } };
        control = std::make_shared<control_module>(reactor, state);
    }
};

TEST_CASE_METHOD(cluster_control_static_metadata, "pre_requires")
{
    REQUIRE(state->stats.control_leader == false);
    REQUIRE(state->stats.convey_enabled == false);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "post_init_state")
{
    control->init();
    REQUIRE(state->stats.control_leader == false);
    REQUIRE(state->stats.convey_enabled == false);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "post_init_connect")
{
    REQUIRE(netch->connects == 0);
    control->init();
    REQUIRE(netch->connects == 3);
    REQUIRE(netch->last_peers.size() == 3);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "init_starts_acquire")
{
    REQUIRE(lease_node->acquiring == 0);
    control->init();
    REQUIRE(lease_node->acquiring == 1);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "cb_free_dont_enable_delivery")
{
    control->init();
    lease_node->call_free();
    REQUIRE(state->stats.control_leader == false);
    REQUIRE(state->stats.convey_enabled == false);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "cb_free_finalize_app")
{
    control->init();
    lease_node->call_free();
    REQUIRE(state->stats.finalize == true);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_no_value_give_weak_delivery")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_no_value_enables_xtasks_service")
{
    control->init();
    REQUIRE(xtasks_service->running == false);
    lease_node->call_busy(lease_node->node_id(), "");
    REQUIRE(state->stats.control_leader == true);
    REQUIRE(xtasks_service->running == true);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "busy_with_no_value_give_weak_delivery")
{
    control->init();
    lease_node->call_busy("other", "");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "busy_with_no_value_dont_enable_xtasks_service")
{
    control->init();
    lease_node->call_busy("other", "");
    REQUIRE(state->stats.control_leader == false);
    REQUIRE(xtasks_service->running == false);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_no_value_dont_finalize_app")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "");
    REQUIRE(state->stats.finalize == false);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_empty_json_value_give_weak_delivery")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "{}");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_invalid_value_give_weak_delivery")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "trashtrashtrashtrashtrash");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(state->stats.robust_delivery == 0);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "free_after_win_do_finalize_app")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "");
    lease_node->call_free();
    REQUIRE(state->stats.convey_enabled == false);
    REQUIRE(state->stats.finalize == true);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_free_win_reset_finalize_app")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "");
    lease_node->call_free();
    lease_node->call_busy(lease_node->node_id(), "");
    REQUIRE(state->stats.finalize == false);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_then_busy_only_stop_xtasks_service")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "");
    lease_node->call_busy("unknown_node", "");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(xtasks_service->running == false);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "busy_then_win_only_stop_xtasks_service")
{
    control->init();
    lease_node->call_busy("unknown_node", "");
    lease_node->call_busy(lease_node->node_id(), "");
    REQUIRE(state->stats.convey_enabled == true);
    REQUIRE(xtasks_service->running == true);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "win_with_valid_value_dont_reconfig_network")
{
    control->init();
    auto new_metadata = state->settings->cluster.static_metadata;
    new_metadata.peers.erase({ 3, "host:3333" });
    lease_node->call_busy(lease_node->node_id(), new_metadata.dump());
    REQUIRE(netch->connects == 3);
}

TEST_CASE_METHOD(cluster_control_static_metadata, "new_delivery_mode_do_apply")
{
    control->init();
    auto new_metadata = state->settings->cluster.static_metadata;
    new_metadata.robust_delivery = false;
    lease_node->call_busy(lease_node->node_id(), new_metadata.dump());
    REQUIRE(state->stats.robust_delivery == false);

    new_metadata.robust_delivery = true;
    lease_node->call_busy(lease_node->node_id(), new_metadata.dump());
    REQUIRE(state->stats.robust_delivery == true);
}

// ====== leasemeta

struct cluster_control_leasemeta : cluster_control_fixture_base
{
    cluster_control_leasemeta() : cluster_control_fixture_base()
    {
        state->settings->cluster.use_static_metadata = false;
        state->settings->cluster.leasemeta.arbiters = { "arbiter-a", "arbiter-b", "arbiter-c" };
        state->settings->cluster.lease_port_offset = 1;
        state->settings->cluster.sync_port_offset = 2;
        control = std::make_shared<control_module>(reactor, state);
        metadata.valid = true;
        metadata.peers = { peer_info{ 1, "host:1111" },
                           peer_info{ 2, "host:2222" },
                           peer_info{ 3, "host:3333" } };
    }

    cluster_metadata metadata;
};

TEST_CASE_METHOD(cluster_control_leasemeta, "win_with_valid_value_do_first_network_reconfig")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    REQUIRE(netch->connects == 3);
    REQUIRE(netch->last_peers.size() == 3);
}

TEST_CASE_METHOD(cluster_control_leasemeta, "busy_with_valid_value_do_first_network_reconfig")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    REQUIRE(netch->connects == 3);
    REQUIRE(netch->last_peers.size() == 3);
}

TEST_CASE_METHOD(cluster_control_leasemeta, "win_update_do_network_reconfig")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    metadata.peers.erase({ 3, "host:3333" });
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    REQUIRE(netch->connects == 6);
    REQUIRE(netch->last_peers.size() == 2);
}

TEST_CASE_METHOD(cluster_control_leasemeta, "busy_update_do_network_reconfig")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    metadata.peers.erase({ 3, "host:3333" });
    lease_node->call_busy("unknown_node", metadata.dump());
    REQUIRE(netch->connects == 6);
    REQUIRE(netch->last_peers.size() == 2);
}

TEST_CASE_METHOD(cluster_control_leasemeta, "set_valid_topology_do_update_lease_value")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    metadata.peers.erase({ 3, "host:3333" });

    REQUIRE(lease_node->value == "");
    control->async_set_topology(metadata.peers, [](auto, auto) -> bool { return true; });
    reactor.io()->run();
    REQUIRE(lease_node->value == metadata.dump());
}

TEST_CASE_METHOD(cluster_control_leasemeta, "set_valid_topology_over_invalid_do_update_lease_value")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), "~~invalid~~");

    REQUIRE(lease_node->value == "");
    control->async_set_topology(metadata.peers, [](auto, auto) -> bool { return true; });
    reactor.io()->run();
    REQUIRE(lease_node->value == metadata.dump());
}

TEST_CASE_METHOD(cluster_control_leasemeta, "set_invalid_topology_do_nothing")
{
    control->init();
    lease_node->call_busy(lease_node->node_id(), metadata.dump());
    metadata.peers.erase({ 3, "host:3333" });

    REQUIRE(lease_node->value == "");
    control->async_set_topology(metadata.peers, [](auto, auto) -> bool { return false; });
    reactor.io()->run();
    REQUIRE(lease_node->value == "");
}

/* tests:
@@ force_weak
*/

}}
