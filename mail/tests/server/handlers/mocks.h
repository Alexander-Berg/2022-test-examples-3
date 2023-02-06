#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/db/adaptors/meta_adaptor.h>
#include <internal/db/adaptors/peers_adaptor.h>
#include <internal/services/blackbox/blackbox.h>
#include <internal/expected.h>

#include <ymod_webserver/server.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::services;
using namespace sharpei::services::blackbox;
using namespace sharpei::db;

struct Streamer : yplatform::net::streamer_base {
    Streamer(std::string* responseBackup = nullptr) : responseBackup(responseBackup) {}

    ~Streamer() {
        if (responseBackup) {
            *responseBackup = getResult();
        }
    }

    std::string getResult() const {
        std::stringstream ss;
        ss << ostr->rdbuf();
        return ss.str();
    }

    std::string* responseBackup = nullptr;
};

struct MockStreamable : public yplatform::net::streamable {
    MOCK_METHOD(void, send_client_stream, (const yplatform::net::buffers::const_chunk_buffer&), (override));
    MOCK_METHOD(void, send_client_stream2, (const yplatform::net::buffers::const_chunk_buffer&, bool), (override));
    MOCK_METHOD(yplatform::net::streamer_wrapper, client_stream, (), (override));
    MOCK_METHOD(bool, is_open, (), (const, override));
};

struct MockStream: public ymod_webserver::http::stream {
    MOCK_METHOD(void, send_client_stream, (const yplatform::net::buffers::const_chunk_buffer&), (override));
    MOCK_METHOD(void, send_client_stream2, (const yplatform::net::buffers::const_chunk_buffer&, bool), (override));
    MOCK_METHOD(yplatform::net::streamer_wrapper, client_stream, (), (override));
    MOCK_METHOD(bool, is_open, (), (const, override));

    MOCK_METHOD(yplatform::time_traits::timer_ptr, make_timer, (), (const, override));

    MOCK_METHOD(void, result, (ymod_webserver::codes::code, const std::string&), ());
    MOCK_METHOD(void, result, (ymod_webserver::codes::code, const std::string&, const std::string&), ());

    MOCK_METHOD(void, begin_poll_connect, (), (override));
    MOCK_METHOD(void, cancel_poll_connect, (), (override));

    MOCK_METHOD(void, set_code, (ymod_webserver::codes::code, const std::string&), (override));
    MOCK_METHOD(void, add_header, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, add_header, (const std::string&, std::time_t), (override));
    MOCK_METHOD(void, set_content_type, (const std::string&), (override));
    MOCK_METHOD(void, set_content_type, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, set_cache_control, (ymod_webserver::cache_response_header, const std::string&), (override));
    MOCK_METHOD(void, set_connection, (bool), (override));

    MOCK_METHOD(void, result_body, (const std::string&), (override));
    MOCK_METHOD(yplatform::net::streamable_ptr, result_stream, (const std::size_t), (override));
    MOCK_METHOD(yplatform::net::streamable_ptr, result_chunked, (), (override));

    MOCK_METHOD(void, add_error_handler, (const error_handler& handler), (override));
    MOCK_METHOD(void, on_error, (const boost::system::error_code& e), (override));
    MOCK_METHOD(ymod_webserver::request_ptr, request, (), (const, override));
    MOCK_METHOD(ymod_webserver::context_ptr, ctx, (), (const, override));
    MOCK_METHOD(ymod_webserver::codes::code, result_code, (), (const, override));

    MOCK_METHOD(bool, is_secure, (), (const, override));
    MOCK_METHOD(boost::asio::io_service&, get_io_service, (), (override));
    MOCK_METHOD(const boost::asio::ip::address&, remote_addr, (), (const, override));
};

struct MetaAdaptorMock {
    using GetMasterHandler = std::function<void(ExplainedError, std::string)>;
    using ShardIdHandler = std::function<void (ExplainedError, Shard::Id)>;
    using RegDataHandler = std::function<void (ExplainedError, RegData)>;

    MOCK_METHOD(void, getMaster, (GetMasterHandler), (const));
    MOCK_METHOD(void, getDomainShardId, (const DomainId domainId, ShardIdHandler), (const));
    MOCK_METHOD(void, getRegData, (RegDataHandler), (const));
    MOCK_METHOD(void, getOrganizationShardId, (const OrgId orgId, ShardIdHandler), (const));
};

struct PeersAdaptorMock {
    using Handler = std::function<void (ExplainedError, Shard::Id)>;

    MOCK_METHOD(void, createDomain, (const std::string&, const CreateDomainParams&, Handler), (const));
    MOCK_METHOD(void, createOrganization, (const std::string&, const CreateOrganizationParams&, Handler), (const));
};

struct MetaAdaptor {
    MetaAdaptorMock* impl;

    template <class CompletionToken>
    auto getMaster(CompletionToken&& token) const {
        return performAsyncOperation<std::string, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token),
            [&](auto handler) { return impl->getMaster(std::move(handler)); });
    }

    template <class CompletionToken>
    auto getDomainShardId(const DomainId domainId, CompletionToken&& token) const
    {
        return performAsyncOperation<Shard::Id, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token),
            [&] (auto handler) { return impl->getDomainShardId(domainId, std::move(handler)); }
        );
    }

    template <class CompletionToken>
    auto getRegData(CompletionToken&& token) const {
        return performAsyncOperation<RegData, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token), [&](auto handler) { return impl->getRegData(std::move(handler)); });
    }

    template <class CompletionToken>
    auto getOrganizationShardId(const OrgId orgId, CompletionToken&& token) const {
        return performAsyncOperation<Shard::Id, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token),
            [&] (auto handler) { return impl->getOrganizationShardId(orgId, std::move(handler)); }
        );
    }
};

struct PeersAdaptor {
    PeersAdaptorMock* impl;

    template <class CompletionToken>
    auto createDomain(const std::string& master, const CreateDomainParams& params,
            CompletionToken&& token) const {
        return performAsyncOperation<Shard::Id, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token),
            [&] (auto handler) { impl->createDomain(master, params, std::move(handler)); }
        );
    }

    template <class CompletionToken>
    auto createOrganization(const std::string& master, const CreateOrganizationParams& params,
            CompletionToken&& token) const {
        return performAsyncOperation<Shard::Id, SingleHandlerAsyncOperation>(
            std::forward<CompletionToken>(token),
            [&] (auto handler) { impl->createOrganization(master, params, std::move(handler)); }
        );
    }
};

struct BlackboxMock : Blackbox {
    MOCK_METHOD(expected<std::vector<HostedDomain>>, getHostedDomains, (DomainId, const TaskContextPtr&), (const, override));
    MOCK_METHOD(expected<std::vector<HostedDomain>>, getHostedDomains, (const std::string&, const TaskContextPtr&), (const, override));
};

} // namespace
