#include "protocol.h"
#include "call_impl.h"
#include <ymod_httpclient/request.h>
#include <catch.hpp>

using namespace ymod_httpclient;
using namespace ymod_httpclient::detail;

const string BOUNDARY = "--------yplatform-http-client-data-boundary";
const string DELIMITER = "\r\n--" + BOUNDARY + "\r\n";
const string CLOSED_DELIMITER = "\r\n--" + BOUNDARY + "--\r\n";
const string CONTENT_TYPE_MIXED_HDR =
    "Content-Type: multipart/mixed; boundary=" + BOUNDARY + "\r\n";
const string CONTENT_TYPE_ALTERNATIVE_HDR =
    "Content-Type: multipart/alternative; boundary=" + BOUNDARY + "\r\n";
const string CONTENT_TYPE_FORM_DATA_HDR =
    "Content-Type: multipart/form-data; boundary=" + BOUNDARY + "\r\n";
const string JSON_BODY = "{\"foo\": 1}";
const string JSON_PART = "Content-Type: application/json\r\n\r\n" + JSON_BODY;
const string RFC822_BODY = "From: from@ya.ru\r\n\r\nHello";
const string RFC822_PART = "Content-Type: message/rfc822\r\n\r\n" + RFC822_BODY;

auto make_request_data(request&& req)
{
    auto req_data = make_shared<request_data>();
    req_data->context = boost::make_shared<yplatform::task_context>();

    auto remote_point = boost::make_shared<remote_point_info>();
    remote_point->host = "localhost";
    remote_point->uri_prefix = req.url;

    req_data->remote_point = remote_point;
    assign(req_data->headers, std::move(req.headers));
    req_data->post = std::move(req.body);
    req_data->mpost = std::move(req.multipart_body);
    req_data->method = req.method;
    req_data->attempt = req.attempt;
    return req_data;
}

string write_to_buffer(request req)
{
    auto buffer = write_to_buffer(make_request_data(std::move(req)));
    return { static_cast<const char*>(buffer->data().data()), buffer->data().size() };
}

TEST_CASE("write_to_buffer/empty", "")
{
    auto req = request::POST("/", {});
    req.body.reset();
    req.multipart_body.reset();
    auto buffer = write_to_buffer(req);

    REQUIRE(
        buffer ==
        "POST / HTTP/1.1\r\n"
        "Host: localhost\r\n"
        "Connection: close\r\n"
        "Content-Length: 0\r\n"
        "Accept: */*\r\n"
        "\r\n");
}

TEST_CASE("write_to_buffer/body/put", "")
{
    auto req = request::PUT("/", "foobar");
    auto buffer = write_to_buffer(req);

    REQUIRE(
        buffer ==
        "PUT / HTTP/1.1\r\n"
        "Host: localhost\r\n"
        "Connection: close\r\n"
        "Content-Length: 6\r\n"
        "Content-Transfer-Encoding: 8bit\r\n"
        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n"
        "Accept: */*\r\n"
        "\r\n"
        "foobar");
}

TEST_CASE("write_to_buffer/multipart/empty", "")
{
    auto req = request::MPOST("/", {});
    auto buffer = write_to_buffer(req);

    REQUIRE(
        buffer ==
        "POST / HTTP/1.1\r\n"
        "Host: localhost\r\n"
        "Connection: close\r\n" +
            CONTENT_TYPE_FORM_DATA_HDR +
            "Content-Length: 0\r\n"
            "Accept: */*\r\n"
            "\r\n");
}

TEST_CASE("write_to_buffer/multipart/mixed_with_single_part", "")
{
    auto req = request::MPOST("/", body_mixed({ json_part(string(JSON_BODY)) }));
    auto buffer = write_to_buffer(req);
    REQUIRE(Catch::contains(buffer, CONTENT_TYPE_MIXED_HDR));
    string expected_body = DELIMITER + JSON_PART + CLOSED_DELIMITER;
    REQUIRE(Catch::contains(
        buffer, "Content-Length: " + std::to_string(expected_body.length()) + "\r\n"));
    REQUIRE(Catch::endsWith(buffer, expected_body));
}

TEST_CASE("write_to_buffer/multipart/alternative_with_many_parts", "")
{
    auto json = json_part(string(JSON_BODY));
    auto rfc822 = rfc822_part(string(RFC822_BODY));
    auto req = request::MPOST("/", body_alternative({ json, rfc822 }));
    auto buffer = write_to_buffer(req);

    REQUIRE(Catch::contains(buffer, CONTENT_TYPE_ALTERNATIVE_HDR));

    string expected_body = DELIMITER + JSON_PART + DELIMITER + RFC822_PART + CLOSED_DELIMITER;
    REQUIRE(Catch::contains(
        buffer, "Content-Length: " + std::to_string(expected_body.length()) + "\r\n"));
    REQUIRE(Catch::endsWith(buffer, expected_body));
}

TEST_CASE("write_to_buffer/multipart/form_data", "")
{
    auto req =
        request::MPOST("/", body_form_data({ part("key1", "value1"), part("key2", "value2") }));
    auto buffer = write_to_buffer(req);

    REQUIRE(Catch::contains(buffer, CONTENT_TYPE_FORM_DATA_HDR));

    string expected_body = DELIMITER +
        "Content-Disposition: form-data; name=\"key1\"\r\n\r\n"
        "value1" +
        DELIMITER +
        "Content-Disposition: form-data; name=\"key2\"\r\n\r\n"
        "value2" +
        CLOSED_DELIMITER;
    REQUIRE(Catch::contains(
        buffer, "Content-Length: " + std::to_string(expected_body.length()) + "\r\n"));
    REQUIRE(Catch::endsWith(buffer, expected_body));
}

TEST_CASE("write_to_buffer/multipart/skip_part_with_empty_body", "")
{
    auto json = json_part(string(JSON_BODY));
    auto rfc822 = rfc822_part("");
    auto req = request::MPOST("/", body_alternative({ json, rfc822 }));
    auto buffer = write_to_buffer(req);

    string expected_body = DELIMITER + JSON_PART + CLOSED_DELIMITER;
    REQUIRE(Catch::contains(
        buffer, "Content-Length: " + std::to_string(expected_body.length()) + "\r\n"));
    REQUIRE(Catch::endsWith(buffer, expected_body));
}

TEST_CASE("write_to_buffer/multipart/skip_all_parts_with_empty_body", "")
{
    auto json = json_part("");
    auto rfc822 = rfc822_part("");
    auto req = request::MPOST("/", body_alternative({ json, rfc822 }));
    auto buffer = write_to_buffer(req);

    REQUIRE(Catch::contains(buffer, "Content-Length: 0\r\n"));
    REQUIRE(Catch::endsWith(buffer, "\r\n\r\n"));
}

TEST_CASE("write_to_buffer/multipart/additional_headers", "")
{
    string headers = "My-Header: value\r\n";
    auto req = request::MPOST("/", headers, body_alternative({}));
    auto buffer = write_to_buffer(req);

    REQUIRE(Catch::contains(buffer, headers));
}

TEST_CASE("write_to_buffer/multipart/content_type_priority", "")
{
    string custom_content_type = "Content-Type: text/plain\r\n";
    auto req = request::MPOST("/", custom_content_type, body_mixed({}));
    auto buffer = write_to_buffer(req);

    REQUIRE(Catch::contains(buffer, custom_content_type));
    REQUIRE(!Catch::contains(buffer, "Content-Type: multipart/mixed;"));
}
