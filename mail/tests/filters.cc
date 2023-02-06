#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <logdog/logger.h>

namespace logdog::testing::errors {

enum errors {
    ok,
    not_ok,
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
        case not_ok:
            return "not_ok";
        default:
            return "unknown";
        }
    }
};

inline const boost::system::error_category& category() {
    static error_category instance;
    return instance;
}

inline boost::system::error_code make_error_code(errors e) {
    return {static_cast<int>(e), category()};
}

} // namespace logdog::testing::errors

namespace boost::system {

template <>
struct is_error_code_enum<logdog::testing::errors::errors> : std::true_type {};

} // namespace boost::system

namespace std {

template <>
struct is_error_code_enum<logdog::testing::errors::errors> : std::true_type {};

} // namespace std

namespace {

using namespace logdog::filters;
using namespace logdog::testing;

template <typename T1, typename T2>
auto eq_value(const T1& v1, const T2& v2) {
    return v1 == v2;
}

template <typename T,typename ...Ts>
auto eq_value(const boost::variant<Ts...>& lhs, const T& rhs) {
    auto lhs_ptr = boost::get<T>(&lhs);
    return lhs_ptr ? *lhs_ptr == rhs : false;
}

template <typename Attr, typename Val>
auto expect_attribute_eq(const Attr& attr, Val&& val) {
    return eq_value(logdog::value(*attr), val);
}

template <typename Val>
constexpr bool expect_attribute_eq(logdog::static_optional<logdog::none_t>, Val&&) {
    return false;
}

template <typename Sequence, typename Attr, typename Val>
bool expect_attribute(const Sequence& seq, Attr&& attr, Val&& val) {
    if constexpr (decltype(logdog::has_attribute(seq, attr))::value) {
        return expect_attribute_eq(logdog::get_attribute(seq, attr), val);
    } else {
        return false;
    }
}

TEST(expand_system_error, should_add_error_code_from_system_error_exception_if_no_error_code_attribute_found) {
    using filter = make_sequence<expand_system_error>;
    using error_code = boost::system::error_code;
    const boost::system::system_error ex(error_code{errors::not_ok});

    filter::apply([&](auto ...args){
            const auto seq = std::tie(args...);
            EXPECT_TRUE(
                expect_attribute(seq, logdog::error_code, error_code{errors::not_ok}));
        },
        logdog::exception=ex
    );
}

TEST(expand_system_error, should_add_error_code_from_std_system_error_exception_if_no_error_code_attribute_found) {
    using filter = make_sequence<expand_system_error>;
    using error_code = std::error_code;
    const std::system_error ex(error_code{errors::not_ok});

    filter::apply([&](auto ...args){
            const auto seq = std::tie(args...);
            EXPECT_TRUE(
                expect_attribute(seq, logdog::error_code, error_code{errors::not_ok}));
        },
        logdog::exception=ex
    );
}

TEST(expand_system_error, should_not_add_error_code_from_system_error_exception_if_error_code_attribute_found) {
    using filter = make_sequence<expand_system_error>;
    using error_code = boost::system::error_code;
    const boost::system::system_error ex(error_code{errors::not_ok});

    filter::apply([&](auto ...args){
            const auto seq = std::tie(args...);
            EXPECT_FALSE(
                expect_attribute(seq, logdog::error_code, error_code{errors::not_ok}));
        },
        logdog::exception=ex, logdog::error_code=error_code{errors::ok}
    );
}

}
