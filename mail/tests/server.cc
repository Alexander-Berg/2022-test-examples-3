#include <cstdlib>
#include <iostream>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/thread.hpp>

#include "mem_alloc.h"

using namespace std;

typedef boost::asio::ssl::stream<boost::asio::ip::tcp::socket> ssl_socket;

class session
{
public:
    session(boost::asio::io_service& io_service, boost::asio::ssl::context& context)
        : socket_(io_service, context)
    {
    }

    ssl_socket::lowest_layer_type& socket()
    {
        return socket_.lowest_layer();
    }

    void start()
    {
        socket_.async_handshake(
            boost::asio::ssl::stream_base::server,
            make_custom_alloc_handler(
                alloc_,
                boost::bind(&session::handle_handshake, this, boost::asio::placeholders::error)));
    }

    void handle_handshake(const boost::system::error_code& error)
    {
        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(data_, max_length),
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(
                        &session::handle_read,
                        this,
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::bytes_transferred)));
        }
        else
        {
            cout << "handle_handshake error: " << error.message() << endl;
            delete this;
        }
    }

    void handle_read(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            boost::asio::async_write(
                socket_,
                boost::asio::buffer(data_, bytes_transferred),
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(&session::handle_write, this, boost::asio::placeholders::error)));
        }
        else
        {
            delete this;
        }
    }

    void handle_write(const boost::system::error_code& error)
    {
        delete this;
        return;

        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(data_, max_length),
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(
                        &session::handle_read,
                        this,
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::bytes_transferred)));
        }
        else
        {
            delete this;
        }
    }

private:
    ssl_socket socket_;
    enum
    {
        max_length = 1024
    };
    char data_[max_length];

    handler_allocator<> alloc_;
};

class server
{
public:
    server(boost::asio::io_service& io_service, unsigned short port)
        : io_service_(io_service)
        , acceptor_(io_service, boost::asio::ip::tcp::endpoint(boost::asio::ip::tcp::v4(), port))
        , context_(boost::asio::ssl::context::sslv23)
    {
        context_.set_options(
            boost::asio::ssl::context::default_workarounds | boost::asio::ssl::context::no_sslv2 |
            boost::asio::ssl::context::single_dh_use);
        //    context_.set_password_callback(boost::bind(&server::get_password, this));
        context_.use_certificate_chain_file("server.crt");
        context_.use_private_key_file("server.key.insecure", boost::asio::ssl::context::pem);
        context_.use_tmp_dh_file("dh512.pem");
        context_.set_verify_mode(boost::asio::ssl::verify_none);
        start_accept();
    }

    std::string get_password() const
    {
        return "test";
    }

    void start_accept()
    {
        session* new_session = new session(io_service_, context_);
        acceptor_.async_accept(
            new_session->socket(),
            boost::bind(
                &server::handle_accept, this, new_session, boost::asio::placeholders::error));
    }

    void handle_accept(session* new_session, const boost::system::error_code& error)
    {
        if (!error)
        {
            new_session->start();
        }
        else
        {
            delete new_session;
        }

        start_accept();
    }

private:
    boost::asio::io_service& io_service_;
    boost::asio::ip::tcp::acceptor acceptor_;
    boost::asio::ssl::context context_;
};

int main(int argc, char* argv[])
{
    try
    {
        if (argc != 2)
        {
            std::cerr << "Usage: server <port>\n";
            return 1;
        }

        boost::asio::io_service io_service;

        using namespace std; // For atoi.
        server s(io_service, atoi("9990"));

        boost::thread_group tg;

        //    io_service.run();
        for (int i = 0; i < 2; ++i)
        {
            tg.create_thread(boost::bind(&boost::asio::io_service::run, &io_service));
        }

        tg.join_all();
    }
    catch (std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
    }

    return 0;
}
