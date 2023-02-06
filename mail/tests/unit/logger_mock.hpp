#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/log.hpp>

namespace logdog {

template <typename Key1, typename Value1, typename Key2, typename Value2>
static constexpr bool operator ==(const attribute<Key1, Value1>&, const attribute<Key2, Value2>&) {
    return std::is_same_v<Key1, Key2> && std::is_same_v<Value1, Value2>;
}

} // namespace logdog

namespace collie::tests {

using namespace testing;

using ExceptionAttribute = decltype(collie::log::exception=std::declval<const std::exception&>());
using ErrorAttribute = decltype(collie::log::error_code=std::declval<std::error_code>());
using RequestIdAttribute = decltype(collie::log::request_id=std::declval<const std::string&>());
using UniqIdAttribute = decltype(collie::log::uniq_id=std::declval<const std::string&>());
using MessageAttribute = decltype(collie::log::message=std::declval<std::string>());

struct LoggerMock {
    using error_level = std::decay_t<decltype(logdog::error)>;
    MOCK_METHOD(bool, applicable, (error_level), (const));
    MOCK_METHOD(void, write, (error_level, ExceptionAttribute), (const));
    MOCK_METHOD(void, write, (error_level, RequestIdAttribute, UniqIdAttribute, ExceptionAttribute), (const));
    MOCK_METHOD(void, write, (error_level, RequestIdAttribute, UniqIdAttribute, MessageAttribute), (const));
    MOCK_METHOD(void, write, (error_level, RequestIdAttribute, UniqIdAttribute, MessageAttribute, ErrorAttribute), (const));
};

struct LoggerMockWrapper {
    std::shared_ptr<StrictMock<LoggerMock>> impl = std::make_shared<StrictMock<LoggerMock>>();

    template <class T>
    bool applicable(T level) const {
        return impl->applicable(level);
    }

    template <class ... Ts>
    void write(Ts&& ... args) const {
        impl->write(std::forward<Ts>(args) ...);
    }
};

} // namespace collie::tests
