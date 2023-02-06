#pragma once

#include "connection_provider_mock.hpp"

#include <src/services/db/begin.hpp>
#include <src/services/db/commit.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace collie::services::db {

template <class ... Ts>
struct Begin<tests::ConnectionProvider<Ts...>> {
    static auto apply(tests::ConnectionProvider<Ts...> provider) {
        ozo::error_code ec;
        provider.mock->begin(ec);
        if (ec) {
            return make_expected_from_error<decltype(provider)>(error_code(std::move(ec)));
        }
        return expected(std::move(provider));
    }
};

template <class ... Ts>
struct Commit<tests::ConnectionProvider<Ts...>> {
    template <class Transaction>
    static auto apply(tests::ConnectionProvider<Ts...> provider, Transaction&&) {
        ozo::error_code ec;
        provider.mock->commit(ec);
        if (ec) {
            return make_expected_from_error<decltype(provider)>(error_code(std::move(ec)));
        }
        return expected(std::move(provider));
    }
};

} // collie::services::db
