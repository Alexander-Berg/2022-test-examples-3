#include "../src/convey/fcm_batch_aggregator.h"
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;
using namespace hub;

static constexpr size_t TEST_BATCH_SIZE = 3;

struct batcher_test
{
    batcher_test() : batcher(TEST_BATCH_SIZE)
    {
    }

    void add(const string& app_name, bool bright, string token = "")
    {
        push_subscription_params mob_sub;
        mob_sub.push_token = token;
        mob_sub.app_name = app_name;
        batcher.add(sub, mob_sub, bright, req, nullptr, fcm_batch_callback_t{});
    }

    fcm_batch_aggregator::data_ptr get()
    {
        return batcher.get_all();
    }

    fcm_batch_aggregator batcher;
    sub_t sub;
    push_requests_queue req;
};

TEST_CASE_METHOD(batcher_test, "batcher/correct_batch_size")
{
    for (auto i = 2 * TEST_BATCH_SIZE; i; --i)
    {
        add("app", true);
    }
    auto r = get();
    REQUIRE(r->size() == 2);
    CHECK((*r)[0]->subscriptions.size() == TEST_BATCH_SIZE);
    CHECK((*r)[1]->subscriptions.size() == TEST_BATCH_SIZE);
    for (auto& b : *r)
    {
        CHECK(b->tokens.front() == '[');
        CHECK(b->tokens.back() == ']');
    }
}

TEST_CASE_METHOD(batcher_test, "batcher/correct_batch_size2")
{
    for (int i = 2 * TEST_BATCH_SIZE + 1; i; --i)
    {
        add("app", true);
    }
    auto r = get();
    REQUIRE(r->size() == 3);
    CHECK((*r)[0]->subscriptions.size() == TEST_BATCH_SIZE);
    CHECK((*r)[1]->subscriptions.size() == TEST_BATCH_SIZE);
    CHECK((*r)[2]->subscriptions.size() == 1);
    for (auto& b : *r)
    {
        CHECK(b->tokens.front() == '[');
        CHECK(b->tokens.back() == ']');
    }
}

TEST_CASE_METHOD(batcher_test, "batcher/group_by_bright")
{
    add("app", true);
    add("app", false);
    REQUIRE(get()->size() == 2);
}

TEST_CASE_METHOD(batcher_test, "batcher/group_by_app")
{
    add("app1", true, "1");
    add("app2", true, "2");
    add("app1", true, "3");
    add("app2", true, "4");
    auto q = get();
    REQUIRE(q->size() == 2);
    CHECK(q->at(0)->tokens == "[\"1\",\"3\"]");
    CHECK(q->at(1)->tokens == "[\"2\",\"4\"]");
}
