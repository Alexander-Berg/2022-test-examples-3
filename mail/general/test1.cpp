#include <iostream>
#include <chrono>
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/tuple/tuple.hpp>
#include <boost/fusion/adapted/boost_tuple.hpp>
#include <apq/connection.hpp>
#include <apq/connection_pool.hpp>
#include <apq/serialize_composite.hpp>

const char* conninfo = "host=localhost user=tester password=12345678 "
                       "dbname=testdb port=5432";

struct tester
{
    explicit tester(boost::asio::io_service& ios, int n) : conn_(ios), n_(n)
    {
    }

    void run()
    {
        std::cout << "tester::run() this=" << this << std::endl;
        std::cout << "\tn=" << n_ << std::endl;
        conn_.async_connect(conninfo, boost::bind(&tester::handle_connect, this, _1));
    }

    void handle_connect(const apq::result& res)
    {
        std::cout << "tester::handle_connect() ec=" << res.code() << std::endl;

        if (res.code())
        {
            std::cout << "\terror=" << res.message() << std::endl;
            return;
        }

        const char* query = "select $1::bigint as value1, $2::text as value2"
                            " union "
                            "select $1::bigint + 1 as value1, $2::text as value2";

        query_.reset(new apq::query(query));
        query_->bind_const_int64(1234567890);
        query_->bind_const_string("Hello world!");

        conn_.async_request_single(*query_, boost::bind(&tester::handle_request, this, _1, _2));
    }

    void handle_request(const apq::result& res, apq::row_iterator it)
    {
        std::cout << "tester::handle_request()"
                  << " ec=" << res.code() << std::endl;

        if (res.code())
        {
            std::cout << "\terror=" << res.message() << std::endl;
            return;
        }

        if (it == apq::row_iterator())
        {
            // Request done.
            if (--n_ > 0)
            {
                conn_.async_request(*query_, boost::bind(&tester::handle_request, this, _1, _2));
            }
            return;
        }

        for (; it != apq::row_iterator(); ++it)
        {
            std::cout << "\trow: ";

            for (int i = 0; i < it->size(); ++i)
                std::cout << "[" << it->at(i) << "]";

            if (it->has_column("value2")) std::cout << ", value2=" << it->at("value2");

            std::cout << std::endl;
        }
    }

    apq::connection conn_;

    boost::scoped_ptr<apq::query> query_;

    int n_;
};

struct tester2
{
    explicit tester2(apq::connection_pool& pool, int n) : pool_(pool), n_(n)
    {
    }

    void run()
    {
        std::cout << "tester2::run() this=" << this << std::endl;
        std::cout << "\tn=" << n_ << std::endl;

        // query_.reset(new apq::query("select * from blacklist limit 5"));
        // query_.reset(new apq::query("select uid, email2 from blacklist limit 5"));
        // query_.reset(new apq::query("insert into my_table1 (id, value1) values($1,
        // $2::my_type)"));
        query_.reset(new apq::query("select * from my_func1($1::my_type[])"));

        values_.clear();
        values_.push_back(
            apq::serialize_composite(boost::make_tuple(123, "La \\la-la-la \"la-la-la")));
        values_.push_back(
            apq::serialize_composite(boost::make_tuple(123, "La \\la-la-la \"la-la-la")));
        query_->bind_cref_string_vector(values_);

        pool_.async_request(
            *query_,
            boost::bind(&tester2::handle_request, this, _1, _2),
            apq::result_format_text,
            std::chrono::milliseconds(500));
    }

    void handle_request(const apq::result& res, apq::row_iterator it)
    {
        std::cout << "tester2::handle_request() this=" << this << ", ec=" << res.code()
                  << std::endl;

        if (res.code())
        {
            std::cout << "\terror=" << res.message() << std::endl;
            std::cout << "\tsqlstate=" << res.sqlstate();
            if (res.sqlstate_code() == apq::error::sqlstate::undefined_column)
                std::cout << ", undefined column" << std::endl;
            std::cout << std::endl;
        }
        else
        {
            for (; it != apq::row_iterator(); ++it)
            {
                std::cout << "\trow: ";

                for (int i = 0; i < it->size(); ++i)
                    std::cout << "[" << it->at(i) << "]";

                std::cout << std::endl;

                int x;
                it->at(0, x);
            }
        }

        if (--n_ > 0) run();
    }

    apq::connection_pool& pool_;

    boost::scoped_ptr<apq::query> query_;

    std::vector<std::string> values_;

    int n_;
};

int main(int, char**)
{
    unsigned int single_count = 1;
    unsigned int pooled_count = 1;
    unsigned int worker_count = 2;

    boost::asio::io_service ios;

    // Fire using single connections.
    std::vector<boost::shared_ptr<tester>> t1;
    for (unsigned int i = 0; i < single_count; ++i)
    {
        t1.push_back(boost::make_shared<tester>(boost::ref(ios), 2));
        t1.back()->run();
    }

    apq::connection_pool pool(ios);
    pool.set_conninfo(conninfo);
    pool.set_limit(10);
    pool.set_connect_timeout(std::chrono::milliseconds(500));
    pool.set_queue_timeout(std::chrono::seconds(5));

    // Fire using connection pool.
    std::vector<boost::shared_ptr<tester2>> t2;
    for (unsigned int i = 0; i < pooled_count; ++i)
    {
        t2.push_back(boost::make_shared<tester2>(boost::ref(pool), 4));
        t2.back()->run();
    }

    boost::thread_group thrs;
    for (unsigned int i = 0; i < worker_count; ++i)
    {
        thrs.create_thread(boost::bind(&boost::asio::io_service::run, &ios));
    }
    thrs.join_all();

    return EXIT_SUCCESS;
}
