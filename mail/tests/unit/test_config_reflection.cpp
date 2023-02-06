#include <gtest/gtest.h>

#include "generic_operators.hpp"

#include <src/config_reflection.hpp>

#include <yplatform/ptree.h>
#include <yamail/data/deserialization/ptree.h>
#include <yamail/data/serialization/yajl.h>

namespace ymod_httpclient {

using collie::tests::operator==;

static inline bool operator==(const timeouts& left, const timeouts& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace ozo {

using collie::tests::operator==;

static inline bool operator==(const connection_pool_config& left, const connection_pool_config& right) {
    return boost::fusion::operator==(left, right);
}

static inline bool operator==(const connection_pool_timeouts& left, const connection_pool_timeouts& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace pgg {

using collie::tests::operator==;

}

namespace sharpei::client::http {

using collie::tests::operator==;

}

namespace sharpei::client {

static inline bool operator==(const Settings& left, const Settings& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace collie {

namespace server {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace services {

namespace sharpei {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace db {

static inline bool operator==(const Timeouts& left, const Timeouts& right) {
    return boost::fusion::operator==(left, right);
}

namespace contacts {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace events_queue {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace directory {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace staff {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace ml {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace recognizer {

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

static inline bool operator==(const Config& left, const Config& right) {
    return boost::fusion::operator==(left, right);
}

}

namespace {

using namespace testing;

using collie::Config;
using yamail::data::deserialization::fromPtree;

struct TestFromPtreeForConfig : Test {
    yplatform::ptree data;
    Config expected;

    TestFromPtreeForConfig() {
        data.put("profiler_log_name", "var/log/collie/profiler.bin");
        data.put("event_queue_module", "collie_directory_event_queue");
        data.put("tvm_module", "tvm");
        data.put("server.web_server_module", "web_server");
        data.put("server.coroutine_stack_size", "1048576");
        data.put("services.http_client_module", "http_client");
        data.put("services.recognizer.language_dict", "/usr/share/recognizer/queryrec.dict");
        data.put("services.recognizer.language_weights", "/usr/share/recognizer/queryrec.weights");
        data.put("services.recognizer.encoding_dict", "/usr/share/recognizer/dict.dict");
        data.put("services.db.contacts.credentials.user", "collie");
        data.put("services.db.contacts.credentials.password", "qwerty");
        data.put("services.db.contacts.max_total_pools_capacity", "1000");
        data.put("services.db.contacts.connection_pool.capacity", "100");
        data.put("services.db.contacts.connection_pool.queue_capacity", "200");
        data.put("services.db.contacts.connection_pool.idle_timeout_ms", "400");
        data.put("services.db.contacts.timeouts.connection_pool.connect_ms", "500");
        data.put("services.db.contacts.timeouts.connection_pool.queue_ms", "600");
        data.put("services.db.contacts.timeouts.request_ms", "700");
        data.put("services.db.contacts.max_retries_number", "2");
        data.put("services.db.contacts.query_conf", "contacts_query_conf");
        data.put("services.db.contacts.org_sharpei.connect_timeout_ms", "300");
        data.put("services.db.contacts.org_sharpei.client.address.host", "http://org_sharpei.mail.yandex.net");
        data.put("services.db.contacts.org_sharpei.client.address.port", "4000");
        data.put("services.db.contacts.org_sharpei.client.retries", "3");
        data.put("services.db.contacts.org_sharpei.client.timeout_ms", "1000");
        data.put("services.db.contacts.org_sharpei.client.keep_alive", "false");
        data.put("services.db.contacts.user_sharpei.connect_timeout_ms", "301");
        data.put("services.db.contacts.user_sharpei.client.address.host", "http://user_sharpei.mail.yandex.net");
        data.put("services.db.contacts.user_sharpei.client.address.port", "4001");
        data.put("services.db.contacts.user_sharpei.client.retries", "4");
        data.put("services.db.contacts.user_sharpei.client.timeout_ms", "1001");
        data.put("services.db.contacts.user_sharpei.client.keep_alive", "true");
        data.put("services.db.events_queue.credentials.user", "collie");
        data.put("services.db.events_queue.credentials.password", "qwerty");
        data.put("services.db.events_queue.max_total_pools_capacity", "3");
        data.put("services.db.events_queue.connection_pool.capacity", "1");
        data.put("services.db.events_queue.connection_pool.queue_capacity", "200");
        data.put("services.db.events_queue.connection_pool.idle_timeout_ms", "400");
        data.put("services.db.events_queue.timeouts.connection_pool.connect_ms", "500");
        data.put("services.db.events_queue.timeouts.connection_pool.queue_ms", "600");
        data.put("services.db.events_queue.timeouts.request_ms", "700");
        data.put("services.db.events_queue.max_retries_number", "2");
        data.put("services.db.events_queue.query_conf", "events_queue_query_conf");
        data.put("services.db.events_queue.sharpei.connect_timeout_ms", "300");
        data.put("services.db.events_queue.sharpei.client.address.host", "http://collie-sharpei-testing.mail.yandex.net");
        data.put("services.db.events_queue.sharpei.client.address.port", "80");
        data.put("services.db.events_queue.sharpei.client.retries", "3");
        data.put("services.db.events_queue.sharpei.client.timeout_ms", "1000");
        data.put("services.db.events_queue.sharpei.client.keep_alive", "false");
        data.put("services.directory.location", "https://api-internal-test.directory.ws.yandex.net");
        data.put("services.directory.retries", "2");
        data.put("services.directory.instant_messengers", "icq,skype");
        data.put("services.directory.social_profiles", "facebook,twitter");
        data.put("services.directory.target_service_name", "directory");
        data.put("services.directory.http_options.timeouts.connect_ms", "1000");
        data.put("services.directory.http_options.timeouts.total_ms", "10000");
        data.put("services.staff.location", "http://staff-api.test.yandex-team.ru");
        data.put("services.staff.limit", "500");
        data.put("services.staff.page_max_count", "1000");
        data.put("services.staff.retries", "1");
        data.put("services.staff.instant_messengers", "skype,telegram");
        data.put("services.staff.social_profiles", "moi_krug,vkontakte");
        data.put("services.staff.target_service_name", "staff");
        data.put("services.staff.http_client_module", "staff_http_client");
        data.put("services.staff.http_options.timeouts.connect_ms", "1000");
        data.put("services.staff.http_options.timeouts.total_ms", "10000");
        data.put("services.ml.location", "http://ml-test.yandex-team.ru");
        data.put("services.ml.retries", "1");
        data.put("services.ml.target_service_name", "ml");
        data.put("services.ml.http_client_module", "ml_http_client");
        data.put("services.ml.http_options.timeouts.connect_ms", "1000");
        data.put("services.ml.http_options.timeouts.total_ms", "10000");
        data.put("ml_id", "1001");
        data.put("staff_id", "1002");

        expected.profilerLogName = "var/log/collie/profiler.bin";
        expected.eventQueueModule = "collie_directory_event_queue";
        expected.tvmModule = "tvm";
        expected.server.webServerModule = "web_server";
        expected.server.coroutineStackSize = 1048576;
        expected.services.httpClientModule = "http_client";
        expected.services.recognizer.languageDict = "/usr/share/recognizer/queryrec.dict";
        expected.services.recognizer.languageWeights = "/usr/share/recognizer/queryrec.weights";
        expected.services.recognizer.encodingDict = "/usr/share/recognizer/dict.dict";
        expected.services.db.contacts.credentials.user = "collie";
        expected.services.db.contacts.credentials.password = "qwerty";
        expected.services.db.contacts.maxTotalPoolsCapacity = 1000;
        expected.services.db.contacts.connectionPool.capacity = 100;
        expected.services.db.contacts.connectionPool.queue_capacity = 200;
        expected.services.db.contacts.connectionPool.idle_timeout = std::chrono::milliseconds(400);
        expected.services.db.contacts.timeouts.connectionPool.connect = std::chrono::milliseconds(500);
        expected.services.db.contacts.timeouts.connectionPool.queue = std::chrono::milliseconds(600);
        expected.services.db.contacts.timeouts.request = std::chrono::milliseconds(700);
        expected.services.db.contacts.maxRetriesNumber = 2;
        expected.services.db.contacts.queryConf = "contacts_query_conf";
        expected.services.db.contacts.orgSharpei.connectTimeout = std::chrono::milliseconds(300);
        expected.services.db.contacts.orgSharpei.client.sharpeiAddress.host = "http://org_sharpei.mail.yandex.net";
        expected.services.db.contacts.orgSharpei.client.sharpeiAddress.port = 4000;
        expected.services.db.contacts.orgSharpei.client.retries = 3;
        expected.services.db.contacts.orgSharpei.client.timeout = std::chrono::milliseconds(1000);
        expected.services.db.contacts.orgSharpei.client.keepAlive = false;
        expected.services.db.contacts.userSharpei.connectTimeout = std::chrono::milliseconds(301);
        expected.services.db.contacts.userSharpei.client.sharpeiAddress.host = "http://user_sharpei.mail.yandex.net";
        expected.services.db.contacts.userSharpei.client.sharpeiAddress.port = 4001;
        expected.services.db.contacts.userSharpei.client.retries = 4;
        expected.services.db.contacts.userSharpei.client.timeout = std::chrono::milliseconds(1001);
        expected.services.db.contacts.userSharpei.client.keepAlive = true;
        expected.services.db.eventsQueue.credentials.user = "collie";
        expected.services.db.eventsQueue.credentials.password = "qwerty";
        expected.services.db.eventsQueue.maxTotalPoolsCapacity = 3;
        expected.services.db.eventsQueue.connectionPool.capacity = 1;
        expected.services.db.eventsQueue.connectionPool.queue_capacity = 200;
        expected.services.db.eventsQueue.connectionPool.idle_timeout = std::chrono::milliseconds(400);
        expected.services.db.eventsQueue.timeouts.connectionPool.connect = std::chrono::milliseconds(500);
        expected.services.db.eventsQueue.timeouts.connectionPool.queue = std::chrono::milliseconds(600);
        expected.services.db.eventsQueue.timeouts.request = std::chrono::milliseconds(700);
        expected.services.db.eventsQueue.maxRetriesNumber = 2;
        expected.services.db.eventsQueue.queryConf = "events_queue_query_conf";
        expected.services.db.eventsQueue.sharpei.connectTimeout = std::chrono::milliseconds(300);
        expected.services.db.eventsQueue.sharpei.client.sharpeiAddress.host = "http://collie-sharpei-testing.mail.yandex.net";
        expected.services.db.eventsQueue.sharpei.client.sharpeiAddress.port = 80;
        expected.services.db.eventsQueue.sharpei.client.retries = 3;
        expected.services.db.eventsQueue.sharpei.client.timeout = std::chrono::milliseconds(1000);
        expected.services.db.eventsQueue.sharpei.client.keepAlive = false;
        expected.services.directory.location = "https://api-internal-test.directory.ws.yandex.net";
        expected.services.directory.retries = 2;
        expected.services.directory.instantMessengers = "icq,skype";
        expected.services.directory.socialProfiles = "facebook,twitter";
        expected.services.directory.targetServiceName = "directory";
        expected.services.directory.httpOptions.timeouts.connect = std::chrono::milliseconds(1000);
        expected.services.directory.httpOptions.timeouts.total = std::chrono::milliseconds(10000);
        expected.services.staff.location = "http://staff-api.test.yandex-team.ru";
        expected.services.staff.limit = 500;
        expected.services.staff.pageMaxCount = 1000;
        expected.services.staff.retries = 1;
        expected.services.staff.instantMessengers = "skype,telegram";
        expected.services.staff.socialProfiles = "moi_krug,vkontakte";
        expected.services.staff.targetServiceName = "staff";
        expected.services.staff.httpClientModule = "staff_http_client";
        expected.services.staff.httpOptions.timeouts.connect = std::chrono::milliseconds(1000);
        expected.services.staff.httpOptions.timeouts.total = std::chrono::milliseconds(10000);
        expected.services.ml.location = "http://ml-test.yandex-team.ru";
        expected.services.ml.retries = 1;
        expected.services.ml.targetServiceName = "ml";
        expected.services.ml.httpClientModule = "ml_http_client";
        expected.services.ml.httpOptions.timeouts.connect = std::chrono::milliseconds(1000);
        expected.services.ml.httpOptions.timeouts.total = std::chrono::milliseconds(10000);
        expected.mlId = 1001;
        expected.staffId = 1002;
    }
};

TEST_F(TestFromPtreeForConfig, for_config_ptree_should_return_config) {
    EXPECT_EQ(fromPtree<Config>(data), expected);
}

}
