#include "fake_lease_node.h"

#include <ymod_lease/types.h>
#include <ymod_lease/bucket_manager.h>

#include <catch.hpp>
#include <boost/asio.hpp>
#include <boost/function.hpp>
#include <boost/optional.hpp>

#include <functional>
#include <memory>

namespace ph = std::placeholders;

struct bucket_manager_test
{
    using lock_manager = ylease::lock_manager<fake_lease_node>;
    using lock_manager_ptr = std::shared_ptr<lock_manager>;
    using bucket_manager = ylease::bucket_manager<fake_lease_node>;
    using bucket_manager_ptr = std::shared_ptr<bucket_manager>;
    using milliseconds = yplatform::time_traits::milliseconds;

    bucket_manager_test() : lease_node_(std::make_shared<fake_lease_node>(&io_))
    {
        YLOG_GLOBAL(info) << "bucket_manager_test() -----------------------------";
        auto acquire_cb = std::bind(&bucket_manager_test::on_acquire_resource, this, ph::_1);
        auto release_cb = std::bind(&bucket_manager_test::on_release_resource, this, ph::_1);

        lock_manager_ = std::make_shared<lock_manager>(
            &io_, lease_node_, max_owned_count_, extra_acquire_count_);

        bucket_manager_ = std::make_shared<bucket_manager>(&io_, lock_manager_, lease_node_);
        bucket_manager_->init(acquire_cb, release_cb);
    }

    void on_acquire_resource(const std::string& name)
    {
        REQUIRE(acquired_resources_.count(name) == 0);
        acquired_resources_.insert(name);
    }

    void on_release_resource(const std::string& name)
    {
        REQUIRE(acquired_resources_.count(name) == 1);
        acquired_resources_.erase(name);
    }

    void run_for(ymod_lease::time_duration duration)
    {
        io_.run_for(duration);
        io_.reset();
    }

    void run()
    {
        io_.run();
        io_.reset();
    }

    void check_acquire_buckets_and_resources(size_t buckets_size, size_t resources_size)
    {
        io_.post([this, buckets_size]() {
            bucket_manager_->get_acquired_buckets(
                [buckets_size](const std::vector<std::string>& buckets) {
                    REQUIRE(buckets.size() == buckets_size);
                });
        });
        run();
        REQUIRE(acquired_resources_.size() == resources_size);
    }

    void check_resource_value(const std::string& resource, const std::string& expected_value)
    {
        io_.post([this, resource, expected_value]() {
            bucket_manager_->get_resource_value(
                resource,
                [expected_value](const std::string& value) { REQUIRE(expected_value == value); });
        });
        run();
    }

    void add_buckets_and_check(size_t buckets_size, size_t resources_size)
    {
        auto buckets = gen_buckets(buckets_size, resources_size);
        bucket_manager_->add_buckets(buckets);
        run();
        check_acquire_buckets_and_resources(buckets_size, buckets_size * resources_size);
    }

    void update_lease_node()
    {
        auto prev_update_enable = lease_node_->update_enable_;
        lease_node_->update_enable_ = true;
        lease_node_->update();
        lease_node_->update_enable_ = prev_update_enable;
    }

    void acquire_resource_and_set_value(
        const std::string& resource,
        const std::string& owner,
        const std::string& value)
    {
        lease_node_->acquire_resource(resource, owner);
        lease_node_->set_resource_value(resource, value);
        run();
    }

    ylease::buckets_resources_map gen_buckets(size_t buckets_size, size_t resources_size)
    {
        ylease::buckets_resources_map buckets;
        size_t resources_count = 0;
        for (size_t bucket_count = 0; bucket_count < buckets_size; ++bucket_count)
        {
            auto& inserted_bucket = buckets["b" + std::to_string(bucket_count)];
            for (size_t i = 0; i < resources_size; ++i, ++resources_count)
            {
                inserted_bucket.insert(std::to_string(resources_count));
            }
        }
        return buckets;
    }

    boost::asio::io_service io_;
    std::shared_ptr<fake_lease_node> lease_node_;
    lock_manager_ptr lock_manager_;
    bucket_manager_ptr bucket_manager_;
    std::set<std::string> acquired_resources_;
    std::size_t max_owned_count_ = 5;
    std::size_t extra_acquire_count_ = 3;
};

TEST_CASE_METHOD(bucket_manager_test, "should acquire buckets with resources")
{
    add_buckets_and_check(2, 2);
}

TEST_CASE_METHOD(bucket_manager_test, "should acquire resources later if it locked")
{
    lease_node_->acquire_resource("2", "some_other_node_id");
    run();
    auto buckets = gen_buckets(1, 3);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 2);
    lease_node_->free_resource("2");
    run();
    REQUIRE(acquired_resources_.size() == 3);
}

TEST_CASE_METHOD(
    bucket_manager_test,
    "should release part of buckets with resources when new peer occurs")
{
    add_buckets_and_check(2, 2);
    lease_node_->update_peers_count(2);
    run();
    check_acquire_buckets_and_resources(1, 2);
}

TEST_CASE_METHOD(bucket_manager_test, "should acquire free buckets with resources when peers crash")
{
    lease_node_->update_peers_count(3);
    auto buckets = gen_buckets(3, 2);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 2);
    lease_node_->update_peers_count(1);
    run();
    check_acquire_buckets_and_resources(3, 6);
}

TEST_CASE_METHOD(bucket_manager_test, "should release bucket with resources when it was removed")
{
    add_buckets_and_check(3, 2);
    bucket_manager_->del_buckets({ "b0" });
    run();
    check_acquire_buckets_and_resources(2, 4);
}

TEST_CASE_METHOD(
    bucket_manager_test,
    "should acquire added resources and release removed resources")
{
    auto buckets = gen_buckets(1, 2);
    bucket_manager_->add_buckets(buckets);
    run();
    REQUIRE(acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
    buckets["b0"].erase("0");
    buckets["b0"].insert("2");
    bucket_manager_->upd_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 2);
    REQUIRE(acquired_resources_.count("1"));
    REQUIRE(acquired_resources_.count("2"));
}

TEST_CASE_METHOD(bucket_manager_test, "should acquire added resources")
{
    add_buckets_and_check(1, 2);
    REQUIRE(acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
    bucket_manager_->add_resources_to_bucket("b0", { "2", "3" });
    run();
    check_acquire_buckets_and_resources(1, 4);
    REQUIRE(acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
    REQUIRE(acquired_resources_.count("2"));
    REQUIRE(acquired_resources_.count("3"));
}

TEST_CASE_METHOD(bucket_manager_test, "should add bucket and acquire resources")
{
    bucket_manager_->add_resources_to_bucket("b0", { "0" });
    bucket_manager_->add_resources_to_bucket("b0", { "1" });
    run();
    check_acquire_buckets_and_resources(1, 2);
    REQUIRE(acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
}

TEST_CASE_METHOD(bucket_manager_test, "should release removed resources")
{
    add_buckets_and_check(1, 3);
    REQUIRE(acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
    REQUIRE(acquired_resources_.count("2"));
    bucket_manager_->del_resources_from_bucket("b0", { "0", "2" });
    run();
    check_acquire_buckets_and_resources(1, 1);
    REQUIRE(!acquired_resources_.count("0"));
    REQUIRE(acquired_resources_.count("1"));
    REQUIRE(!acquired_resources_.count("2"));
}

TEST_CASE_METHOD(bucket_manager_test, "should acquire bucket with resources again if it was lost")
{
    add_buckets_and_check(3, 2);
    lease_node_->free_resource("b2");
    run();
    check_acquire_buckets_and_resources(3, 6);
    lease_node_->free_resource("2");
    run();
    check_acquire_buckets_and_resources(3, 6);
}

TEST_CASE_METHOD(bucket_manager_test, "should release resources and acquire them after delay")
{
    add_buckets_and_check(3, 2);
    auto delay = milliseconds(1000);
    bucket_manager_->release_buckets_for({ "b0", "b2" }, delay);
    run_for(delay / 2);
    REQUIRE(acquired_resources_.size() == 2);
    run_for(delay / 2 + milliseconds(5)); // Keep gap to prevent flapping
    REQUIRE(acquired_resources_.size() == 6);
}

TEST_CASE_METHOD(bucket_manager_test, "should release resource when bucket lost")
{
    auto buckets = gen_buckets(1, 2);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 2);

    lease_node_->acquire_resource("b0", "some_other_node");
    run();
    check_acquire_buckets_and_resources(0, 0);
}

TEST_CASE_METHOD(bucket_manager_test, "should get owner")
{
    auto owner = "some_other_node_id";
    auto host = "host_xeno";
    acquire_resource_and_set_value("1", owner, host);
    bucket_manager_->add_resources_to_bucket("b0", { "1" });
    run();
    check_resource_value("1", host);
}

TEST_CASE_METHOD(bucket_manager_test, "should not lose resources on bucket flap")
{
    auto buckets = gen_buckets(1, 1);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 1);

    io_.post(std::bind(&fake_lease_node::free_resource, lease_node_, "b0"));
    io_.post(
        std::bind(&fake_lease_node::acquire_resource, lease_node_, "b0", lease_node_->node_id()));
    run();
    check_acquire_buckets_and_resources(1, 1);
}

TEST_CASE_METHOD(bucket_manager_test, "should not lose buckets on del and add")
{
    auto buckets = gen_buckets(1, 1);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 1);

    bucket_manager_->del_buckets({ "b0" });
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 1);
}

TEST_CASE_METHOD(bucket_manager_test, "should not lose resources from buckets on del and add")
{
    auto buckets = gen_buckets(1, 1);
    bucket_manager_->add_buckets(buckets);
    run();
    check_acquire_buckets_and_resources(1, 1);

    bucket_manager_->del_resources_from_bucket("b0", { "0" });
    bucket_manager_->add_resources_to_bucket("b0", { "0" });
    run();
    check_acquire_buckets_and_resources(1, 1);
}

TEST_CASE_METHOD(bucket_manager_test, "should not lose values on del and add resource")
{
    lease_node_->acquire_resource("b0", "other_node");
    lease_node_->acquire_resource("0", "other_node");
    lease_node_->set_resource_value("0", "val");
    run();

    auto buckets = gen_buckets(1, 1);
    bucket_manager_->add_buckets(buckets);
    run();

    std::string value;
    bucket_manager_->get_resource_value("0", [&value](const std::string& val) { value = val; });
    run();
    REQUIRE(value == "val");

    bucket_manager_->del_resources_from_bucket("b0", { "0" });
    bucket_manager_->add_resources_to_bucket("b0", { "0" });
    run();

    value.clear();
    bucket_manager_->get_resource_value("0", [&value](const std::string& val) { value = val; });
    run();
    REQUIRE(value == "val");
}
