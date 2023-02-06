#pragma once

#include <mail_errors/error_code.h>

namespace errors1 {

enum Errors {
    ok = 0,
    logic,
    input,
};

using error_category = mail_errors::error_code::error_category;
using error_code = mail_errors::error_code;
class Category : public error_category {
public:
    const char* name() const noexcept override {
        return "errors1::Category";
    }

    std::string message(int v) const override {
        switch(Errors(v)) {
            case ok :
                return "no error";
            case logic :
                return "logic error";
            case input :
                return "invalid input";
        }
        return "unknown error";
    }
};

inline const error_category& getCategory() {
    static Category instance;
    return instance;
}

inline error_code::base_type make_error_code(Errors e) {
    return error_code::base_type(static_cast<int>(e), getCategory());
}

}

namespace errors2 {

enum Errors {
    logic = 1,
    input,
};

using error_category = mail_errors::error_code::error_category;
using error_code = mail_errors::error_code;
class Category : public error_category {
public:
    const char* name() const noexcept override {
        return "errors2::Category";
    }

    std::string message(int v) const override {
        switch(Errors(v)) {
            case logic :
                return "logic error";
            case input :
                return "invalid input";
        }
        return "unknown error";
    }
};

inline const error_category& getCategory() {
    static Category instance;
    return instance;
}

inline error_code::base_type make_error_code(Errors e) {
    return error_code::base_type(static_cast<int>(e), getCategory());
}

}

namespace boost {
namespace system {

template <>
struct is_error_code_enum<errors1::Errors> : std::true_type {};

template <>
struct is_error_code_enum<errors2::Errors> : std::true_type {};

} // namespace system
} // namespace boost
