#pragma once

#include <tests/unit/services/db/query_repository_mock.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/error_code.hpp>
#include <src/task_context.hpp>
#include <src/services/db/contacts/query.hpp>
#include <src/services/db/passport_user_id.hpp>
#include <src/services/db/org_user_id.hpp>
#include <src/services/db/get_connection.hpp>
#include <src/services/db/request.hpp>
#include <src/services/db/query_repository.hpp>
#include <src/services/db/execute.hpp>

namespace collie::tests {

using namespace testing;

using collie::services::db::PassportUserId;
using collie::services::db::OrgUserId;
using collie::services::db::ConstUserType;

template <class ... Ts>
struct ConnectionProviderMock {
    MOCK_CONST_METHOD0_T(queryRepository, QueryRepository<Ts ...> ());
};

template <class Q, class ...Ts>
struct ConnectionProviderMock<Q, Ts...> : ConnectionProviderMock<Ts...> {
    using RequestResult = expected<
        std::conditional_t<
            std::is_same_v<typename Q::result_type, void>,
            void,
            std::vector<typename Q::result_type>
        >
    >;

    MOCK_CONST_METHOD1_T(request, RequestResult (const Q&));
    MOCK_CONST_METHOD1_T(execute, expected<void> (const Q&));

    using ConnectionProviderMock<Ts...>::request;
    using ConnectionProviderMock<Ts...>::execute;

    using ConnectionProviderMock<Ts...>::gmock_request;
    using ConnectionProviderMock<Ts...>::gmock_execute;

};

template <>
struct ConnectionProviderMock<> {
    using GetConnectionResult = expected<StrictMock<ConnectionProviderMock>*>;

    MOCK_METHOD(std::int64_t, uid, (), (const));
    MOCK_METHOD(ConstUserType, userType, (), (const));
    MOCK_METHOD(TaskContextPtr, context, (), (const));
    MOCK_METHOD(ozo::time_traits::duration, requestTimeout, (), (const));
    MOCK_METHOD(std::size_t, maxRetriesNumber, (), (const));
    MOCK_METHOD(QueryRepository<>, queryRepository, (), (const));
    MOCK_METHOD(GetConnectionResult, getConnection, (), (const));
    MOCK_METHOD(void, request, (), (const));
    MOCK_METHOD(void, execute, (), (const));
    MOCK_METHOD(void, begin, (ozo::error_code&), (const));
    MOCK_METHOD(void, commit, (ozo::error_code&), (const));
};

template <class ... Ts>
struct ConnectionProvider {
    using connection_type = void;

    StrictMock<ConnectionProviderMock<Ts ...>>* mock = nullptr;

    std::int64_t uid() const {
        return mock->uid();
    }

    ConstUserType userType() const {
        return mock->userType();
    }

    auto context() const {
        return mock->context();
    }

    auto maxRetriesNumber() const {
        return mock->maxRetriesNumber();
    }

    auto requestTimeout() const {
        return mock->requestTimeout();
    }

    auto queryRepository() const {
        return mock->queryRepository();
    }
};

// static_assert(services::db::ConnectionProvider<ConnectionProvider<>>);

template <class ... Ts>
struct MakeConnectionProviderMock {
    MOCK_CONST_METHOD2_T(call, ConnectionProvider<Ts ...>(TaskContextPtr, PassportUserId));
    MOCK_CONST_METHOD2_T(call, ConnectionProvider<Ts ...>(TaskContextPtr, OrgUserId));
    MOCK_CONST_METHOD1_T(call, ConnectionProvider<Ts ...>(TaskContextPtr));
};

template <class ... Ts>
struct MakeConnectionProvider {
    StrictMock<MakeConnectionProviderMock<Ts ...>>* mock = nullptr;

    auto operator()(TaskContextPtr context, PassportUserId uid) const {
        return mock->call(context, uid);
    }

    auto operator()(TaskContextPtr context, OrgUserId uid) const {
        return mock->call(context, uid);
    }

    auto operator()(TaskContextPtr context) const {
        return mock->call(context);
    }
};

} // namespace collie::tests

namespace collie::services::db {
template <typename ...Ts>
struct IsConnectionProvider<collie::tests::ConnectionProvider<Ts...>> : std::true_type {};
} // namespace collie::services::db

namespace collie::services::db {

template <class ... Ts>
struct GetConnectionImpl<collie::tests::ConnectionProvider<Ts ...>> {
    static auto apply(collie::tests::ConnectionProvider<Ts ...> provider) {
        return provider.mock->getConnection();
    }
};

template <class ... Ts>
struct RequestWithQueryRepositoryImpl<collie::tests::ConnectionProvider<Ts ...>> {
    template <class Query>
    static auto apply(collie::tests::ConnectionProvider<Ts ...> provider, Query&& query) {
        return provider.mock->request(query);
    }
};

template <class ... Ts>
struct RequestWithQueryRepositoryImpl<Retry<collie::tests::ConnectionProvider<Ts ...>>> {
    template <class Query>
    static auto apply(Retry<collie::tests::ConnectionProvider<Ts ...>> provider, Query&& query) {
        return db::unwrap(provider).mock->request(query);
    }
};

template <class ... Ts>
struct ExecuteWithQueryRepositoryImpl<collie::tests::ConnectionProvider<Ts ...>> {
    template <class Query>
    static auto apply(collie::tests::ConnectionProvider<Ts ...> provider, Query&& query) {
        return provider.mock->execute(query);
    }
};

template <class ... Ts>
struct ExecuteWithQueryRepositoryImpl<Retry<collie::tests::ConnectionProvider<Ts ...>>> {
    template <class Query>
    static auto apply(Retry<collie::tests::ConnectionProvider<Ts ...>> provider, Query&& query) {
        return db::unwrap(provider).mock->execute(query);
    }
};

} // namespace collie::services::db
