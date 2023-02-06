#pragma once

#include "query_repository_mock.hpp"
#include <src/services/db/request.hpp>

#include <gmock/gmock.h>

#include <ozo/error.h>
#include <ozo/time_traits.h>

#include <boost/range/algorithm/copy.hpp>

namespace collie::tests {

using namespace testing;

template <class ... Ts>
struct ConnectionMock;

template <class Query, class ... Ts>
struct ConnectionMock<Query, Ts ...> : ConnectionMock<Ts ...> {
    MOCK_CONST_METHOD2_T(request, ozo::error_code (const Query&, std::vector<typename Query::result_type>&));
};

template <class Query, class ... Ts>
struct RequestImpl {
    template <typename Out, typename Operation>
    static auto apply(ConnectionMock<Query, Ts ...>* self, Query&& q, const ozo::time_traits::duration&,
            Out&& out, Operation op) {
        std::vector<typename Query::result_type> result;
        const auto ec = self->request(std::forward<Query>(q), result);
        boost::copy(result, out);
        op(ec, self);
    }
};

template <>
struct ConnectionMock<> {};

} // namespace collie::tests

namespace collie::services::db::adl {

template <class ...Ts>
struct RequestImpl<collie::tests::ConnectionMock<Ts...>*>
    : collie::tests::RequestImpl<Ts...>{};

template <class ...Ts>
struct RequestImpl<testing::StrictMock<collie::tests::ConnectionMock<Ts...>>*>
    : collie::tests::RequestImpl<Ts...>{};

} // namespace collie::services::db::adl
