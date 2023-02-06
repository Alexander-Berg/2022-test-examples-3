#include <catch.hpp>

#include <header_container.h>

using namespace ymod_httpclient;

struct t_header_container
{
    void check_true_flags(const header_container& headers, const std::set<string>& header_names)
    {
        const auto& flags = headers.contained_headers();

        REQUIRE(flags.host == header_names.contains("Host"));
        REQUIRE(flags.content_length == header_names.contains("Content-Length"));
        REQUIRE(flags.transfer_encoding == header_names.contains("Content-Transfer-Encoding"));
        REQUIRE(flags.content_type == header_names.contains("Content-Type"));
        REQUIRE(flags.connection == header_names.contains("Connection"));
        REQUIRE(flags.x_request_id == header_names.contains("X-Request-Id"));
        REQUIRE(flags.x_request_timeout == header_names.contains("X-Request-Timeout"));
        REQUIRE(flags.x_request_attempt == header_names.contains("X-Request-Attempt"));
        REQUIRE(flags.x_ya_service_ticket == header_names.contains("X-Ya-Service-Ticket"));
    }

    header_container headers;
};

TEST_CASE_METHOD(t_header_container, "empty_container_data")
{
    REQUIRE(headers.data().empty());
}

TEST_CASE_METHOD(t_header_container, "empty_container_service_header_flags")
{
    check_true_flags(headers, {});
}

TEST_CASE_METHOD(t_header_container, "add_header")
{
    headers.add("key", "value");
    REQUIRE(headers.data() == "key: value\r\n");
}

TEST_CASE_METHOD(t_header_container, "add_multiple_headers")
{
    headers.add("key0", "value0");
    headers.add("key1", "value1");
    REQUIRE(headers.data() == "key0: value0\r\nkey1: value1\r\n");
}

TEST_CASE_METHOD(t_header_container, "construct_from_string")
{
    string header_str("key0: value0\r\nkey1: value1\r\n");
    header_container headers(header_str);
    REQUIRE(headers.data() == header_str);
}

TEST_CASE_METHOD(t_header_container, "construct_from_dictionary")
{
    header_dict header_dict = { { "key0", "value0" }, { "key1", "value1" } };
    header_container headers(header_dict);
    REQUIRE(headers.data() == "key0: value0\r\nkey1: value1\r\n");
}

TEST_CASE_METHOD(t_header_container, "move_assign")
{
    string header_str("key0: value0\r\nkey1: value1\r\n");
    header_container new_headers(header_str);
    headers = std::move(new_headers);
    REQUIRE(headers.data() == header_str);
}

TEST_CASE_METHOD(t_header_container, "write_to_stream")
{
    std::ostringstream os;
    headers.add("key", "value");
    os << headers;
    REQUIRE(os.str() == "key: value\r\n");
}

TEST_CASE_METHOD(t_header_container, "update_service_header_flags_when_add_header")
{
    headers.add("Host", "localhost");
    check_true_flags(headers, { "Host" });
}

TEST_CASE_METHOD(t_header_container, "update_service_header_flags_when_construct_from_string")
{
    string header_str("Host: localhost\r\nNot-Service-Header: value\r\n");
    header_container headers(header_str);
    check_true_flags(headers, { "Host" });
}

TEST_CASE_METHOD(t_header_container, "update_service_header_flags_when_construct_from_dictionary")
{
    header_dict header_dict = { { "Host", "localhost" }, { "Not-Service-Header", "value" } };
    header_container headers(header_dict);
    check_true_flags(headers, { "Host" });
}

TEST_CASE_METHOD(t_header_container, "update_service_header_flags_when_move_assign")
{
    string header_str("Host: localhost\r\nNot-Service-Header: value\r\n");
    header_container new_headers(header_str);
    headers = std::move(new_headers);
    check_true_flags(headers, { "Host" });
}

TEST_CASE_METHOD(t_header_container, "update_service_header_flags_when_multiple_actions")
{
    headers.add("Host", "localhost");
    header_container new_headers("Content-Length: 10\r\n");
    headers = std::move(new_headers);
    headers.add("Content-Type", "image");
    check_true_flags(headers, { "Content-Length", "Content-Type" });
}

TEST_CASE_METHOD(t_header_container, "add_headers_with_multiline_value")
{
    headers.add("Host", "line0\n line1\n line2");
    headers.add("Content-Type", "line0\n\tline1\n\tline2");
    REQUIRE(
        headers.data() ==
        "Host: line0\n line1\n line2\r\n"
        "Content-Type: line0\n\tline1\n\tline2\r\n");
    check_true_flags(headers, { "Host", "Content-Type" });
}
