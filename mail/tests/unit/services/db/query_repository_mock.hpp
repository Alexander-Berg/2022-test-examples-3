#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/hana/tuple.hpp>

namespace collie::tests {

using namespace testing;

template <typename ...Ts>
struct QueryRepositoryMock;

template <typename Query, typename ...Ts>
struct QueryRepositoryMock<Query, Ts...> : QueryRepositoryMock<Ts...> {
    MOCK_CONST_METHOD1_T(make_query, Query (const typename Query::parameters_type&));
};

template <>
struct QueryRepositoryMock<> {};

template <class ... Ts>
struct QueryRepository {
    StrictMock<const QueryRepositoryMock<Ts ...>>* mock = nullptr;

    template <class Query>
    auto make_query(const typename Query::parameters_type& params) const {
        return mock->make_query(params);
    }
};

} // namespace collie::tests
