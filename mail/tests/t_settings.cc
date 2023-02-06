#include <ymod_tvm/settings.h>
#include <yplatform/application/config/yaml_to_ptree.h>
#include <catch.hpp>

using ymod_tvm::tvm2::settings;

TEST_CASE("settings/config_ok", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: blackbox
    - name: test
      id: 123
      host: yandex.ru
blackbox_environments:
    - blackbox
    - blackbox-test
tvm_secret: test_secret
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_NOTHROW(settings(conf));
}

TEST_CASE("settings/config_alias", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: blackbox
    - name: test
      id: 123
blackbox_environments:
    - blackbox
    - blackbox-test
tvm_secret: test_secret
)";

    auto conf_alias_str = R"(
my_tvm_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
destinations:
    - name: blackbox
    - name: test
      id: 123
blackbox_environments:
    - blackbox
    - blackbox-test
secret: test_secret
)";

    yplatform::ptree conf, conf_alias;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    utils::config::yaml_to_ptree::convert_str(conf_alias_str, conf_alias);
    settings original(conf), alias(conf_alias);
    REQUIRE(original.my_tvm_id == alias.my_tvm_id);
    REQUIRE(original.target_services == alias.target_services);
    REQUIRE(original.target_services_by_id == alias.target_services_by_id);
    REQUIRE(original.tvm_secret == alias.tvm_secret);
}

TEST_CASE("settings/config_mappings", "[settings]")
{
    auto conf_str = R"(
my_tvm_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
destinations:
    - name: blackbox
      host: bb.ya.ru
    - name: test
      host: ya.ru
      id: 124
blackbox_environments:
    - blackbox
    - blackbox-test
secret: test_secret
)";

    auto conf_mappings_str = R"(
my_tvm_id: self
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
destinations: [blackbox, test]
blackbox_environments:
    - blackbox
    - blackbox-test
secret: test_secret
mappings:
    self: { id: 123 }
    test: { id: 124, host: ya.ru }
    blackbox: { host: bb.ya.ru }
)";

    yplatform::ptree conf, conf_mappings;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    utils::config::yaml_to_ptree::convert_str(conf_mappings_str, conf_mappings);
    settings original(conf), mappings(conf_mappings);
    REQUIRE(original.my_tvm_id == mappings.my_tvm_id);
    REQUIRE(original.target_services == mappings.target_services);
    REQUIRE(original.target_services_by_id == mappings.target_services_by_id);
    REQUIRE(original.tvm_service_by_host == mappings.tvm_service_by_host);
}

TEST_CASE("settings/config_multiple_hosts", "[settings]")
{
    auto conf_str = R"(
my_tvm_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
destinations:
    - name: blackbox
      host: [bb.ya.ru, bb2.ya.ru]
    - name: test
      hosts: [ya.ru, ya2.ru]
      id: 124
blackbox_environments:
    - blackbox
    - blackbox-test
secret: test_secret
)";

    auto conf_mappings_str = R"(
my_tvm_id: self
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
destinations: [blackbox, test]
blackbox_environments:
    - blackbox
    - blackbox-test
secret: test_secret
mappings:
    self: { id: 123 }
    test: { id: 124, host: [ya.ru, ya2.ru] }
    blackbox: { hosts: [bb.ya.ru, bb2.ya.ru] }
)";

    yplatform::ptree conf, conf_mappings;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    utils::config::yaml_to_ptree::convert_str(conf_mappings_str, conf_mappings);
    settings original(conf), mappings(conf_mappings);
    REQUIRE(original.my_tvm_id == mappings.my_tvm_id);
    REQUIRE(original.target_services == mappings.target_services);
    REQUIRE(original.target_services_by_id == mappings.target_services_by_id);
    REQUIRE(original.tvm_service_by_host == mappings.tvm_service_by_host);
    REQUIRE(original.tvm_service_by_host.size() == 4);
}

TEST_CASE("settings/missing_tvm_id", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: blackboxx
    - name: test
      id: 456
)";
    auto conf_str_mappings = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: test
      id: 456
mappings:
    blackboxx:
)";
    const auto error = "no id provided for service blackboxx"
                       " and it's not a known blackbox environment";

    yplatform::ptree conf, conf_mappings;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    utils::config::yaml_to_ptree::convert_str(conf_str_mappings, conf_mappings);
    REQUIRE_THROWS_WITH(settings(conf), error);
    REQUIRE_THROWS_WITH(settings(conf_mappings), error);
}

TEST_CASE("settings/duplicate_service", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: test
      id: 456
    - name: test
      id: 457
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_THROWS_WITH(settings(conf), "duplicate service name test");
}

TEST_CASE("settings/duplicate_tvm_id", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: test1
      id: 456
    - name: test2
      id: 456
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_THROWS_WITH(settings(conf), "duplicate service id 456");
}

TEST_CASE("settings/bad_blackbox_env", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: blackbox
    - name: test
      id: 123
blackbox_environments:
    - blackboxx
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_THROWS_WITH(settings(conf), "unknown blackbox environment blackboxx");
}

TEST_CASE("settings/duplicate_protected_host", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
target_services:
    - name: blackbox
      host: yandex.ru
    - name: test
      id: 123
      host: yandex.ru
blackbox_environments:
    - blackbox
    - blackbox-test
tvm_secret: test_secret
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_THROWS_WITH(settings(conf), "host yandex.ru was already configured");
}

TEST_CASE("settings/fetch_v1_ticket_true_causes_exception", "[settings]")
{
    auto conf_str = R"(
service_id: 123
keys_update_interval: 1:00:00
tickets_update_interval: 1:00:00
retry_interval: 1:00:00
tvm_host: localhost
fetch_v1_ticket: true
target_services:
    - name: blackbox
    - name: test
      id: 123
      host: yandex.ru
blackbox_environments:
    - blackbox
    - blackbox-test
tvm_secret: test_secret
)";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    REQUIRE_THROWS_WITH(
        settings(conf),
        "`true` specified for `fetch_v1_ticket`, but TVM 1.0 is no longer supported.");
}
