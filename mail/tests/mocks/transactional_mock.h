#pragma once

#include <macs_pg/service/factory.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <unordered_map>

namespace pgg {
namespace query {

bool operator ==(const Query& lhs, const Query& rhs);

} // namespace query
} // namespace pgg

namespace tests {

using namespace testing;
using namespace macs;
using namespace macs::pg;
using namespace pgg::query;

std::ostream& operator <<(std::ostream& stream, const error_code& value);

std::ostream& operator <<(std::ostream& stream, const Query& value);

struct QueryMock {
    MOCK_METHOD(Text, text, (), (const));
    MOCK_METHOD(void, mapValues, (const Mapper&), (const));
    MOCK_METHOD(const char*, name, (), (const));
    MOCK_METHOD(Milliseconds, timeout, (), (const));
    MOCK_METHOD(bool, debug, (), (const));
    MOCK_METHOD(Query::EndpointType, endpointType, (), (const));
    MOCK_METHOD(Query::QueryPtr, clone, (), (const));
};

struct Row {
    using Map = std::unordered_map<std::string, boost::any>;
    Map data;

    bool has_column(const std::string& name) const {
        return data.count(name);
    }

    template <typename T>
    void at(const std::string& name, T& value) const {
        const auto it = data.find(name);
        value = (it == data.end() ? T() : boost::any_cast<T>(it->second));
    }
};

struct ConnProviderFake {};

using FakeDataRange = std::vector<Row>;
using FetchHandler = std::function<void (pgg::database::error_code, const FakeDataRange&)>;
using ExecuteHandler = std::function<void (pgg::database::error_code)>;

struct TransactionMock {
    MOCK_METHOD(void, fetchImpl, (const Query&, FetchHandler), ());
    MOCK_METHOD(void, executeImpl, (const Query&, ExecuteHandler), ());
    MOCK_METHOD(void, beginImpl, (ConnProviderFake, ExecuteHandler, Milliseconds), ());
    MOCK_METHOD(void, commitImpl, (ExecuteHandler), ());
    MOCK_METHOD(void, rollbackImpl, (ExecuteHandler), ());

    void fetch(const Query& query, FetchHandler hook) {
        fetchImpl(query, hook);
    }

    void execute(const Query& query, ExecuteHandler hook) {
        executeImpl(query, hook);
    }

    void begin(ConnProviderFake connProvider, ExecuteHandler hook, Milliseconds timeout) {
        beginImpl(connProvider, hook, timeout);
    }

    void commit(ExecuteHandler hook) {
        commitImpl(hook);
    }

    void rollback(ExecuteHandler hook) {
        rollbackImpl(hook);
    }
};

struct TransactionalMock : public boost::enable_shared_from_this<TransactionalMock> {
    MOCK_METHOD(void, fetchImpl, (const Query&, FetchHandler), ());
    MOCK_METHOD(void, executeImpl, (const Query&, ExecuteHandler), ());
    MOCK_METHOD(void, beginImpl, (ExecuteHandler, Milliseconds), ());
    MOCK_METHOD(void, commitImpl, (ExecuteHandler), ());
    MOCK_METHOD(void, rollbackImpl, (ExecuteHandler), ());

    void fetch(const Query& query, FetchHandler hook) {
        fetchImpl(query, hook);
    }

    void execute(const Query& query, ExecuteHandler hook) {
        executeImpl(query, hook);
    }

    void begin(ExecuteHandler hook, Milliseconds timeout) {
        beginImpl(hook, timeout);
    }

    void commit(ExecuteHandler hook) {
        commitImpl(hook);
    }

    void rollback(ExecuteHandler hook) {
        rollbackImpl(hook);
    }
};

using pgg_error_code = pgg::database::error_code;
static const pgg_error_code operationAborted {error_code(boost::asio::error::operation_aborted)};

} // namespace tests
