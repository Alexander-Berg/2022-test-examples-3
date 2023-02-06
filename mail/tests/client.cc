#include <cstdlib>
#include <iostream>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/thread.hpp>
#include <boost/make_shared.hpp>

#include "ntimer.h"
#include "mem_alloc.h"

enum
{
    max_length = 1024
};

class client
{
public:
    client(
        boost::asio::io_service& io_service,
        boost::asio::ssl::context& context,
        boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
        : io_(io_service), context_(context)
    {
        endpoint_iterator_ = endpoint_iterator;

        start();
    }

    void handle_connect(const boost::system::error_code& error)
    {
        if (!error)
        {
            socket_->async_handshake(
                boost::asio::ssl::stream_base::client,
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(
                        &client::handle_handshake, this, boost::asio::placeholders::error)));
        }
        else
        {
            std::cout << "Connect failed: " << error.message() << "\n";
            start();
        }
    }

    void start()
    {
        if (socket_.get()) return;
        socket_.reset(new boost::asio::ssl::stream<boost::asio::ip::tcp::socket>(io_, context_));
        socket_->set_verify_mode(boost::asio::ssl::verify_none);

        ntimer_.start();
        boost::asio::async_connect(
            socket_->lowest_layer(),
            endpoint_iterator_,
            boost::bind(&client::handle_connect, this, boost::asio::placeholders::error));
    }

    void handle_handshake(const boost::system::error_code& error)
    {
        if (!error)
        {
            auto time = ntimer_.stop();
            std::cout << "time:" << time / 1000.0 << "\n";

            memcpy(request_, "1234567890\0", 11);
            size_t request_length = strlen(request_);

            boost::asio::async_write(
                *socket_,
                boost::asio::buffer(request_, request_length),
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(
                        &client::handle_write,
                        this,
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::bytes_transferred)));
        }
        else
        {
            std::cout << "Handshake failed: " << error.message() << "\n";
            start();
        }
    }

    void handle_write(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            boost::asio::async_read(
                *socket_,
                boost::asio::buffer(reply_, bytes_transferred),
                make_custom_alloc_handler(
                    alloc_,
                    boost::bind(
                        &client::handle_read,
                        this,
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::bytes_transferred)));
        }
        else
        {
            std::cout << "Write failed: " << error.message() << "\n";
            start();
        }
    }

    void handle_read(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            //      std::cout << "Reply: ";
            //      std::cout.write(reply_, bytes_transferred);
            //      std::cout << "\n";
        }
        else
        {
            std::cout << "Read failed: " << error.message() << "\n";
        }
        start();
    }

private:
    boost::asio::io_service& io_;
    boost::asio::ssl::context& context_;

    std::auto_ptr<boost::asio::ssl::stream<boost::asio::ip::tcp::socket>> socket_;
    char request_[max_length];
    char reply_[max_length];
    boost::asio::ip::tcp::resolver::iterator endpoint_iterator_;

    handler_allocator<> alloc_;
    ntimer_t ntimer_;
};

int main(int argc, char* argv[])
{
    try
    {
        if (argc != 2)
        {
            std::cerr << "Usage: client <port>\n";
            return 1;
        }

        boost::asio::io_service io_service;

        boost::asio::ip::tcp::resolver resolver(io_service);
        boost::asio::ip::tcp::resolver::query query("127.0.0.1", "9990");
        boost::asio::ip::tcp::resolver::iterator iterator = resolver.resolve(query);

        boost::asio::ssl::context ctx(boost::asio::ssl::context::sslv23);
        //    ctx.load_verify_file("ca.pem");
        ctx.set_verify_mode(boost::asio::ssl::verify_none);

        std::auto_ptr<boost::asio::io_service::work> work(
            new boost::asio::io_service::work(io_service));

        boost::thread_group tg;

        for (int i = 0; i < 2; ++i)
        {
            tg.create_thread(boost::bind(&boost::asio::io_service::run, &io_service));
        }

        std::vector<boost::shared_ptr<client>> clients;

        for (int i = 0; i < 2; ++i)
        {
            clients.push_back(boost::make_shared<client>(io_service, ctx, iterator));
        }

        work.reset();
        tg.join_all();
    }
    catch (std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
    }

    return 0;
}
