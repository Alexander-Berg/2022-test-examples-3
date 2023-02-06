#include "mod_apns/migration_sender.h"
#include "mock_clock.h"
#include <catch.hpp>
#include <limits>

namespace yxiva::mobile::apns {

struct hash_mock
{
    static size_t value;

    size_t operator()(const string& /*str*/)
    {
        value += 10;
        value = value > 100 ? 5 : value;
        return value;
    }
};

size_t hash_mock::value = 5;

using migration_sender_mock = migration_sender_impl<mock_clock, hash_mock>;
struct T_migration_sender
{
    struct sender_mock : sender
    {
        sender_mock(int& counter) : sender(yplatform::log::source()), counter(counter)
        {
        }
        void push(const mobile_task_context_ptr& /*ctx*/, callback_t&& /*cb*/) override
        {
            ++counter;
        }
        const string& secret_type() const override
        {
            static const string secret_type = "mock";
            return secret_type;
        }
        int& counter;
    };

    T_migration_sender()
    {
        auto sender_src = std::make_shared<sender_mock>(push_src_counter);
        auto sender_dst = std::make_shared<sender_mock>(push_dst_counter);
        sender = std::make_shared<migration_sender_mock>(
            std::move(sender_src),
            std::move(sender_dst),
            100,
            seconds(100),
            yplatform::log::source());

        mock_clock::set_now(100);

        auto ctx = boost::make_shared<ymod_webserver::context>();
        auto req = boost::make_shared<ymod_webserver::request>(ctx);
        task = boost::make_shared<mobile_task_context>(req);
    }

    int push_src_counter = 0;
    int push_dst_counter = 0;
    sender_ptr sender;
    mobile_task_context_ptr task;

    void batch_push(size_t n)
    {
        while (n--)
        {
            sender->push(task, {});
        }
    }
};

TEST_CASE_METHOD(T_migration_sender, "migration_sender/migration/gradual", "")
{
    batch_push(10);
    REQUIRE(push_src_counter == 10);
    REQUIRE(push_dst_counter == 0);

    mock_clock::wait(seconds(50));

    batch_push(10);
    REQUIRE(push_src_counter == 15);
    REQUIRE(push_dst_counter == 5);

    mock_clock::wait(seconds(50));

    batch_push(10);
    REQUIRE(push_src_counter == 15);
    REQUIRE(push_dst_counter == 15);
}

TEST_CASE_METHOD(T_migration_sender, "migration_sender/migration/after_finished", "")
{
    mock_clock::wait(seconds(150));

    batch_push(10);
    REQUIRE(push_src_counter == 0);
    REQUIRE(push_dst_counter == 10);

    mock_clock::wait(seconds(50));

    batch_push(10);
    REQUIRE(push_src_counter == 0);
    REQUIRE(push_dst_counter == 20);
}

}
