#include <cstdlib>
#include <iostream>
#include <fstream>
#include <string>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/thread.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/make_shared.hpp>

enum
{
    max_length = 1024
};

using std::string;

class client_ssl
{
public:
    client_ssl(
        boost::asio::io_service& io_service,
        boost::asio::ssl::context& context,
        boost::asio::ip::tcp::resolver::iterator endpoint_iterator,
        const string& uid,
        const string& ts,
        const string& sign)

        : socket_(io_service, context)
    {
        socket_.set_verify_mode(boost::asio::ssl::verify_none);
        boost::asio::async_connect(
            socket_.lowest_layer(),
            endpoint_iterator,
            boost::bind(&client_ssl::handle_connect, this, boost::asio::placeholders::error));

        uid_ = uid;

        request_ = "GET /events/sse?uid=";
        request_ += uid;
        request_ += "&ts=";
        request_ += ts;
        request_ += "&sign=";
        request_ += sign;
        request_ += "&service=mail&format=json&client_id=client_subscribe_test HTTP/1.1\n\n";
    }

    void handle_connect(const boost::system::error_code& error)
    {
        if (!error)
        {
            std::cout << "async_handshake\n";

            socket_.async_handshake(
                boost::asio::ssl::stream_base::client,
                boost::bind(&client_ssl::handle_handshake, this, boost::asio::placeholders::error));
        }
        else
        {
            std::cout << uid_ << " connect failed: " << error.message() << "\n";
        }
    }

    void handle_handshake(const boost::system::error_code& error)
    {
        std::cout << "handle_handshake\n";

        if (!error)
        {
            boost::asio::async_write(
                socket_,
                boost::asio::buffer(request_.data(), request_.size()),
                boost::bind(
                    &client_ssl::handle_write,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " handshake failed: " << error.message() << "\n";
        }
    }

    void handle_write(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(reply_, max_length),
                boost::bind(
                    &client_ssl::handle_read,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " write failed: " << error.message() << "\n";
        }
    }

    void handle_read(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(reply_, max_length),
                boost::bind(
                    &client_ssl::handle_read,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " read failed: " << error.message() << "\n";
        }
    }

private:
    string uid_;
    boost::asio::ssl::stream<boost::asio::ip::tcp::socket> socket_;
    string request_;
    char reply_[max_length];
};

class client
{
public:
    client(
        boost::asio::io_service& io_service,
        boost::asio::ip::tcp::resolver::iterator endpoint_iterator,
        const string& uid,
        const string& ts,
        const string& sign)

        : socket_(io_service)
    {
        boost::asio::async_connect(
            socket_.lowest_layer(),
            endpoint_iterator,
            boost::bind(&client::handle_connect, this, boost::asio::placeholders::error));

        uid_ = uid;

        request_ = "GET /events/sse?uid=";
        request_ += uid;
        request_ += "&ts=";
        request_ += ts;
        request_ += "&sign=";
        request_ += sign;
        request_ += "&service=fake&format=json&client_id=client_subscribe_test HTTP/1.1\n\n";
    }

    void handle_connect(const boost::system::error_code& error)
    {
        if (!error)
        {
            boost::asio::async_write(
                socket_,
                boost::asio::buffer(request_.data(), request_.size()),
                boost::bind(
                    &client::handle_write,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " connect failed: " << error.message() << "\n";
        }
    }

    void handle_write(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(reply_, max_length),
                boost::bind(
                    &client::handle_read,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " write failed: " << error.message() << "\n";
        }
    }

    void handle_read(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            socket_.async_read_some(
                boost::asio::buffer(reply_, max_length),
                boost::bind(
                    &client::handle_read,
                    this,
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << uid_ << " read failed: " << error.message() << "\n";
        }
    }

private:
    string uid_;
    boost::asio::ip::tcp::socket socket_;
    string request_;
    char reply_[max_length];
};

struct params_t
{
    string uid;
    string ts;
    string sign;
    string mdb;
};

std::vector<params_t> load_params(const string& file_name)
{
    std::vector<params_t> result;
    result.reserve(50000);
    std::ifstream f(file_name);
    while (f.good())
    {
        params_t params;
        f >> params.uid;
        f >> params.ts;
        f >> params.sign;
        f >> params.mdb;
        if (params.uid != "") result.push_back(params);
    }
    f.close();
    return result;
}

int main(int argc, char* argv[])
{
    try
    {
        if (argc != 4)
        {
            std::cerr << "Usage: client <server> <port> <uids_file>\n";
            return 1;
        }

        boost::asio::io_service io_service;

        string server = argv[1];
        string port = argv[2];
        string params_file = argv[3];

        std::vector<params_t> params = load_params(params_file);

        boost::asio::ip::tcp::resolver resolver(io_service);
        boost::asio::ip::tcp::resolver::query query(server, port);
        boost::asio::ip::tcp::resolver::iterator iterator = resolver.resolve(query);

        boost::asio::ssl::context ctx(boost::asio::ssl::context::sslv23);
        ctx.set_verify_mode(boost::asio::ssl::verify_none);

        std::auto_ptr<boost::asio::io_service::work> work(
            new boost::asio::io_service::work(io_service));

        boost::thread_group tg;

        for (int i = 0; i < 6; ++i)
        {
            tg.create_thread(boost::bind(&boost::asio::io_service::run, &io_service));
        }

        std::vector<boost::shared_ptr<client>> clients;

        //    std::set<string> seen_mdbs;

        for (int i = 0; i < params.size(); ++i)
        {
            auto c = boost::make_shared<client>(
                io_service, iterator, params[i].uid, params[i].ts, params[i].sign);
            if (i && i % 500 == 0)
            {
                sleep(1);
            }
            clients.push_back(c);
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
