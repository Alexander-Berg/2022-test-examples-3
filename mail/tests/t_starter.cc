#include <boost/make_shared.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include "stub_net_session.h"
#include "stub_net_server.h"
#include <starter.h>
#include <ymod_webserver/settings.h>
#include <catch.hpp>

namespace ymod_webserver {

namespace {

void do_nothing_ec(boost::system::error_code const&)
{
}
}

struct t_starter
{

    t_starter() : session(new stub_session()), server(new stub_server)
    {
        settings.policy_file = "policy file content";
    }

    boost::shared_ptr<ymod_webserver::starter> create_starter(
        ymod_webserver::handler_ptr handler = nullptr)
    {
        endpoint ep;
        settings.endpoints.insert(std::make_pair("", ep));
        server->handler_ = handler;
        return boost::make_shared<ymod_webserver::starter>(session->io_, server, session, settings);
    }

    ymod_webserver::settings settings;
    boost::shared_ptr<stub_session> session;
    boost::shared_ptr<stub_server> server;
};

TEST_CASE_METHOD(t_starter, "starter/init/positive", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();
    REQUIRE(session->read_is_active());
    REQUIRE(!server->finished_);
}

TEST_CASE_METHOD(t_starter, "starter/init/close", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();
    REQUIRE(session->read_is_active());
    session->async_close(do_nothing_ec);
    REQUIRE(server->starter_failed_);
}

TEST_CASE_METHOD(t_starter, "starter/get/1", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("GET / HTTP/1.1\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(session->buffers.write_buffer.size() == 0);

    session->buffers.read_buffer.append("\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
}

TEST_CASE_METHOD(t_starter, "starter/get/1-correct-destroy", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("GET / HTTP/1.1\n\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(!session->read_is_active());

    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/get-headers/connection_reset", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("GET / HTTP/1.1");
    session->complete_read();
    session->io_.poll();

    REQUIRE(session->read_is_active());

    session->complete_read(boost::asio::error::connection_reset);
    session->io_.poll();

    REQUIRE(session->buffers.write_buffer.size() == 0);
    REQUIRE(server->starter_failed_);
}

TEST_CASE_METHOD(t_starter, "starter/policy-request", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("<policy-file-request/>\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(!session->read_is_active());

    session->io_.poll();

    REQUIRE(session->buffers.write_buffer == settings.policy_file);
    REQUIRE(server->starter_finished_);
}

TEST_CASE_METHOD(t_starter, "starter/bad-method", "")
{
    using boost::algorithm::starts_with;
    using boost::algorithm::ends_with;

    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("GETGETGET\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(!session->read_is_active());

    session->io_.poll();

    REQUIRE(starts_with(session->buffers.write_buffer, "HTTP/1.0 400 BadRequest\r\n"));
    REQUIRE(ends_with(session->buffers.write_buffer, "\r\n\r\nBadRequest"));

    REQUIRE(server->starter_finished_);
}

TEST_CASE_METHOD(t_starter, "starter/bad-uri", "")
{
    using boost::algorithm::starts_with;
    using boost::algorithm::ends_with;

    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("GET http:|\n");
    session->complete_read();

    REQUIRE(!session->read_is_active());
    REQUIRE(starts_with(session->buffers.write_buffer, "HTTP/1.0 400 BadRequest\r\n"));
    REQUIRE(ends_with(session->buffers.write_buffer, "\r\n\r\nBadRequest"));
    REQUIRE(server->starter_finished_);
}

TEST_CASE_METHOD(t_starter, "starter/big-request-line", "")
{
    using boost::algorithm::starts_with;
    using boost::algorithm::ends_with;

    const std::string big_uri("/big");
    const std::string get_string("GET " + big_uri);
    settings.max_request_line_size = static_cast<unsigned int>(get_string.size() - 1);

    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append(get_string);
    session->complete_read();

    REQUIRE(!session->read_is_active());
    REQUIRE(starts_with(session->buffers.write_buffer, "HTTP/1.0 414 RequestUriTooLong\r\n"));
    REQUIRE(ends_with(session->buffers.write_buffer, "\r\n\r\nRequestUriTooLong"));
    REQUIRE(server->starter_finished_);
}

TEST_CASE_METHOD(t_starter, "starter/too-large-headers", "")
{
    using boost::algorithm::starts_with;
    using boost::algorithm::ends_with;

    const std::string get_string("GET / HTTP/1.0\r\n");
    const std::string headers("X-Header: value");
    settings.max_request_line_size = static_cast<unsigned>(get_string.size());
    settings.max_headers_size = static_cast<unsigned>(headers.size() - 1);

    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append(get_string);
    session->buffers.read_buffer.append(headers);
    session->complete_read();

    REQUIRE(!session->read_is_active());
    CAPTURE(session->buffers.write_buffer);
    REQUIRE(starts_with(session->buffers.write_buffer, "HTTP/1.0 413 RequestEntityTooLarge\r\n"));
    REQUIRE(ends_with(session->buffers.write_buffer, "\r\n\r\nRequestEntityTooLarge"));
    REQUIRE(server->starter_finished_);
}

class post_handler : public handler
{
    void execute(request_ptr req, response_ptr stream) final
    {
        REQUIRE(req);
        REQUIRE(stream);
        REQUIRE(req->method == ymod_webserver::methods::mth_post);
        REQUIRE(std::string(req->raw_body.begin(), req->raw_body.end()) == "body");
        stream->set_code(ymod_webserver::codes::ok);
        stream->set_content_type("text", "plain");
        stream->result_body("ok");
    }
};

TEST_CASE_METHOD(t_starter, "starter/post/with_content_length/single", "")
{
    {
        auto starter = create_starter(boost::make_shared<post_handler>());
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("POST / HTTP/1.1\r\n"
                                        "Host: localhost:9999\r\n"
                                        "Content-Length: 4\r\n"
                                        "\r\n"
                                        "body\r\n");
    session->complete_read();

    REQUIRE(session->buffers.write_buffer.size() > 0);
    REQUIRE(!session->read_is_active());
    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/post/with_content_length/separate_headers_and_body", "")
{
    {
        auto starter = create_starter(boost::make_shared<post_handler>());
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("POST / HTTP/1.1\r\n"
                                        "Host: localhost:9999\r\n"
                                        "Content-Length: 4\r\n"
                                        "\r\n");
    session->complete_read();

    session->buffers.read_buffer.append("body\r\n");
    session->complete_read();

    REQUIRE(session->buffers.write_buffer.size() > 0);
    REQUIRE(!session->read_is_active());
    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/post/with_content_length/split_body", "")
{
    {
        auto starter = create_starter(boost::make_shared<post_handler>());
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("POST / HTTP/1.1\r\n"
                                        "Host: localhost:9999\r\n"
                                        "Content-Length: 4\r\n"
                                        "\r\n");
    session->complete_read();

    session->buffers.read_buffer.append("bo");
    session->complete_read();

    session->buffers.read_buffer.append("dy\r\n");
    session->complete_read();

    REQUIRE(session->buffers.write_buffer.size() > 0);
    REQUIRE(!session->read_is_active());
    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/post/chunked/single", "")
{
    {
        auto starter = create_starter(boost::make_shared<post_handler>());
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("POST / HTTP/1.1\r\n"
                                        "Host: localhost:9999\r\n"
                                        "Transfer-Encoding: ChUnKeD\r\n"
                                        "\r\n"
                                        "4\r\n"
                                        "body\r\n"
                                        "0\r\n"
                                        "\r\n");
    session->complete_read();

    REQUIRE(session->buffers.write_buffer.size() > 0);
    REQUIRE(!session->read_is_active());
    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/post/chunked/split_body", "")
{
    {
        auto starter = create_starter(boost::make_shared<post_handler>());
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("POST / HTTP/1.1\r\n"
                                        "Host: localhost:9999\r\n"
                                        "Transfer-Encoding: chunked\r\n"
                                        "\r\n"
                                        "2\r\n"
                                        "bo\r\n");
    session->complete_read();

    session->buffers.read_buffer.append("2\r\n"
                                        "dy\r\n"
                                        "0\r\n"
                                        "\r\n");
    session->complete_read();

    REQUIRE(session->buffers.write_buffer.size() > 0);
    REQUIRE(!session->read_is_active());
    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
    REQUIRE(server->starter_destroyed_);
}

TEST_CASE_METHOD(t_starter, "starter/ignores_newline_before_request_line", "")
{
    {
        auto starter = create_starter();
        starter->run();
    }
    session->io_.poll();

    session->buffers.read_buffer.append("\r\n"
                                        "\r\n"
                                        "\r\n"
                                        "\r\n"
                                        "GET / HTTP/1.1\r\n"
                                        "\r\n");
    session->complete_read();
    session->io_.poll();

    REQUIRE(server->finished_);
    REQUIRE(server->execute_http_);
}

}
