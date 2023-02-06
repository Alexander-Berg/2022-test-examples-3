#include "service_manager_mocks.h"

#include <catch.hpp>
#include <algorithm>
#include <set>

using namespace yxiva::platform;

TEST_CASE("service_manager/empty", "")
{
    auto manager = service_manager_mock::create_manager();

    CHECK(!manager->find_service_by_send_token("test_send_token"));
    CHECK(!manager->find_service_by_listen_token("test_listen_token"));
    CHECK(!manager->find_service_by_name("test_service"));
    auto res = manager->test_find_services_by_owner("corp:user");
    CHECK(res.first);
    CHECK(res.second.empty());
}

TEST_CASE("service_manager/add_and_remove_service", "")
{
    auto manager = service_manager_mock::create_manager();
    item_builder b;

    service_properties data;
    data.name = "test_service";
    data.owner_prefix = "corp:";
    data.owner_id = "user";
    data.is_passport = true;
    manager->add_conf({ b.token("").name("test_service").owner("corp:user").data(data).build() });

    CHECK(!manager->find_service_by_send_token("test_send_token"));
    CHECK(!manager->find_service_by_listen_token("test_listen_token"));

    auto serv = manager->find_service_by_name("test_service");
    REQUIRE(serv);
    CHECK(serv->properties.name == "test_service");
    CHECK(serv->properties.is_passport);
    CHECK(!serv->properties.is_stream);

    auto res = manager->test_find_services_by_owner("corp:user");
    CHECK(res.first);
    REQUIRE(res.second.size() == 1);
    CHECK(res.second.front()->properties.name == "test_service");
    auto res2 = manager->test_find_services_by_owner("corp:user2");
    CHECK(res2.first);
    CHECK(res2.second.empty());

    // Delete the last record
    manager->add_conf({ b.svc_data("test_service", "corp:user", true).build() });

    auto revoked_svc = manager->find_service_by_name("test_service");
    REQUIRE(revoked_svc);
    CHECK(revoked_svc->properties.revoked);

    auto res3 = manager->test_find_services_by_owner("corp:user");
    REQUIRE(res3.first);
    REQUIRE(res3.second.size() == 1);
    CHECK(res3.second.front()->properties.name == "test_service");
    CHECK(res3.second.front()->properties.revoked);
}

TEST_CASE("service_manager/add_and_remove_tokens", "")
{
    auto manager = service_manager_mock::create_manager();
    item_builder b;

    // Add services and tokens.
    manager->add_conf({ b.token("").svc_data("svc1", "o1").build(),
                        b.svc_data("svc2", "o2").build(),
                        b.token("ltoken1").ltoken_data("svc1", "lt1").build(),
                        b.token("ltoken2").ltoken_data("svc1", "lt2").build(),
                        b.token("ltoken3").ltoken_data("svc2", "lt3").build(),
                        b.token("stoken1").stoken_data("svc1", "st1").build(),
                        b.token("stoken2").stoken_data("svc2", "st1").build() });

    auto svc1 = manager->find_service_by_name("svc1");
    auto svc2 = manager->find_service_by_name("svc2");
    REQUIRE(svc1);
    REQUIRE(svc2);
    REQUIRE(svc1 != svc2);

    CHECK(manager->find_service_by_send_token("stoken1") == svc1);
    CHECK(manager->find_service_by_send_token("stoken2") == svc2);
    CHECK(manager->find_service_by_listen_token("ltoken1") == svc1);
    CHECK(manager->find_service_by_listen_token("ltoken2") == svc1);
    CHECK(manager->find_service_by_listen_token("ltoken3") == svc2);

    // Revoke all the tokens.
    manager->add_conf(
        { b.name("svc1:lt1").token("ltoken1").ltoken_data("svc1", "lt1", true).build(),
          b.name("svc1:lt2").token("ltoken2").ltoken_data("svc1", "lt2", true).build(),
          b.name("svc2:lt3").token("ltoken3").ltoken_data("svc2", "lt3", true).build(),
          b.name("svc1:st1").token("stoken1").stoken_data("svc1", "st1", true).build(),
          b.name("svc2:st1").token("stoken2").stoken_data("svc2", "st1", true).build() });

    // Services are not removed.
    REQUIRE(manager->find_service_by_name("svc1"));
    REQUIRE(manager->find_service_by_name("svc2"));

    CHECK(!manager->find_service_by_send_token("stoken1"));
    CHECK(!manager->find_service_by_send_token("stoken2"));
    CHECK(!manager->find_service_by_listen_token("ltoken1"));
    CHECK(!manager->find_service_by_listen_token("ltoken2"));
    CHECK(!manager->find_service_by_listen_token("ltoken3"));
}

TEST_CASE("service_manager/update_on_request", "")
{
    item_builder b;
    // Update will remove svc1 and add svc2.
    std::vector<item> start_items{ b.svc_data("svc1", "o1").build() },
        update_items{ b.svc_data("svc2", "o1").build(), b.svc_data("svc1", "o1", true).build() };

    auto update_config = std::make_shared<conf_list>();
    std::swap(update_config->items, update_items);
    update_config->max_revision = update_config->items.back().revision;
    auto manager = service_manager_mock::create_manager(update_config);
    manager->add_conf(std::move(start_items));

    auto services = manager->test_find_services_by_owner("o1");
    CHECK(services.first);
    REQUIRE(services.second.size() == 2);
    std::map<string, bool> rev_stat = { { "svc1", true }, { "svc2", false } };
    for (auto& service : services.second)
    {
        CHECK(rev_stat.count(service->properties.name));
        CHECK(rev_stat[service->properties.name] == service->properties.revoked);
    }
}

TEST_CASE("service_manager/process_items_in_reversed_order", "")
{
    auto manager = service_manager_mock::create_manager();
    item_builder b;

    // Add services and tokens.
    std::vector<item> items = {
        b.token("").svc_data("svc1", "o1").build(),
        b.svc_data("svc2", "o2").build(),
        b.svc_data("svc3", "o3").build(),
        b.token("ltoken1").ltoken_data("svc1", "lt1", true).build(),
        b.token("ltoken2").ltoken_data("svc1", "lt2").build(),
        b.token("ltoken3").ltoken_data("svc2", "lt2").build(),
        b.token("stoken1").stoken_data("svc1", "st1", true).build(),
        b.token("stoken2").stoken_data("svc1", "st2").build(),
        b.token("stoken3").stoken_data("svc2", "st1").build(),
        b.name("fcm:app")
            .token("")
            .owner("xivaservice:svc3")
            .data(application_config{ "", "svc3", "fcm", "app", 0, "FAKE", 0, "", {}, 0 })
            .build(),
        b.name("gcm:app2")
            .token("")
            .owner("xivaservice:svc3")
            .data( // gcm_compatibility
                application_config{ "", "svc3", "gcm", "app2", 0, "FAKE", 0, "", {}, 0 })
            .build()
    }; // gcm_compatibility
    // Items not ordered by revision, as in loaded dump.
    std::reverse(items.begin(), items.end());
    manager->add_conf(std::move(items));

    auto svc1 = manager->find_service_by_name("svc1");
    auto svc2 = manager->find_service_by_name("svc2");
    auto svc3 = manager->find_service_by_name("svc3");
    REQUIRE(svc1);
    REQUIRE(svc2);
    REQUIRE(svc3);
    CHECK(svc1->send_tokens.size() == 2);
    CHECK(svc1->listen_tokens.size() == 2);
    CHECK(svc1->apps.empty());
    CHECK(svc2->send_tokens.size() == 1);
    CHECK(svc2->listen_tokens.size() == 1);
    CHECK(svc2->apps.empty());
    CHECK(svc3->send_tokens.empty());
    CHECK(svc3->listen_tokens.empty());
    REQUIRE(
        svc1->send_tokens.count({ "stoken1", get_environment_name(config_environment::SANDBOX) }) ==
        1);
    REQUIRE(
        svc1->send_tokens.count({ "stoken2", get_environment_name(config_environment::SANDBOX) }) ==
        1);
    CHECK(svc1->send_tokens.at({ "stoken1", get_environment_name(config_environment::SANDBOX) })
              .revoked);
    CHECK(!svc1->send_tokens.at({ "stoken2", get_environment_name(config_environment::SANDBOX) })
               .revoked);
    CHECK(
        svc2->send_tokens.count({ "stoken3", get_environment_name(config_environment::SANDBOX) }) ==
        1);
    REQUIRE(
        svc1->listen_tokens.count(
            { "ltoken1", get_environment_name(config_environment::SANDBOX) }) == 1);
    REQUIRE(
        svc1->listen_tokens.count(
            { "ltoken2", get_environment_name(config_environment::SANDBOX) }) == 1);
    CHECK(svc1->listen_tokens.at({ "ltoken1", get_environment_name(config_environment::SANDBOX) })
              .revoked);
    CHECK(!svc1->listen_tokens.at({ "ltoken2", get_environment_name(config_environment::SANDBOX) })
               .revoked);
    CHECK(
        svc2->listen_tokens.count(
            { "ltoken3", get_environment_name(config_environment::SANDBOX) }) == 1);
    CHECK(svc3->apps.size() == 2);
    CHECK(svc3->apps.count(std::tie("fcm", "app")) == 1);
    CHECK(svc3->apps.count(std::tie("fcm", "app2")) == 1); // gcm_compatibility

    CHECK(!manager->find_service_by_listen_token("ltoken1"));
    CHECK(manager->find_service_by_listen_token("ltoken2") == svc1);
    CHECK(manager->find_service_by_listen_token("ltoken3") == svc2);
    CHECK(!manager->find_service_by_send_token("stoken1"));
    CHECK(manager->find_service_by_send_token("stoken2") == svc1);
    CHECK(manager->find_service_by_send_token("stoken3") == svc2);

    auto o1 = manager->test_find_services_by_owner("o1");
    auto o2 = manager->test_find_services_by_owner("o2");
    auto o3 = manager->test_find_services_by_owner("o3");
    REQUIRE(o1.first);
    REQUIRE(o2.first);
    REQUIRE(o3.first);
    REQUIRE(o1.second.size() == 1);
    REQUIRE(o2.second.size() == 1);
    REQUIRE(o3.second.size() == 1);
    REQUIRE(o1.second.front() == svc1);
    REQUIRE(o2.second.front() == svc2);
    REQUIRE(o3.second.front() == svc3);
}

TEST_CASE("service_manager/queue_xconf_list_requests", "")
{
    auto manager = service_manager_mock::create_manager();

    std::atomic_size_t cnt{ 0 };
    auto xconf = manager->get_xconf();
    CHECK(xconf->get_count() == 0);

    manager->test_schedule_xconf_update([&cnt](const operation::result&) { ++cnt; });
    manager->test_schedule_xconf_update([&cnt](const operation::result&) { ++cnt; });
    manager->test_schedule_xconf_update([&cnt](const operation::result&) { ++cnt; });
    CHECK(cnt == 0);
    CHECK(xconf->get_count() == 1);

    manager->run();
    CHECK(cnt == 3);

    CHECK(xconf->get_count() == 1);
    manager->test_schedule_xconf_update([&cnt](const operation::result&) { ++cnt; });
    manager->test_schedule_xconf_update([&cnt](const operation::result&) { ++cnt; });
    CHECK(xconf->get_count() == 2);
    CHECK(cnt == 3);

    manager->run();
    CHECK(cnt == 5);
    CHECK(xconf->get_count() == 2);
}

TEST_CASE("service_manager/updating_services_and_tokens", "")
{
    static const size_t ENV_COUNT = 3;

    std::shared_ptr<service_data> test_svc;
    bool cb_called = false;
    auto ctx = boost::make_shared<task_context>();
    auto manager = service_manager_mock::create_manager();

    service_properties draft;
    draft.name = "test_svc";
    draft.owner_prefix = "test_";
    draft.owner_id = "uid";

    // Request service creation.
    manager->create_service(
        ctx,
        draft,
        [&test_svc, &cb_called](const operation::result& res, std::shared_ptr<service_data> svc) {
            CHECK(res);
            test_svc = svc;
            cb_called = true;
        });
    manager->run();
    // Check that service was created successfully.
    CHECK(cb_called);
    REQUIRE(test_svc);
    CHECK(test_svc->properties.name == draft.name);
    REQUIRE(test_svc->send_tokens.size() == ENV_COUNT);
    REQUIRE(test_svc->listen_tokens.size() == ENV_COUNT);
    auto stoken = test_svc->send_tokens.begin()->first.token;
    auto ltoken = test_svc->listen_tokens.begin()->first.token;
    // Check that xconf received changes.
    auto conf = manager->get_xconf()->test_list();
    CHECK(conf->max_revision == 7);
    CHECK(conf->items.size() == 7);
    CHECK(conf->items[0].type == config_type::SERVICE);
    CHECK(conf->items[1].type == config_type::SEND_TOKEN);
    CHECK(conf->items[2].type == config_type::LISTEN_TOKEN);
    // Check that no changes are applied before xconf update.
    CHECK(!manager->find_service_by_send_token(stoken));
    CHECK(!manager->find_service_by_listen_token(ltoken));
    // Enforce xconf update by getting service by owner.
    auto res = manager->test_find_services_by_owner("test_uid");
    CHECK(res.first);
    REQUIRE(res.second.size() == 1);
    auto svc = res.second.front();
    // Check that now service is found by tokens.
    CHECK(svc == manager->find_service_by_send_token(stoken));
    CHECK(svc == manager->find_service_by_listen_token(ltoken));

    // Request creating the same service.
    cb_called = false;
    manager->create_service(
        ctx, draft, [&cb_called](const operation::result& res, std::shared_ptr<service_data> svc) {
            // Check that duplicates are not allowed.
            CHECK(res.error_reason == "service already exists");
            CHECK(!svc);
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    // Check that service is not passport and request it's change to passport.
    CHECK(!test_svc->properties.is_passport);
    draft.is_passport = true;
    cb_called = false;
    manager->update_service_properties(ctx, draft, [&cb_called](const operation::result& res) {
        CHECK(res);
        cb_called = true;
    });
    manager->run();
    CHECK(cb_called);
    // Enforce xconf update by getting service by owner.
    auto res2 = manager->test_find_services_by_owner("test_uid");
    CHECK(res2.first);
    REQUIRE(res2.second.size() == 1);
    svc = res2.second.front();
    // Check that changes are applied.
    CHECK(svc->properties.is_passport);

    // Create new send and listen tokens.
    send_token_properties st;
    std::tie(st.service, st.name, st.revoked) = std::make_tuple(draft.name, "new_token", false);
    listen_token_properties lt;
    std::tie(lt.service, lt.name, lt.revoked) = std::make_tuple(draft.name, "new_token", false);
    string new_st, new_lt;
    cb_called = false;
    manager->update_send_token(
        ctx,
        "sandbox",
        st,
        draft.owner(),
        [&cb_called, &new_st](const operation::result& res, const string& token) {
            CHECK(res.error_reason == "");
            cb_called = true;
            new_st = token;
        });
    manager->run();
    CHECK(cb_called);
    cb_called = false;
    manager->update_listen_token(
        ctx,
        "sandbox",
        lt,
        draft.owner(),
        [&cb_called, &new_lt](const operation::result& res, const string& token) {
            CHECK(res.error_reason == "");
            cb_called = true;
            new_lt = token;
        });
    manager->run();
    CHECK(cb_called);
    // Check wrong owner.
    cb_called = false;
    manager->update_send_token(
        ctx,
        "sandbox",
        st,
        draft.owner() + "_",
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res.error_reason == "service owner mismatch");
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    cb_called = false;
    manager->update_listen_token(
        ctx,
        "sandbox",
        lt,
        draft.owner() + "_",
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res.error_reason == "service owner mismatch");
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    // Check wrong service.
    st.service = "123";
    lt.service = "123";
    cb_called = false;
    manager->update_send_token(
        ctx,
        "sandbox",
        st,
        draft.owner(),
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res.error_reason == "service not found");
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    cb_called = false;
    manager->update_listen_token(
        ctx,
        "sandbox",
        lt,
        draft.owner(),
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res.error_reason == "service not found");
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    // Enforce xconf update by getting service by owner.
    auto res3 = manager->test_find_services_by_owner("test_uid");
    CHECK(res3.first);
    REQUIRE(res3.second.size() == 1);
    svc = res3.second.front();
    manager->get_xconf()->test_do_update();
    manager->run();
    // Check that now service is found by tokens.
    CHECK(svc == manager->find_service_by_send_token(stoken));
    CHECK(svc == manager->find_service_by_listen_token(ltoken));
    CHECK(svc == manager->find_service_by_send_token(new_st));
    CHECK(svc == manager->find_service_by_listen_token(new_lt));
    // Revoke new tokens
    std::tie(st.service, st.revoked) = std::make_tuple(draft.name, true);
    std::tie(lt.service, lt.revoked) = std::make_tuple(draft.name, true);
    cb_called = false;
    manager->update_send_token(
        ctx,
        "sandbox",
        st,
        draft.owner(),
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res);
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    cb_called = false;
    manager->update_listen_token(
        ctx,
        "sandbox",
        lt,
        draft.owner(),
        [&cb_called](const operation::result& res, const string& /*token*/) {
            CHECK(res);
            cb_called = true;
        });
    manager->run();
    CHECK(cb_called);
    // Enforce xconf update by getting service by owner.
    auto res4 = manager->test_find_services_by_owner("test_uid");
    CHECK(res4.first);
    REQUIRE(res4.second.size() == 1);
    svc = res4.second.front();
    // Check that new tokens are revoked and old onew are intact.
    CHECK(svc == manager->find_service_by_send_token(stoken));
    CHECK(svc == manager->find_service_by_listen_token(ltoken));
    CHECK(!manager->find_service_by_send_token(new_st));
    CHECK(!manager->find_service_by_listen_token(new_lt));

    // Revoke service
    draft.revoked = true;
    cb_called = false;
    manager->update_service_properties(ctx, draft, [&cb_called](const operation::result& res) {
        CHECK(res);
        cb_called = true;
    });
    manager->run();
    CHECK(cb_called);
    // Enforce xconf update by getting service by owner.
    auto res5 = manager->test_find_services_by_owner("test_uid");
    CHECK(res5.first);
    REQUIRE(res5.second.size() == 1);
    svc = res5.second.front();
    // Check that revoked service can't be found by token
    CHECK(!manager->find_service_by_send_token(stoken));
    CHECK(!manager->find_service_by_listen_token(ltoken));
    CHECK(!manager->find_service_by_send_token(new_st));
    CHECK(!manager->find_service_by_listen_token(new_lt));
}

TEST_CASE("service_manager/mobile_apps", "")
{
    auto manager = service_manager_mock::create_manager();
    item_builder b;
    REQUIRE(!manager->find_app(FCM, "app"));
    REQUIRE(!manager->find_app(APNS, "app"));
    // Add service-owned app.
    manager->add_conf(
        { b.name("fcm:app")
              .token("")
              .owner("xivaservice:svc3")
              .data(application_config{ "", "svc3", FCM, "app", 0, "FAKE", 0, "", {}, 0 })
              .build() });
    REQUIRE(manager->find_app(FCM, "app"));
    CHECK(manager->find_app(FCM, "app")->app_name == "app");
    CHECK(manager->find_app(FCM, "app")->platform == FCM);
    CHECK(manager->find_app("gcm", "app")->platform == FCM); // gcm_compatibility
    CHECK(manager->find_service_by_name("svc3")->apps.count(std::tie("fcm", "app")) == 1);
    REQUIRE(!manager->find_app(APNS, "app"));
    // Add service-owned app. // gcm_compatibility
    manager->add_conf(
        { b.name("gcm:app2")
              .token("")
              .owner("xivaservice:svc3")
              .data( // gcm_compatibility
                  application_config{ "", "svc3", "gcm", "app2", 0, "FAKE", 0, "", {}, 0 })
              .build() });                                       // gcm_compatibility
    REQUIRE(manager->find_app("fcm", "app2"));                   // gcm_compatibility
    CHECK(manager->find_app("fcm", "app2")->app_name == "app2"); // gcm_compatibility
    CHECK(manager->find_app("fcm", "app2")->platform == "fcm");  // gcm_compatibility
    CHECK(manager->find_app("gcm", "app2")->platform == "fcm");  // gcm_compatibility
    CHECK(
        manager->find_service_by_name("svc3")->apps.count(std::tie("fcm", "app2")) ==
        1);                                    // gcm_compatibility
    REQUIRE(manager->find_app("gcm", "app2")); // gcm_compatibility
    REQUIRE(!manager->find_app(APNS, "app2"));
    // Add user-owned app.
    manager->add_conf(
        { b.name("apns:app2")
              .token("")
              .owner("user")
              .data(application_config{ "", "svc3", APNS, "app", 0, "FAKE", 0, "", {}, 0 })
              .build() });
    REQUIRE(manager->find_app(FCM, "app"));
    REQUIRE(manager->find_app(APNS, "app"));
    CHECK(manager->find_app(APNS, "app")->app_name == "app");
    CHECK(manager->find_app(APNS, "app")->platform == APNS);
    // Remove apps by rewriting them with empty secret.
    manager->add_conf({ b.name("fcm:app")
                            .token("")
                            .owner("xivaservice:svc3")
                            .data(application_config{ "", "svc3", FCM, "app", 0, "", 0, "", {}, 0 })
                            .build() });
    manager->add_conf(
        { b.name("apns:app2")
              .token("")
              .owner("user")
              .data(application_config{ "", "svc3", APNS, "app", 0, "", 0, "", {}, 0 })
              .build() });
    REQUIRE(manager->find_app(APNS, "app"));
    REQUIRE(manager->find_app(FCM, "app"));
    CHECK(manager->find_app(FCM, "app")->secret_key == "");
    CHECK(manager->find_app(APNS, "app")->secret_key == "");
    REQUIRE(manager->find_service_by_name("svc3")->apps.count(std::tie(FCM, "app")) == 1);
    CHECK(manager->find_service_by_name("svc3")->apps.at(std::tie(FCM, "app")).secret_key == "");
}

TEST_CASE("service_manager/update_service_properties")
{
    auto manager = service_manager_mock::create_manager();
    item_builder b;
    bool callback_called = false;

    manager->add_conf({ b.svc_data("apple", "buratino").build() });
    auto ctx = boost::make_shared<task_context>();
    auto steal_apple = [](auto props) {
        props.owner_id = "nekto";
        return props;
    };
    auto update_cb = [&](auto&& res) {
        callback_called = true;
        REQUIRE(res.error_reason == "");
    };

    manager->update_service_properties(ctx, "apple", steal_apple, update_cb);
    manager->run();
    REQUIRE(callback_called);

    // Changes submitted to xconf, but not yet seen by the manager.
    auto not_yet_updated_service = manager->find_service_by_name("apple");
    REQUIRE(not_yet_updated_service->properties.owner_id == "buratino");

    // Trigger an xconf update, so the manager would see the changes.
    manager->get_all_services(ctx, [](auto&&, auto&&) {});
    manager->run();
    auto updated_service = manager->find_service_by_name("apple");
    REQUIRE(updated_service->properties.owner_id == "nekto");
}
