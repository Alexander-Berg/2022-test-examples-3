#pragma once

#include <internal/config_reflection.h>
#include <internal/logger.h>
#include <internal/mail/config.h>
#include <internal/db/pools/connection_pool.h>
#include <internal/db/adaptors/meta_adaptor.h>
#include <internal/db/adaptors/peers_adaptor.h>
#include <internal/db/adaptors/shard_adaptor.h>
#include <internal/poller/ishards_provider.h>
#include <user_journal/journal.h>

#include <yamail/data/serialization/ptree.h>
#include <ymod_httpclient/call.h>

#include <gmock/gmock.h>

namespace ymod_httpclient {

static inline bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

static inline bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

static inline bool operator ==(const options& lhs, const options& rhs) {
    return lhs.log_post_body == rhs.log_post_body
        && lhs.log_headers == rhs.log_headers
        && lhs.reuse_connection == rhs.reuse_connection
        && lhs.timeouts == rhs.timeouts;
}

static inline bool operator ==(const response& lhs, const response& rhs) {
    return lhs.status == rhs.status
        && lhs.headers == rhs.headers
        && lhs.body == rhs.body
        && lhs.reason == rhs.reason;
}

static inline std::ostream& operator <<(std::ostream& stream, const request::method_t value) {
    switch (value) {
        case request::method_t::GET:
            return stream << "GET";
        case request::method_t::POST:
            return stream << "POST";
        case request::method_t::PUT:
            return stream << "PUT";
        case request::method_t::HEAD:
            return stream << "HEAD";
        case request::method_t::DELETE:
            return stream << "DELETE";
    }
    return stream << static_cast<request::method_t>(value);
}

static inline std::ostream& operator <<(std::ostream& stream, const request& value) {
    return stream << "ymod_httpclient::request {"
                  << value.method << ", "
                  << '"' << value.url << "\", "
                  << '"' << value.headers << "\", "
                  << static_cast<const void *>(value.body.get())
                  << "}";
}

static inline std::ostream& operator <<(std::ostream& stream, const timeouts& value) {
    return stream << "ymod_httpclient::timeouts {"
        << value.connect.count() << ", "
        << value.total.count()
        << "}";
}

static inline std::ostream& operator <<(std::ostream& stream, const options& value) {
    using namespace std::string_literals;
    return stream << "ymod_httpclient::options {"
        << (value.log_post_body ? std::to_string(*value.log_post_body) : "none"s) << ", "
        << (value.log_headers ? std::to_string(*value.log_headers) : "none"s) << ", "
        << (value.reuse_connection ? std::to_string(*value.reuse_connection) : "none"s) << ", "
        << value.timeouts
        << "}";
}

static inline std::ostream& operator <<(std::ostream& stream, const response& value) {
    using namespace std::string_literals;
    return stream << "ymod_httpclient::response {"
        << value.status
        << ", \"" << value.body
        << "\", \"" << value.reason
        << "\"}";
}

} // namespace ymod_httpclient

namespace sharpei {

inline void addHostlist(yplatform::ptree& tree, const std::vector<db::Host>& hostlist) {
    for (const auto& host : hostlist) {
        yplatform::ptree tmp = yamail::data::serialization::toPtree(host);
        tree.add_child("meta_connection.endpoint_provider.hostlist", tmp);
    }
}

inline yplatform::ptree makeTestConfigAsPtree() {
    yplatform::ptree tree;

    tree.add("meta_poller.base.interval_sec", 1);
    tree.add("meta_poller.base.coroutine_stack_size", 1024 * 1024);
    tree.add("meta_poller.enabled", true);
    tree.add("shards_poller.base.interval_sec", 1);
    tree.add("shards_poller.base.coroutine_stack_size", 1024 * 1024);
    tree.add("shards_poller.meta_max_connections", 2);

    tree.add("meta_connection.db.adaptor.conn_info.host", "localhost");
    tree.add("meta_connection.db.adaptor.conn_info.port", 5432);
    tree.add("meta_connection.db.adaptor.conn_info.dbname", "sharddb");
    tree.add("meta_connection.db.adaptor.conn_info.auth_info.user", "sharpei");
    tree.add("meta_connection.endpoint_provider.use_meta_cache_based_provider_in_meta_pool", true);
    tree.add("meta_connection.endpoint_provider.alive_hosts_threshold", 0);
    tree.add("meta_connection.db.pool.connect_timeout_ms", 100);
    tree.add("meta_connection.db.pool.queue_timeout_ms", 300);
    tree.add("meta_connection.db.pool.idle_timeout_ms", 60000);
    tree.add("meta_connection.db.pool.max_connections", 6);
    tree.add("meta_connection.db.pool.async_resolve", true);
    tree.add("meta_connection.db.pool.ipv6_only", true);
    tree.add("meta_connection.db.pool.dns.cache_ttl_sec", 120);
    tree.add("meta_connection.db.adaptor.request_timeout_ms", 100);

    addHostlist(tree, {{"localhost", db::DC::sas}});

    tree.add("shard_connection.adaptor.auth_info.user", "sharpei");

    tree.add("shard_connection.pool.connect_timeout_ms", 1000);
    tree.add("shard_connection.pool.queue_timeout_ms", 1000);
    tree.add("shard_connection.pool.idle_timeout_ms", 60000);
    tree.add("shard_connection.pool.max_connections", 1);
    tree.add("shard_connection.pool.async_resolve", true);
    tree.add("shard_connection.pool.ipv6_only", true);
    tree.add("shard_connection.pool.dns.cache_ttl_sec", 120);
    tree.add("shard_connection.adaptor.request_timeout_ms", 1000);
    tree.add("shard_connection.workers", 4);

    tree.add("peers_connection.adaptor.conn_info.port", 5432);
    tree.add("peers_connection.adaptor.conn_info.auth_info.user", "sharpei");
    tree.add("peers_connection.adaptor.conn_info.dbname", "sharddb");
    tree.add("peers_connection.pool.connect_timeout_ms", 1000);
    tree.add("peers_connection.pool.queue_timeout_ms", 1000);
    tree.add("peers_connection.pool.idle_timeout_ms", 60000);
    tree.add("peers_connection.pool.max_connections", 1);
    tree.add("peers_connection.pool.async_resolve", true);
    tree.add("peers_connection.pool.ipv6_only", true);
    tree.add("peers_connection.pool.dns.cache_ttl_sec", 120);
    tree.add("peers_connection.adaptor.request_timeout_ms", 1000);

    tree.add("cache.history_capacity", 3);
    tree.add("cache.errors_limit", 2);

    tree.add("pa.log", "var/log/sharpei/profiler.log");

    tree.add("blackbox.location", "http://blackbox");
    tree.add("blackbox.retries", 7);
    tree.add("blackbox.http.module", "http_client");
    tree.add("blackbox.http.options.reuse_connection", true);
    tree.add("blackbox.http.options.timeouts.connect_ms", 1);
    tree.add("blackbox.http.options.timeouts.total_ms", 1);

    tree.add("tvm_module", "tvm");

    tree.add("coroutine_stack_size", 1024 * 1024);

    return tree;
}

inline ConfigPtr makeTestConfig() {
    auto tree = makeTestConfigAsPtree();
    return makeConfig(tree);
}

namespace mail {

inline ConfigPtr makeTestConfig() {
    yplatform::ptree tree;

    tree.add("registration.get_alive_master_attempts", 10);
    tree.add("registration.mdb_timeout_ms", 1000);
    tree.add("registration.sharddb_timeout_ms", 1000);
    tree.add("registration.max_commit_tries", 3);
    tree.add("registration.max_rollback_tries", 3);
    tree.add("registration.retry_delay_base_s", 0);
    tree.add("registration.enable_welcome_letters", true);
    tree.add("registration.create_base_filters", true);

    tree.add("user_journal.table_name", "users_history");
    tree.add("user_journal.write_chunk_size", 100);
    tree.add("user_journal.write_poll_timeout_ms", 500);
    tree.add("user_journal.tskv", "var/log/sharpei/user_journal.tskv");
    tree.add("user_journal.locale", "");
    tree.add("user_journal.tskv_format", "mail-user-journal-tskv-log");

    return makeConfig(sharpei::makeTestConfig(), tree);
}

} // namespace mail

class MockProfiler: public Profiler {
    MOCK_METHOD(void, write, (const std::string&, std::chrono::milliseconds, const std::string&), (override));
};

namespace db {

class ApqConnectionPoolMock : public ApqConnectionPool {
public:
    using ApqConnectionPool::ApqConnectionPool;

    MOCK_METHOD(void, async_request, (const apq::query& query, apq::connection_pool::request_handler_t handler, apq::result_format rt,
                      apq::time_traits::duration_type tm), (override));

    MOCK_METHOD(void, async_update, (const apq::query& query, apq::connection_pool::update_handler_t handler,
                      apq::time_traits::duration_type tm), (override));
};

class MockConnectionPool: public sharpei::db::IConnectionPool {
public:
    MOCK_METHOD(ConnectionPoolPtr, get, (const ConnectionInfo&), (override));
    MOCK_METHOD(Stats, stats, (), (override));
};

template <class UserIdValue>
struct MockedMetaAdaptor : public MetaAdaptor<UserIdValue> {
    using UserId = typename MetaAdaptor<UserIdValue>::UserId;
    using GetShardIdHandler = BaseMetaAdaptor::GetShardIdHandler;
    using GetShardHandler = BaseMetaAdaptor::GetShardHandler;
    using GetAllShardsHandler = BaseMetaAdaptor::GetAllShardsHandler;
    using GetRegDataHandler = BaseMetaAdaptor::GetRegDataHandler;
    using GetUserDataHandler = BaseMetaAdaptor::GetUserDataHandler;
    using GetMasterHandler = BaseMetaAdaptor::GetMasterHandler;
    using ErrorHandler = BaseMetaAdaptor::ErrorHandler;
    using FinishHandler = BaseMetaAdaptor::FinishHandler;

    MOCK_CONST_METHOD3_T(getShard, void (Shard::Id, const GetShardHandler&, const ErrorHandler&));
    MOCK_CONST_METHOD2_T(getAllShards, void (const GetAllShardsHandler&, const ErrorHandler&));
    MOCK_CONST_METHOD3_T(getUserRegData, void(const UserId&, GetRegDataHandler, ErrorHandler));
    MOCK_CONST_METHOD2_T(ping, void (const FinishHandler&, const ErrorHandler&));
    MOCK_CONST_METHOD4_T(getUserData, void (const UserId& uid, const GetUserDataHandler&, const ErrorHandler&, QueryType));
    MOCK_CONST_METHOD4_T(getDeletedUserData, void (const UserId& uid, const GetUserDataHandler&, const ErrorHandler&, QueryType));
    MOCK_CONST_METHOD2_T(getMaster, void (const GetMasterHandler&, const ErrorHandler&));
    MOCK_CONST_METHOD2_T(getRegData, void(GetRegDataHandler, ErrorHandler));
    MOCK_CONST_METHOD3_T(getDomainShardId, void (const DomainId, GetShardIdHandler, ErrorHandler));
    MOCK_CONST_METHOD3_T(getOrganizationShardId, void (const OrgId, GetShardIdHandler, ErrorHandler));
};

template <class UserIdValue>
struct MockedPeersAdaptor {
    MOCK_CONST_METHOD3_T(createUser, void (const std::string&, const CreateUserParams<UserIdValue>&, std::function<void (ExplainedError, Shard::Id)>));
    MOCK_CONST_METHOD3_T(updateUser, void (const std::string&, const UpdateUserParams<UserIdValue>&, std::function<void (ExplainedError)>));
};

template <class UserIdValue>
struct MockedPeersAdaptorWrapper {
    std::shared_ptr<MockedPeersAdaptor<UserIdValue>> impl;

    MockedPeersAdaptorWrapper() : impl(std::make_shared<MockedPeersAdaptor<UserIdValue>>()) {}

    template <class Handler>
    void createUser(const std::string& master, const CreateUserParams<UserIdValue>& params, Handler&& handler) const {
        return impl->createUser(master, params, std::forward<Handler>(handler));
    }

    template <class Handler>
    void updateUser(const std::string& master, const UpdateUserParams<UserIdValue>& params, Handler&& handler) const {
        return impl->updateUser(master, params, std::forward<Handler>(handler));
    }
};

} // namespace db

using namespace ::user_journal;

struct MapperMock : public Mapper {
    MOCK_METHOD(void, mapValue, (const Operation & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Target & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::string & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Date & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (bool v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (int v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (size_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (std::time_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::vector<std::string> & v, const std::string & name) , (const, override));
};

struct RequestParametersMock : public parameters::RequestParameters {
    MOCK_METHOD(void, map, (const Mapper&), (const, override));
    MOCK_METHOD(const std::string&, uid, (), (const, override));
};

struct WriterMock : public Writer {
    MOCK_METHOD(void, write, (const std::string&, const Entry&), (const, override));
};

struct HttpClientMock : ymod_httpclient::simple_call {
    using Response = ymod_httpclient::response;
    using Request = ymod_httpclient::request;
    using Options = ymod_httpclient::options;

    MOCK_METHOD(Response, run, (task_context_ptr, Request), (override));
    MOCK_METHOD(Response, run, (task_context_ptr, Request, const Options&), (override));

    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, const Options&, callback_type), (override));
};

struct ClusterClientMock : ymod_httpclient::cluster_call {
    using Request = ymod_httpclient::request;
    using Options = ymod_httpclient::cluster_client_options;

    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, callback_type callback), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr ctx, Request req, const Options& options, callback_type callback), (override));
};


inline ymod_httpclient::timeouts makeHttpTimeouts() {
    ymod_httpclient::timeouts result;
    result.connect = std::chrono::seconds(1);
    result.total = std::chrono::seconds(2);
    return result;
}

inline ymod_httpclient::options makeHttpOptions() {
    ymod_httpclient::options result;
    result.timeouts = makeHttpTimeouts();
    return result;
}

inline ymod_httpclient::cluster_client_options makeClusterClientOptions() {
    ymod_httpclient::cluster_client_options result;
    result.timeouts = makeHttpTimeouts();
    return result;
}

struct ShardAdaptorMock : db::ShardAdaptor {
    MOCK_METHOD(void, resetHostCache, (const ShardWithoutRoles& shard, const FinishHandler& handler,
                                       const ErrorHandler& errorHandler), (override));
};

struct ShardsProviderMock : poller::IShardsProvider {
    MOCK_METHOD(expected<std::vector<ShardWithoutRoles>>, getAllShards, (const TaskContextPtr&), (const, override));
};

} // namespace sharpei
