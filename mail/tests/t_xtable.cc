#include "../src/xtable/impl.h"
#include <catch.hpp>

#define UID "1"
#define SERVICE "s"
#define SERVICENAME "s"
#define UUID "1234-1234-1234-123A"
#define LOWERCASE_UUID "1234-1234-1234-123a"
#define ANY_ID "7d844e5537fcaad113f83704456cfc070b048c3a"
#define MOB_ID "mob:123412341234123a"
#define FILTERED_UUID "123412341234123a"
#define FILTERED_MOB_ID "mob:123412341234123a"
#define XMOB_CALLBACK "xivamob:ru.yandex.push.test:p9u3s9s8h4t0o1k4e8n"
#define ANY_CALLBACK "http://any.yandex.net/blabla"
#define INACTIVE_CALLBACK "inactive://any.yandex.net/blabla"

namespace yxiva { namespace hub {

class test_context : public yplatform::task_context
{
public:
    const string& get_name() const override
    {
        static const string name = "test ctx";
        return name;
    }
};

struct test_xtable
{
    boost::shared_ptr<MemoryStorage> storage;
    std::shared_ptr<xtable_impl> xtable;
    add_callback_t empty_add_callback = [](const error_code&) {};
    subscribe_callback_t empty_subscribe_callback = [](const error_code&, const sub_t&) {};
    yplatform::task_context_ptr ctx = boost::make_shared<test_context>();

    test_xtable()
    {
        storage = boost::make_shared<MemoryStorage>();
        xtable = std::make_shared<xtable_impl>();
        xtable->init(storage);
    }

    sub_list sync_find(
        const string& uid,
        const string& service,
        const XTable::find_options& options = {})
    {
        sub_list result;
        xtable->find(
            ctx, uid, service, options, [&result](const error_code&, const sub_list& list) {
                result = list;
            });
        return result;
    }

    sub_list sync_storage_find(const string& uid, const string& service)
    {
        sub_list result;
        storage->find(ctx, uid, service, {}, [&result](const error_code&, const sub_list& list) {
            result = list;
        });
        return result;
    }

    void subscribe(
        task_context_ptr ctx,
        const string& uid,
        const string& service,
        const filter_t& filter,
        const string& callback_url,
        const string& extra_data,
        const string& client,
        const string& session_key,
        const ttl_t ttl,
        const string& subscription_id,
        const local_id_t local_id,
        const subscribe_callback_t& callback)
    {
        sub_t subscription;
        subscription.uid = uid;
        subscription.service = service;
        subscription.filter = filter;
        subscription.callback_url = callback_url;
        subscription.extra_data = extra_data;
        subscription.client = client;
        subscription.session_key = session_key;
        subscription.ttl = ttl;
        subscription.init_local_id = local_id;
        subscription.ack_local_id = local_id;
        subscription.id =
            subscription_id.empty() ? make_subscription_id(subscription) : subscription_id;
        xtable->subscribe(ctx, subscription, callback);
    }
};

TEST_CASE_METHOD(
    test_xtable,
    "xtable/hacks/subscribe() for any other subscriber erases delimiters",
    "store subscriptions using xtable interface and check values in DB")
{
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        ANY_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        ANY_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 2);
    REQUIRE(list[0].session_key == UUID);
    REQUIRE(list[1].session_key == UUID);
}

TEST_CASE_METHOD(
    test_xtable,
    "xtable/hacks/add xivamob via subscribe() doesnt erase delimiters",
    "")
{
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 2);
    REQUIRE(list[0].session_key == UUID);
    REQUIRE(list[1].session_key == UUID);
}

TEST_CASE_METHOD(
    test_xtable,
    "xtable/hacks/subscribe_mobile() erases delimiters gcm_compatibility",
    "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        "gcm",
        "test_device",
        "",
        empty_subscribe_callback); // gcm_compatibility

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].session_key == FILTERED_UUID);
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe_mobile() erases delimiters", "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        "fcm",
        "test_device",
        "",
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].session_key == FILTERED_UUID);
}

TEST_CASE_METHOD(
    test_xtable,
    "xtable/hacks/subscribe_mobile() applies tolower gcm_compatibility",
    "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        "UPPERLOWERCASE",
        1,
        "",
        1,
        "gcm",
        "test_device",
        "",
        empty_subscribe_callback); // gcm_compatibility

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].session_key == "upperlowercase");
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe_mobile() applies tolower", "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        "UPPERLOWERCASE",
        1,
        "",
        1,
        "fcm",
        "test_device",
        "",
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].session_key == "upperlowercase");
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe_mobile() updates entries with same uuids", "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        "fcm",
        "test_device",
        "",
        empty_subscribe_callback);
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "",
        1,
        "custom",
        "test_device",
        "",
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].client == "any");
    REQUIRE(list[0].platform == "custom");
}

TEST_CASE_METHOD(test_xtable, "xtable/implements_active_only_flag", "")
{
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        ANY_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        INACTIVE_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);

    SECTION("ignores_inactive_subscriptions_by_default", "")
    {
        auto list = sync_find(UID, SERVICE);
        REQUIRE(list.size() == 1);
        REQUIRE(list[0].callback_url == ANY_CALLBACK);
    }

    SECTION("includes_inactive_subscriptions_if_told_to", "")
    {
        XTable::find_options options;
        options.show_inactive = true;
        auto list = sync_find(UID, SERVICE, options);
        REQUIRE(list.size() == 2);
        REQUIRE(list[0].callback_url == ANY_CALLBACK);
        REQUIRE(list[1].callback_url == INACTIVE_CALLBACK);
    }
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe() subscription id can be omitted", "")
{
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        ANY_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "",
        1,
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].id == ANY_ID);
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe() subscription id can be explicitly set", "")
{
    subscribe(
        ctx,
        UID,
        SERVICENAME,
        {},
        ANY_CALLBACK,
        {},
        "mobile",
        UUID,
        1,
        "any_subscription_id",
        1,
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].id == "any_subscription_id");
}

TEST_CASE_METHOD(test_xtable, "xtable/hacks/subscribe_mobile() subscription id can be omitted", "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "",
        1,
        "custom",
        "test_device",
        "",
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].id == MOB_ID);
}

TEST_CASE_METHOD(
    test_xtable,
    "xtable/hacks/subscribe_mobile() subscription id can be explicitly set",
    "")
{
    xtable->subscribe_mobile(
        ctx,
        UID,
        SERVICENAME,
        {},
        XMOB_CALLBACK,
        {},
        "any",
        UUID,
        1,
        "mob_subscription_id",
        1,
        "custom",
        "test_device",
        "",
        empty_subscribe_callback);

    auto list = sync_storage_find(UID, SERVICE);
    REQUIRE(list.size() == 1);
    REQUIRE(list[0].id == "mob_subscription_id");
}

}}
