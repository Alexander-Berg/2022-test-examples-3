#pragma once

#include <ymod_webserver/response.h>

class TFakeHttpStream
    : public ymod_webserver::http::stream
    , public boost::enable_shared_from_this<TFakeHttpStream>
{
public:
    yplatform::time_traits::timer_ptr make_timer() const override { return yplatform::time_traits::timer_ptr(); }
    boost::asio::io_service& get_io_service() override { return Io; }

    void begin_poll_connect() override {}
    void cancel_poll_connect() override {}

    void set_code(ymod_webserver::codes::code cd, const std::string& reason = "") override {
        Code = cd;
        Reason = reason;
    }
    void add_header(const std::string&, const std::string&) override {}
    void add_header(const std::string&, std::time_t) override {}
    void set_content_type(const std::string& type, const std::string& subtype) override {
        ContentType = type;
        ContentSubType = subtype;
    }
    void set_content_type(const std::string& type) override { ContentType = type; }
    void set_cache_control(ymod_webserver::cache_response_header, const std::string& = "") override {}
    void set_connection(bool) override {}

    void result_body(const std::string& body) override { Body = body; }
    yplatform::net::streamable_ptr result_stream(const std::size_t) override { return yplatform::net::streamable_ptr(); }
    yplatform::net::streamable_ptr result_chunked() override { return yplatform::net::streamable_ptr(); }

    void add_error_handler(const error_handler&) override {}
    void on_error(const boost::system::error_code& e) override { LastError = e; }
    ymod_webserver::request_ptr request() const override { return Request; }
    ymod_webserver::context_ptr ctx() const override { return Request->ctx(); }

    ymod_webserver::codes::code result_code() const override { return Code; }

    bool is_secure() const override { return false; }
    const boost::asio::ip::address& remote_addr() const override { return Address; }

    // streamable
    void send_client_stream(const yplatform::net::buffers::const_chunk_buffer&) override {}
    yplatform::net::streamer_wrapper client_stream() override {
        return yplatform::net::streamer_wrapper(new yplatform::net::streamer<TFakeHttpStream>(shared_from_this()));
    }
    bool is_open() const override { return false; }

public:
    boost::asio::io_service Io;
    ymod_webserver::codes::code Code;
    std::string Reason;
    std::string ContentType;
    std::string ContentSubType;
    std::string Body;
    boost::system::error_code LastError;
    ymod_webserver::request_ptr Request = boost::make_shared<ymod_webserver::request>();
    boost::asio::ip::address Address = boost::asio::ip::make_address("127.0.0.1");
};
