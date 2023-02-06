#include "src/nodes_registry.h"

#include <catch.hpp>
#include <thread>

using namespace ylease;

struct notification
{
    string tag;
    std::vector<string> list;

    bool operator==(const notification& other) const
    {
        return std::tie(tag, list) == std::tie(other.tag, other.list);
    }
};

struct t_nodes_registry
{
public:
    t_nodes_registry()
    {
        registry = std::make_shared<nodes_registry>(
            io, [&](const string& tag, const std::vector<string>& list) {
                notifications.push_back(notification{ tag, list });
            });
    }

    boost::asio::io_service io;
    std::shared_ptr<nodes_registry> registry;
    std::vector<notification> notifications;
    time_duration interval = milliseconds(10);
};

TEST_CASE_METHOD(t_nodes_registry, "nodes_registry/idle", "no notifications on idle")
{
    io.poll();
    REQUIRE(notifications.size() == 0);
}

TEST_CASE_METHOD(t_nodes_registry, "nodes_registry/first_insert", "notifications on insert new tag")
{
    registry->insert("127.0.0.1", "node1", "a", interval);
    io.poll();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications.back() == notification{ "a", { "127.0.0.1" } });
}

TEST_CASE_METHOD(
    t_nodes_registry,
    "nodes_registry/insert_different",
    "notification on unique tag&node")
{
    registry->insert("127.0.0.1", "node1", "a", interval);
    io.poll();
    REQUIRE(notifications.size() == 1);

    registry->insert("127.0.0.1", "node1", "a", interval);
    io.poll();
    INFO(notifications.back().tag)
    REQUIRE(notifications.size() == 1);

    registry->insert("127.0.0.1", "node2", "a", interval);
    registry->insert("127.0.0.1", "node1", "b", interval);
    io.poll();
    REQUIRE(notifications.size() == 3);
}

TEST_CASE_METHOD(t_nodes_registry, "nodes_registry/erase", "notification on erase")
{
    registry->insert("127.0.0.1", "node1", "a", interval);
    io.poll();
    REQUIRE(notifications.size() == 1);

    std::this_thread::sleep_for(interval + milliseconds(1));
    io.poll();
    REQUIRE(notifications.size() == 2);
    REQUIRE(notifications.back() == notification{ "a", {} });
}

TEST_CASE_METHOD(t_nodes_registry, "nodes_registry/port_changed", "notification ")
{
    registry->insert("127.0.0.1:80", "node1", "a", interval);
    io.poll();
    registry->insert("127.0.0.1:81", "node1", "a", interval);
    io.poll();
    REQUIRE(notifications.size() == 2);
    REQUIRE(notifications.back() == notification{ "a", { "127.0.0.1:81" } });
}
