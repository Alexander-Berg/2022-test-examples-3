#pragma once

#include <src/error_code.hpp>

namespace collie::tests {

enum class Error {
    ok,
    fail,
};

} // namespace collie::tests

namespace boost::system {

template <>
struct is_error_code_enum<collie::tests::Error> : std::true_type {};

} // namespace boost::system

namespace collie::tests {

class ErrorCategory final : public boost::system::error_category {
public:
    const char* name() const noexcept override {
        return "collie::logic::abook::ErrorCategory";
    }

    std::string message(int value) const override {
        switch (static_cast<Error>(value)) {
            case Error::ok:
                return "ok";
            case Error::fail:
                return "fail";
        }
        return "unknown error code: " + std::to_string(value);
    }

    static const ErrorCategory& instance() {
        static const ErrorCategory errorCategory;
        return errorCategory;
    }
};

inline error_code::base_type make_error_code(Error ec) {
    return error_code::base_type(static_cast<int>(ec), ErrorCategory::instance());
}

} // namespace collie::tests
