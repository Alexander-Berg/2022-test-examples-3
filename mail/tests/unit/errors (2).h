#pragma once

#include <mail_errors/error_code.h>

namespace errors {
enum Errors {
    ok = 0,
    pgg_error,
    sharpei_error
};

using error_category = mail_errors::error_code::error_category;
using error_code = mail_errors::error_code;
class Category: public mail_errors::error_code::error_category {
public:
    const char* name() const noexcept override {
        return "errors::Category";
    }

    std::string message(int v) const override {
        switch (Errors(v)) {
            case ok:
                return "no error";
            case pgg_error:
                return "pgg failed";
            case sharpei_error:
                return "Failed get conninfo";
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
    struct is_error_code_enum<errors::Errors> : std::true_type {};

}
}
