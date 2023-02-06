#include "socket_mock.h"
#include <yplatform/net/stream/detail/reduce_ssl_buffers.h>
#include <catch.hpp>

using namespace yplatform;

const size_t REDUCED_SIZE = 8 * 1024;
const string CERT_PATH = "data/cert.pem";
const string KEY_PATH = "data/key.pem";

using fake_stream_core_tag = private_access_tag<
    boost::asio::ssl::stream<socket_mock&>,
    boost::asio::ssl::detail::stream_core>;

namespace yplatform {

template struct private_access<
    fake_stream_core_tag,
    &boost::asio::ssl::stream<socket_mock&>::core_>;

}

namespace yplatform { namespace net { namespace stream { namespace detail {

// Specialize get_core function to allow reduce_ssl_buffers work with socket_mock.
template <>
inline ssl_stream_core* get_core(ssl_stream<socket_mock&>& stream)
{
    return &get_field<fake_stream_core_tag>(stream);
}

}}}}

struct t_reduce_ssl_buffers
{
    using ssl_stream = boost::asio::ssl::stream<socket_mock&>;

    t_reduce_ssl_buffers()
        : server_socket(io)
        , client_socket(io)
        , ssl_context(make_ssl_context())
        , server_stream(server_socket, ssl_context)
        , client_stream(client_socket, ssl_context)
    {
        server_socket.logger().set_log_prefix("server_socket");
        client_socket.logger().set_log_prefix("client_socket");
        connect_sockets(server_socket, client_socket);
        run_handshake();
    }

    boost::asio::ssl::context make_ssl_context()
    {
        boost::asio::ssl::context::method method = boost::asio::ssl::context::sslv23;
        boost::asio::ssl::context ret(method);
        boost::asio::ssl::context::verify_mode verify_mode = 0;
        ret.set_verify_mode(verify_mode);
        ret.use_certificate_chain_file(CERT_PATH);
        ret.use_private_key_file(KEY_PATH, boost::asio::ssl::context::pem);
        return ret;
    }

    void run_io()
    {
        io.reset();
        io.run();
    }

    void run_handshake()
    {
        bool client_handshake_finished = false, server_handshake_finished = false;
        client_stream.async_handshake(
            boost::asio::ssl::stream_base::handshake_type::client,
            [&](boost::system::error_code ec) {
                if (ec) throw std::runtime_error("client handshake error: " + ec.message());
                YLOG_G(info) << "client handshake finished";
                client_handshake_finished = true;
            });
        server_stream.async_handshake(
            boost::asio::ssl::stream_base::handshake_type::server,
            [&](boost::system::error_code ec) {
                if (ec) throw std::runtime_error("server handshake error: " + ec.message());
                YLOG_G(info) << "server handshake finished";
                server_handshake_finished = true;
            });
        run_io();
        if (!client_handshake_finished) throw std::runtime_error("client handshake not finished");
        if (!server_handshake_finished) throw std::runtime_error("server handshake not finished");
    }

    void send_to_stream(string msg, ssl_stream& stream)
    {
        bool finished = false;
        YLOG_G(info) << "write msg with " << msg.length() << " bytes";
        boost::asio::async_write(
            stream,
            make_buffer_sequence({ &msg }),
            [&](boost::system::error_code ec, std::size_t bytes) mutable {
                if (ec) throw std::runtime_error("send error: " + ec.message());
                YLOG_G(info) << bytes << " bytes was written";
                if (bytes != msg.size())
                {
                    YLOG_G(error) << "send only " << bytes << " of " << msg.size() << " bytes";
                    throw std::runtime_error("send error: write not all message");
                }
                finished = true;
            });
        run_io();
        if (!finished) throw std::runtime_error("send not finished");
    }

    string receive_from_stream(ssl_stream& stream, size_t bytes)
    {
        string msg;
        msg.resize(bytes);
        bool finished = false;
        boost::asio::async_read(
            stream,
            make_buffer_sequence({ &msg }),
            [&](boost::system::error_code ec, std::size_t received_bytes) mutable {
                if (ec) throw std::runtime_error("receive error: " + ec.message());
                if (received_bytes != bytes)
                    throw std::runtime_error(
                        "receive only " + std::to_string(received_bytes) + " bytes");
                finished = true;
            });
        run_io();
        if (!finished) throw std::runtime_error("receive not finished");
        return msg;
    }

    void send_on_client(const string& msg)
    {
        send_to_stream(msg, client_stream);
    }

    void send_on_server(const string& msg)
    {
        send_to_stream(msg, server_stream);
    }

    string receive_on_client(size_t bytes)
    {
        return receive_from_stream(client_stream, bytes);
    }

    string receive_on_server(size_t bytes)
    {
        return receive_from_stream(server_stream, bytes);
    }

    std::vector<boost::asio::mutable_buffer> make_buffer_sequence(std::vector<string*> v)
    {
        std::vector<boost::asio::mutable_buffer> ret;
        for (auto& s : v)
        {
            ret.push_back(boost::asio::mutable_buffer((void*)s->data(), s->size()));
        }
        return ret;
    }

    boost::asio::ssl::detail::stream_core& get_stream_core(ssl_stream& stream)
    {
        auto core = yplatform::net::stream::detail::get_core(stream);
        if (!core) throw std::runtime_error("stream_core not found");
        return *core;
    }

    std::pair<BIO*, BIO*> get_bio_pair(ssl_stream& stream)
    {
        auto& core = get_stream_core(stream);
        auto ssl = core.engine_.native_handle();
        auto int_bio = SSL_get_rbio(ssl);
        auto ext_bio = yplatform::get_field<net::stream::detail::ext_bio_tag>(core.engine_);
        return std::make_pair(int_bio, ext_bio);
    }

    boost::asio::io_service io;
    socket_mock server_socket;
    socket_mock client_socket;
    boost::asio::ssl::context ssl_context;
    ssl_stream server_stream;
    ssl_stream client_stream;
};

TEST_CASE_METHOD(t_reduce_ssl_buffers, "reduce_stream_core_buffers")
{
    yplatform::net::stream::detail::reduce_ssl_buffers(server_stream, REDUCED_SIZE);
    auto& core = get_stream_core(server_stream);
    REQUIRE(core.input_buffer_space_.size() == REDUCED_SIZE);
    REQUIRE(core.output_buffer_space_.size() == REDUCED_SIZE);
}

TEST_CASE_METHOD(t_reduce_ssl_buffers, "reduce_bio_buffers")
{
    yplatform::net::stream::detail::reduce_ssl_buffers(server_stream, REDUCED_SIZE);
    auto [int_bio, ext_bio] = get_bio_pair(server_stream);
    REQUIRE(BIO_get_write_buf_size(int_bio, 0) == REDUCED_SIZE);
    REQUIRE(BIO_get_write_buf_size(ext_bio, 0) == REDUCED_SIZE);
}

TEST_CASE_METHOD(t_reduce_ssl_buffers, "send_after_reduce")
{
    yplatform::net::stream::detail::reduce_ssl_buffers(server_stream, REDUCED_SIZE);
    size_t size = GENERATE(1, 10, 100, 1000, 10000, 100000);
    string msg(size, 'x');
    send_on_server(msg);
    auto received_msg = receive_on_client(size);
    REQUIRE(received_msg.length() == msg.length());
    REQUIRE(received_msg == msg);
}

TEST_CASE_METHOD(t_reduce_ssl_buffers, "receive_after_reduce")
{
    yplatform::net::stream::detail::reduce_ssl_buffers(server_stream, REDUCED_SIZE);
    size_t size = GENERATE(1, 10, 100, 1000, 10000, 100000);
    string msg(size, 'x');
    send_on_client(msg);
    auto received_msg = receive_on_server(size);
    REQUIRE(received_msg.length() == msg.length());
    REQUIRE(received_msg == msg);
}
