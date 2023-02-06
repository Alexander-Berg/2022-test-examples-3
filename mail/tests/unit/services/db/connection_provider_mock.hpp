#pragma once

#include "query_repository_mock.hpp"

#include <gmock/gmock.h>

#include <src/services/db/connection_provider.hpp>
#include <src/task_context.hpp>
#include <src/services/db/begin.hpp>
#include <src/services/db/execute.hpp>
#include <src/services/db/request.hpp>

#include <ozo/error.h>
#include <ozo/time_traits.h>

#include <boost/hana/define_struct.hpp>

namespace collie::tests {

using namespace testing;

struct FakeQuery {
    BOOST_HANA_DEFINE_STRUCT(FakeQuery,
        (std::int64_t, param)
    );

    using parameters_type = FakeQuery;
    using result_type = std::tuple<std::int64_t>;

    friend FakeQuery get_text(FakeQuery self) {
        return self;
    }

    friend const char* to_const_char(FakeQuery) {
        return "query name";
    }
};

inline bool operator ==(const FakeQuery& lhs, const FakeQuery& rhs) {
    return boost::hana::to_tuple(lhs) == boost::hana::to_tuple(rhs);
}

using BackInsertIterator = std::back_insert_iterator<std::vector<typename FakeQuery::result_type>>;

struct Out {
    Out() = default;
    Out(BackInsertIterator) {}
};

inline bool operator ==(Out, Out) {
    return true;
}

struct ConnectionProviderMock {
    MOCK_METHOD(ozo::time_traits::duration, requestTimeout, (), (const));
    MOCK_METHOD(std::size_t, maxRetriesNumber, (), (const));
    MOCK_METHOD(TaskContextPtr, context, (), (const));
    MOCK_METHOD(QueryRepository<FakeQuery>, queryRepository, (), (const));
    MOCK_METHOD(ozo::error_code, execute, (const FakeQuery&, const ozo::time_traits::duration&), (const));
    MOCK_METHOD(ozo::error_code, request, (const FakeQuery&, const ozo::time_traits::duration&, Out), (const));
    MOCK_METHOD(ozo::error_code, begin, (const ozo::time_traits::duration&), (const));
};

struct ConnectionProvider {
    using connection_type = ConnectionProvider;

    StrictMock<ConnectionProviderMock>* mock = nullptr;

    auto requestTimeout() const {
        return mock->requestTimeout();
    }

    auto maxRetriesNumber() const {
        return mock->maxRetriesNumber();
    }

    TaskContextPtr context() const {
        return mock->context();
    }

    QueryRepository<FakeQuery> queryRepository() const {
        return mock->queryRepository();
    }

    operator bool () const {
        return true;
    }

    friend std::string get_error_context(ConnectionProvider) {
        return "dummy";
    }

    friend std::string_view error_message(ConnectionProvider) {
        return "dummy";
    }
};

} // namespace collie::tests

namespace collie::services::db {
template <>
struct IsConnectionProvider<collie::tests::ConnectionProvider> : std::true_type {};
}

namespace collie::services::db::adl {

template <>
struct BeginImpl<collie::tests::ConnectionProvider> {
    template <typename ConnectionProvider>
    static auto apply(ConnectionProvider&& self, const ozo::time_traits::duration& timeout,
            boost::asio::yield_context yield) {
        *yield.ec_ = self.mock->begin(timeout);
        return self;
    }
};

template <>
struct ExecuteImpl<collie::tests::ConnectionProvider> {
    template <typename ConnectionProvider>
    static auto apply(ConnectionProvider&& self, const tests::FakeQuery& q,
            const ozo::time_traits::duration& timeout, boost::asio::yield_context yield) {
        *yield.ec_ = self.mock->execute(q, timeout);
        return self;
    }
};

template <>
struct RequestImpl<collie::tests::ConnectionProvider> {
    template <typename ConnectionProvider>
    static auto apply(ConnectionProvider&& self, const tests::FakeQuery& q,
            const ozo::time_traits::duration& timeout, tests::Out out,
            boost::asio::yield_context yield) {
        *yield.ec_ = self.mock->request(q, timeout, out);
        return self;
    }
};

} // namespace collie::services::db::adl
