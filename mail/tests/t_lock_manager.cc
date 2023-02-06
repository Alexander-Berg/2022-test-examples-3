#include <ymod_lease/lock_manager.h>
#include <ymod_lease/types.h>
#include <tests/fake_lease_node.h>

#include <catch.hpp>
#include <boost/asio.hpp>
#include <boost/function.hpp>

#include <functional>
#include <memory>

namespace ph = std::placeholders;

struct lock_manager_test
{
    using lock_manager = ylease::lock_manager<fake_lease_node>;
    using lock_manager_ptr = std::shared_ptr<lock_manager>;
    using milliseconds = ylease::time_traits::milliseconds;

    lock_manager_test() : lease_node_(std::make_shared<fake_lease_node>(&io_))
    {
        YLOG_GLOBAL(info) << "lock_manager_test() -----------------------------";
        auto acquire_cb = std::bind(&lock_manager_test::on_acquire_resource, this, ph::_1);
        auto release_cb = std::bind(&lock_manager_test::on_release_resource, this, ph::_1);

        lock_manager_ = std::make_shared<lock_manager>(
            &io_, lease_node_, max_owned_count_, extra_acquire_count_);
        lock_manager_->init(acquire_cb, release_cb);
    }

    void on_acquire_resource(const std::string& name)
    {
        REQUIRE(acquired_resources_.count(name) == 0);
        acquired_resources_.insert(name);
        if (acquire_hook_)
        {
            acquire_hook_(name);
        }
    }

    void on_release_resource(const std::string& name)
    {
        REQUIRE(acquired_resources_.count(name) == 1);
        acquired_resources_.erase(name);
        if (release_hook_)
        {
            release_hook_(name);
        }
    }

    void run_for(ylease::time_duration duration)
    {
        io_.run_for(duration);
        io_.reset();
    }

    void run()
    {
        io_.run();
        io_.reset();
    }

    void check_is_owner(const std::string& resource)
    {
        lock_manager_->check_is_owner(resource, [](bool is_owner) { REQUIRE(is_owner); });
        run();
    }

    std::vector<std::string> gen_resources(const std::size_t count)
    {
        std::vector<std::string> ret;
        for (std::size_t i = 1; i <= count; ++i)
        {
            ret.push_back(std::to_string(i));
        }
        return ret;
    }

    boost::asio::io_service io_;
    std::shared_ptr<fake_lease_node> lease_node_;
    lock_manager_ptr lock_manager_;
    std::set<std::string> acquired_resources_;
    std::size_t max_owned_count_ = 5;
    std::size_t extra_acquire_count_ = 3;
    std::function<void(const std::string& resource)> acquire_hook_;
    std::function<void(const std::string& resource)> release_hook_;
};

TEST_CASE_METHOD(lock_manager_test, "should acquire resources")
{
    lock_manager_->on_add_resources({ "1", "2", "3" });

    run();
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(lock_manager_test, "should release part of resources when new peer occurs")
{
    lock_manager_->on_add_resources({ "1", "2", "3", "4" });

    run();
    REQUIRE(acquired_resources_.size() == 4);

    lock_manager_->update_peers_count(2);

    run();
    REQUIRE(acquired_resources_.size() == 2);
}

TEST_CASE_METHOD(lock_manager_test, "should acquire free resources when peers crash")
{
    lock_manager_->update_peers_count(3);
    lock_manager_->on_add_resources({ "1", "2", "3" });

    run();
    REQUIRE(acquired_resources_.size() == 1);

    lock_manager_->update_peers_count(1);

    run();
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(lock_manager_test, "should release resource when it was removed")
{
    lock_manager_->on_add_resources({ "1", "2", "3" });

    run();
    REQUIRE(acquired_resources_.size() == 3);

    lock_manager_->on_del_resources({ "3" });

    run();
    REQUIRE(acquired_resources_.size() == 2);
}

TEST_CASE_METHOD(lock_manager_test, "should acquire resource again if it was lost")
{
    lock_manager_->on_add_resources({ "1", "2", "3" });

    run();
    REQUIRE(acquired_resources_.size() == 3);

    lease_node_->free_resource("2");

    run();
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(
    lock_manager_test,
    "should release resource if it was lost and owned by some other node")
{
    lock_manager_->on_add_resources({ "1", "2", "3" });

    run();
    REQUIRE(acquired_resources_.size() == 3);

    lease_node_->acquire_resource("2", "some_other_node_id");

    run();
    REQUIRE(acquired_resources_.size() == 2);
}

TEST_CASE_METHOD(lock_manager_test, "should't acquire greater than max_acquired_count resources")
{
    acquire_hook_ = [this](const std::string&) {
        REQUIRE(acquired_resources_.size() <= max_owned_count_ + extra_acquire_count_);
    };

    auto resources = gen_resources(2 * (max_owned_count_ + extra_acquire_count_));
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(acquired_resources_.size() == max_owned_count_);
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_);
}

TEST_CASE_METHOD(lock_manager_test, "should't do anything with resource owned by other node")
{
    lease_node_->acquire_resource("1", "some_other_node_id");
    lock_manager_->on_add_resources({ "1" });

    run();
    REQUIRE(acquired_resources_.size() == 0);

    lock_manager_->on_del_resources({ "1" });

    run();
    REQUIRE(acquired_resources_.size() == 0);
}

TEST_CASE_METHOD(
    lock_manager_test,
    "should try acquire max_owned_count + extra_acquire_count resources on start")
{
    lease_node_->update_enable_ = false;

    auto resources = gen_resources(2 * (max_owned_count_ + extra_acquire_count_));
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_ + extra_acquire_count_);
}

TEST_CASE_METHOD(
    lock_manager_test,
    "should move to read_only free resources when max_owned_count resources were acquired")
{
    lease_node_->update_enable_ = false;

    auto resources = gen_resources(max_owned_count_ + extra_acquire_count_);
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_ + extra_acquire_count_);

    for (std::size_t i = 0; i < max_owned_count_; ++i)
    {
        lease_node_->acquire_resource(resources[i], lease_node_->my_node_id_);
    }

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_);
}

TEST_CASE_METHOD(
    lock_manager_test,
    "should move to read_only resources owned by other node and try to acquire free resources")
{
    lease_node_->update_enable_ = false;

    auto resources = gen_resources(max_owned_count_ + 2 * extra_acquire_count_);
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_ + extra_acquire_count_);

    std::size_t other_node_acquired_count = 0;
    for (auto& [name, resource] : lease_node_->resources_)
    {
        if (resource.state != fake_lease_node::acquiring) continue;
        if (other_node_acquired_count == extra_acquire_count_) break;
        lease_node_->acquire_resource(name, "other_node_id");
        ++other_node_acquired_count;
    }

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_ + extra_acquire_count_);
    for (auto& [name, resource] : lease_node_->resources_)
    {
        if (resource.state != fake_lease_node::acquiring) continue;
        REQUIRE(resource.owner != "other_node_id");
    }
}

TEST_CASE_METHOD(
    lock_manager_test,
    "should move to read only part of resources when new peer occurs")
{
    lease_node_->update_enable_ = false;

    auto resources = gen_resources(max_owned_count_ + extra_acquire_count_);
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(lease_node_->acquiring_count() == max_owned_count_ + extra_acquire_count_);

    lock_manager_->update_peers_count(2);

    run();
    REQUIRE(lease_node_->acquiring_count() < max_owned_count_ + extra_acquire_count_);
}

TEST_CASE_METHOD(lock_manager_test, "should not acquire released resources")
{
    lock_manager_->on_add_resources({ "1", "2" });

    run();
    REQUIRE(acquired_resources_.size() == 2);

    lock_manager_->on_add_resources({ "3" });
    lock_manager_->release_resources_for({ "1", "3" }, milliseconds(500));

    run_for(milliseconds(250));
    REQUIRE(acquired_resources_.size() == 1);

    run_for(milliseconds(500));
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(lock_manager_test, "should override released ts")
{
    lock_manager_->on_add_resources({ "1", "2" });

    run();
    REQUIRE(acquired_resources_.size() == 2);

    lock_manager_->release_resources_for({ "1", "2" }, milliseconds(1000));
    lock_manager_->release_resources_for({ "1" }, milliseconds(500));

    run_for(milliseconds(250));
    REQUIRE(acquired_resources_.size() == 0);

    run_for(milliseconds(500));
    REQUIRE(acquired_resources_.size() == 1);
}

TEST_CASE_METHOD(lock_manager_test, "should acquire new not released resources")
{
    auto resources = gen_resources(2 * max_owned_count_);
    lock_manager_->on_add_resources(resources);

    run();
    REQUIRE(acquired_resources_.size() == max_owned_count_);

    std::set<std::string> first_acquired = acquired_resources_;
    lock_manager_->release_resources_for(
        { first_acquired.begin(), first_acquired.end() }, milliseconds(999999));

    run_for(milliseconds(250));
    REQUIRE(acquired_resources_.size() == max_owned_count_);
    std::set<std::string> new_acquired = acquired_resources_;

    std::vector<std::string> intersection;
    std::set_intersection(
        first_acquired.begin(),
        first_acquired.end(),
        new_acquired.begin(),
        new_acquired.end(),
        std::back_inserter(intersection));
    REQUIRE(intersection.empty());
}

TEST_CASE_METHOD(lock_manager_test, "should not acquire released resources when they became free")
{
    lock_manager_->update_peers_count(3);
    lock_manager_->on_add_resources({ "1", "2" });
    lease_node_->acquire_resource("1", "some_other_node_id");
    lease_node_->acquire_resource("2", "some_other_node_id2");

    run();
    REQUIRE(acquired_resources_.size() == 0);

    lock_manager_->release_resources_for({ "1", "2" }, milliseconds(999999));
    lease_node_->free_resource("1");
    lease_node_->free_resource("2");

    run_for(milliseconds(250));
    REQUIRE(acquired_resources_.size() == 0);
}

TEST_CASE_METHOD(lock_manager_test, "should keep acquired resource when add existing resource")
{
    std::string resource = "1";
    lock_manager_->on_add_resources({ resource });
    run();
    check_is_owner(resource);
    lock_manager_->on_add_resources({ resource });
    run();
    check_is_owner(resource);
}

TEST_CASE_METHOD(lock_manager_test, "should not lose resources on peers count flap")
{
    lock_manager_->on_add_resources({ "1", "2", "3" });
    run();
    REQUIRE(acquired_resources_.size() == 3);
    lock_manager_->update_peers_count(3);
    lock_manager_->update_peers_count(1);
    run();
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(lock_manager_test, "should not lose resources on del and add")
{
    lock_manager_->on_add_resources({ "1" });
    run();
    REQUIRE(acquired_resources_.size() == 1);
    lock_manager_->on_del_resources({ "1" });
    lock_manager_->on_add_resources({ "1" });
    run();
    REQUIRE(acquired_resources_.size() == 1);
}

TEST_CASE_METHOD(lock_manager_test, "should return acquired resources")
{
    lock_manager_->on_add_resources({ "1", "2" });
    lock_manager_->update_peers_count(2);
    run();
    std::vector<std::string> acquired_resources;
    lock_manager_->get_acquired_resources(
        [&acquired_resources](const std::vector<std::string>& resources) {
            YLOG_G(info) << "acquired resources size " << resources.size();
            acquired_resources = resources;
        });
    run();
    REQUIRE(acquired_resources.size() == 1);
}

TEST_CASE_METHOD(lock_manager_test, "should notify when resource value updated")
{
    std::vector<std::pair<std::string, std::string>> updates;
    lock_manager_->subscribe_for_value_update(
        [&updates](const std::string& resource, const std::string& value) {
            updates.emplace_back(resource, value);
        });
    lock_manager_->update_peers_count(2);
    lock_manager_->on_add_resources({ "1", "2" });
    lease_node_->acquire_resource("2", "some_other_node_id");
    run();
    updates.clear();
    lease_node_->set_resource_value("1", "v1");
    lease_node_->set_resource_value("2", "v2");
    run();
    REQUIRE(updates.size() == 2);
    REQUIRE(updates[0].first == "1");
    REQUIRE(updates[0].second == "v1");
    REQUIRE(updates[1].first == "2");
    REQUIRE(updates[1].second == "v2");
}