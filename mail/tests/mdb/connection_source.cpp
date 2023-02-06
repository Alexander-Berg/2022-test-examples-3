#include <ozo/yandex/mdb/connection_source.h>
#include "../test_error.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {
using namespace ozo::yandex::tests;
using namespace ::testing;

struct simple_source_map : Test {
    struct connection_source {
        std::string connstr;
        struct connection_type;
        bool operator == (const connection_source& rhs) const {
            return connstr == rhs.connstr;
        }
        friend std::ostream& operator << (std::ostream& s, const connection_source& v) {
            return s << '{' << v.connstr << '}';
        }
    };

    struct source_ctor_mock {
        MOCK_METHOD(connection_source, call, (const std::string&), (const));

        connection_source operator () (const std::string& connstr) const {
            return call(connstr);
        }
    };

    StrictMock<source_ctor_mock> source_ctor;
    connection_source source{"test connstr"};
};

TEST_F(simple_source_map, should_call_connection_source_ctor_if_no_source_found) {
    auto source_map = ozo::yandex::mdb::simple_source_map(std::cref(source_ctor));
    EXPECT_CALL(source_ctor, call("test connstr")).WillOnce(Return(source));
    auto res = source_map("test connstr");
    EXPECT_EQ(source, res);
}

TEST_F(simple_source_map, should_not_call_connection_source_ctor_if_source_found) {
    auto source_map = ozo::yandex::mdb::simple_source_map(
        std::cref(source_ctor), {{"test connstr", source}});
    auto res = source_map("test connstr");
    EXPECT_EQ(source, res);
}

TEST_F(simple_source_map, should_apply_function_to_internal_map_and_return_call_result) {
    auto source_map = ozo::yandex::mdb::simple_source_map(
        std::cref(source_ctor), {{"test connstr", source}});
    const decltype(source_map)::map_type expected{{"test connstr", source}};
    const auto res = source_map.apply([&expected] (auto& map) {
        EXPECT_EQ(map, expected);
        return 10;
    });
    EXPECT_EQ(res, 10);
}

struct resolve_connstr_op : Test {
    ozo::io_context io;

    using duration = ozo::time_traits::duration;

    struct connection {};

    struct handler_mock {
        MOCK_METHOD(void, call, (ozo::error_code, connection), (const));

        void operator () (ozo::error_code ec, connection conn) const {
            return call(ec, conn);
        }
    } handler;

    struct connection_source {
        using connection_type = connection;

        using handler_type = std::function<void(ozo::error_code, connection_type)>;

        MOCK_METHOD(void, call, (duration, handler_type), (const));

        void operator () (ozo::io_context&, duration t, handler_type h) const {
            call(t, h);
        }
    } source;

    struct source_map_mock {
        using connection_type = connection_source::connection_type;

        MOCK_METHOD(connection_source&, call, (const std::string&), (const));

        connection_source& operator () (const std::string& connstr) const {
            return call(connstr);
        }
    } source_map;
};

using namespace std::chrono_literals;

TEST_F(resolve_connstr_op, should_call_handler_for_error) {
    ozo::yandex::mdb::resolve_connstr_op op{io, std::cref(source_map), 1s, std::cref(handler)};

    EXPECT_CALL(handler, call(ozo::error_code{error::error}, _));

    op(error::error, "");
}

TEST_F(resolve_connstr_op, should_resolve_connection_string_to_source_and_invoke_it_with_handler) {
    ozo::yandex::mdb::resolve_connstr_op op{io, std::cref(source_map), 1s, std::cref(handler)};

    EXPECT_CALL(source_map, call("connection string")).WillOnce(ReturnRef(source));
    EXPECT_CALL(source, call(duration(1s), _))
        .WillOnce(InvokeArgument<1>(ozo::error_code{}, connection{}));
    EXPECT_CALL(handler, call(ozo::error_code{}, _));

    op(ozo::error_code{}, "connection string");
}

struct connection_source : Test {
    static constexpr ozo::yandex::mdb::role<struct test_role_tag> test_role;
    static constexpr ozo::yandex::mdb::role<struct another_test_role_tag> another_test_role;

    using role = std::variant<decltype(test_role), decltype(another_test_role)>;

    using source_map_mock = resolve_connstr_op::source_map_mock;

    using handler_mock = resolve_connstr_op::handler_mock;

    source_map_mock source_map;
    handler_mock handler;
    ozo::io_context io;

    struct resolver_mock {
        using handler_type = std::function<void(ozo::error_code, std::string)>;
        MOCK_METHOD(void, call, (role, handler_type), (const));
        template <typename Role, typename Handler>
        void operator () (Role role, Handler&& handler) const {
            call(role, std::forward<Handler>(handler));
        }
    } resolver;
};

TEST_F(connection_source, should_call_resolver_with_role_and_callback) {
    auto source = ozo::yandex::mdb::connection_source(
        test_role, std::cref(resolver), std::cref(source_map)
    );
    EXPECT_CALL(resolver, call(role{test_role}, _));
    source(io, 1s, std::cref(handler));

    EXPECT_CALL(resolver, call(role{test_role}, _));
    std::as_const(source)(io, 1s, std::cref(handler));

    EXPECT_CALL(resolver, call(role{test_role}, _));
    std::move(source)(io, 1s, std::cref(handler));
}

TEST_F(connection_source, rebind_role_should_return_connection_source_with_new_role) {
    auto source = ozo::yandex::mdb::connection_source(
        test_role, std::cref(resolver), std::cref(source_map)
    );
    EXPECT_EQ(source.rebind_role(another_test_role).role(), another_test_role);
    EXPECT_EQ(std::as_const(source).rebind_role(another_test_role).role(), another_test_role);
    EXPECT_EQ(std::move(source).rebind_role(another_test_role).role(), another_test_role);
}


} // namespace
