#include <iostream>
#include <string>
#include <boost/shared_ptr.hpp>

#include <boost/log/utility/init/from_stream.hpp>

#include <boost/system/error_code.hpp>
#include <boost/program_options.hpp>
#include <yplatform/net/line_filter.h>
#include <yplatform/net/client.h>
#include <yplatform/net/byte_order.h>
#include <ymod_messenger/types.h>
#include "../src/session/client_session.h"

using namespace yplatform;
using std::string;
using std::cout;
using std::endl;
using boost::shared_ptr;
using boost::system::error_code;
using namespace boost::asio;
using boost::asio::ip::tcp;
using yplatform::net::io_pool;

typedef yplatform::zerocopy::streambuf buffer_t;
typedef yplatform::zerocopy::segment segment_t;
typedef shared_ptr<buffer_t> buffer_ptr;
typedef yplatform::net::output_buffer_sequence<yplatform::net::buffers::const_chunk_buffer>
    output_queue_t;

typedef net::client<ymod_messenger::client_session> client_t;

class server_session
{
public:
    server_session(tcp::socket&& socket) : socket_(std::move(socket))
    {
    }

    void start()
    {
        start_read();
    }

private:
    void start_read()
    {
        socket_.async_read_some(
            boost::asio::buffer(data_, max_length),
            [this](boost::system::error_code const& ec, size_t length) {
                if (!ec)
                {
                    if (length == 0) return;
                    boost::mutex::scoped_lock lock(m);
                    queue_.push(length);
                    lock.unlock();
                    start_write();
                    start_read();
                }
                else
                {
                    std::cerr << "read err: " << ec.message() << std::endl;
                }
            });
    }
    tcp::socket socket_;
    enum
    {
        max_length = 1024
    };
    char data_[max_length];
};

class server
{
public:
    server(io_pool& pool, short port)
        : pool_(pool), acceptor_(*pool.io(), tcp::endpoint(tcp::v4(), port)), socket_(*pool.io())
    {
    }

    void start()
    {
        do_accept();
    }

private:
    void do_accept()
    {
        acceptor_.async_accept(socket_, [this](boost::system::error_code ec) {
            if (!ec)
            {
                std::shared_ptr<server_session> sess =
                    std::make_shared<server_session>(std::move(socket_));
                sess->start();
                reset_socket();
            }
            else
            {
                std::cerr << "accept err: " << ec.message() << std::endl;
            }

            do_accept();
        });
    }

    void reset_socket()
    {
        socket_ = std::move(tcp::socket(*pool_.io()));
    }

    io_pool& pool_;
    tcp::acceptor acceptor_;
    tcp::socket socket_;
};

class client_session
{
public:
    client_session(io_service& io, tcp::socket&& socket)
        : io_(io), socket_(std::move(socket)), strand_(io)
    {
        repo.retain();
        rounds_ = 0;
        queue_.reserve(1000);
        write_active_ = false;
    }

    void close()
    {
        socket_.close();
    }

    void send(unsigned msg_size)
    {
        //        boost::mutex::scoped_lock lock(mutex);

        //        safe_send(msg_size);
        strand_.dispatch(
            //            make_custom_alloc_handler(read_alloc_,
            boost::bind(&session::safe_send, shared_from_this(), msg_size))
            //        )
            ;
    }

    uint32_t rounds() const
    {
        return rounds_;
    }

    void start_read()
    {
        auto self = shared_from_this();
        socket_.async_read_some(
            boost::asio::buffer(data_, max_length),
            strand_.wrap(
                //            mutex_wrap(
                make_custom_alloc_handler(
                    read_alloc_,
                    [this, self](boost::system::error_code const& ec, size_t length) {
                        if (!ec)
                        {
                            rounds_++;
                            safe_send(length);
                            start_read();
                        }
                        else
                        {
                            std::clog << "err read" << endl;
                        }
                    })
                //                        , mutex
                ));
        ++rounds_;
    }

private:
    void safe_send(unsigned msg_size)
    {
        boost::mutex::scoped_lock lock(m);
        queue_.push_back(msg_size);
        lock.unlock();
        start_write();
    }

    void start_write()
    {
        if (write_active_) return;

        boost::mutex::scoped_lock lock(m);
        if (queue_.size() == 0) return;
        unsigned length = queue_.front();
        queue_.erase(queue_.begin());
        lock.unlock();

        auto self = shared_from_this();
        write_active_ = true;
        send_timer_.start();
        boost::asio::async_write(
            socket_,
            boost::asio::buffer(data_, length),
            strand_.wrap(make_custom_alloc_handler(
                write_alloc_, [this, self](boost::system::error_code const& ec, size_t length) {
                    write_active_ = false;
                    send_time_(send_timer_.stop());
                    if (!ec)
                    {
                        start_write();
                    }
                    else
                    {
                        std::clog << "err write" << endl;
                    }
                })));
    }

    io_service& io_;
    tcp::socket socket_;
    strand strand_;
    uint32_t rounds_;
    boost::mutex m;
    handler_allocator strand_alloc_;
    handler_allocator read_alloc_;
    handler_allocator write_alloc_;

    enum
    {
        max_length = 1024
    };
    char data_[max_length];
    std::vector<unsigned> queue_;
    bool write_active_;
    boost::mutex mutex;

    ntimer_t send_timer_;
    accumulator_t send_time_;
};

struct options_t
{
    unsigned count;
    unsigned size;
};

options_t parse_options(int argc, char** argv)
{
    options_t result;
    namespace po = boost::program_options;
    po::options_description description("allowed options");
    description.add_options()("help,h", "produce help message")(
        "size,s", po::value<unsigned>(&result.size)->default_value(1024), "message size")(
        "count,c", po::value<unsigned>(&result.count)->default_value(1000), "messages count");
    po::variables_map options;
    try
    {
        po::store(po::parse_command_line(argc, argv, description), options);
        po::notify(options);
    }
    catch (const std::exception& e)
    {
        std::cout << "command line error: " << e.what() << std::endl;
        exit(1);
    }
    if (options.empty() || options.count("help"))
    {
        std::cout << description << std::endl;
        exit(0);
    }

    return result;
}

int main(int argc, char** argv)
{
    options_t opts = parse_options(argc, argv);

    init_log();

    auto random_data = generate_data(opts.size, opts.count);

    io_pool server_pool(new io_pool());
    server_pool.init(0, 1, 1);
    server server_instance(server_pool, 12399);
    server_instance.start();

    io_pool client_pool(new io_pool());
    client_pool.init(0, 1, 1);

    cout << "ready to run" << endl;

    server_pool.run();
    client_pool.run();

    // connect
    io_service& io = *client_pool.io();
    tcp::socket socket(io);
    tcp::endpoint ep(ip::address::from_string("127.0.0.1"), 12399);
    boost::system::error_code ec1, ec2;
    ec2 = socket.connect(ep, ec1);
    if (ec1)
    {
        std::cerr << ec1.message() << std::endl;
    }
    if (ec2)
    {
        std::cerr << ec2.message() << std::endl;
    }
    client_session client_sess(io, std::move(socket));

    //
    client_sess.async_close();

    sleep(1);
    server_pool.stop();
    client_pool.stop();

    return 0;
}