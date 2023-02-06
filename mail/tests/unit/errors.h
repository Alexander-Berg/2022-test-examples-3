#pragma once

#include <src/access_impl/retry.h>

/**
 * Error codes for testing
 */

namespace doberman {
namespace testing {
namespace errors {

enum errors {
    ok,
    retriable,
    nonretriable,
};

class error_category : public boost::system::error_category {
public:
    const char* name() const BOOST_NOEXCEPT override {
        return "doberman::testing::errors";
    }

    std::string message(int ev) const override {
        switch (ev) {
        case ok:
            return "ok";
        case retriable:
            return "retriable";
        case nonretriable:
            return "nonretriable";
        default:
            return "unknown";
        }
    }

    bool equivalent(int code, const boost::system::error_condition &ec
                    ) const noexcept override {
        using namespace doberman::access_impl;
        if (code ==  retriable) {
            return ec == errc::temporary_error;
        }
        return boost::system::error_category::equivalent(code, ec);
    }

    bool equivalent(const boost::system::error_code &code, int ec
                    ) const noexcept override {
        return boost::system::error_category::equivalent(code, ec);
    }
};

inline const boost::system::error_category& category() {
    static error_category instance;
    return instance;
}

inline boost::system::error_code make_error_code(errors e) {
    return {static_cast<int>(e), category()};
}

} // namespace errors
} // namespace testing
} // namespace doberman

namespace boost {
namespace system {

template <>
struct is_error_code_enum<doberman::testing::errors::errors> {
    static const bool value = true;
};

} // namespace system
} // namespace boost
