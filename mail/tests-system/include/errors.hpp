#pragma once

#include <mail_errors/error_code.h>

namespace apq_tester {

using mail_errors::error_code;

enum class Error
{
    ok,
    connectError,
    sizeResultError,
    resultError
};

}
namespace boost::system {

template <>
struct is_error_condition_enum<apq_tester::Error> : std::true_type
{
};

}

namespace apq_tester {

class ErrorCategory final : public boost::system::error_category
{
public:
    const char* name() const noexcept override
    {
        return "apq_tester";
    }

    std::string message(int value) const override
    {
        switch (static_cast<Error>(value))
        {
        case Error::ok:
            return "ok";
        case Error::connectError:
            return "connection error";
        case Error::sizeResultError:
            return "size result error";
        case Error::resultError:
            return "result error";
        }
        return "unknown error code: " + std::to_string(value);
    }
};

inline const ErrorCategory& getErrorCategory()
{
    static ErrorCategory errorCategory;
    return errorCategory;
}

inline auto make_error_code(Error ec)
{
    return error_code::base_type(static_cast<int>(ec), getErrorCategory());
}

inline auto make_error_condition(Error e)
{
    return boost::system::error_condition(static_cast<int>(e), getErrorCategory());
}

}
