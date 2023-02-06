#include <signal.h>
#include <boost/thread/thread.hpp>
#include <boost/bind.hpp>

#include <yplatform/module.h>
#include <yplatform/find.h>
#include <yplatform/task_context.h>
#include <yplatform/time_traits.h>
#include <ymod_httpclient/call.h>
#include <ymod_httpclient/response/string_data_handler.h>

namespace ymod_httpclient_test {

using yplatform::ptree;

class context : public yplatform::task_context
{
public:
    context() : yplatform::task_context()
    {
    }

    context(const string& ctx_uniq_id) : yplatform::task_context(ctx_uniq_id)
    {
    }

    virtual const string& get_name() const
    {
        static const string name = "ymod_httpclient_test";
        return name;
    }
};

class stress_client : public yplatform::module
{
public:
    explicit stress_client(boost::asio::io_service& io) : io(io)
    {
    }

    void init(const ptree& config)
    {
        concurrency_ = config.get("concurrency", 1);
        total_count_ = config.get("count", 100);
        use_future_ = config.get("use_future", "on") == "on";
        url_ = config.get<string>("url");
        post_size_ = config.get("post-size", 0U);
        debug_ = config.get("debug", false);
        if (post_size_) post_str_ = make_shared<string>(post_size_, 'p');
        stopped_ = false;
    }

    void start()
    {
        http_client_ = yplatform::find<yhttp::call>("http_client");
        io.post([this] { start_worker_threads(); });
    }

    void stop()
    {
        stopped_ = true;
    }

private:
    void start_worker_threads()
    {
        start_time_ = time_traits::clock::now();
        processed_count_ = 0;
        started_count_ = 0;
        current_concurrency_ = 0;
        YLOG_G(info) << "Test started";
        run_request();
    }

    void run_request()
    {
        while (current_concurrency_ < concurrency_)
        {
            if (stopped_) return;
            int current = ++current_concurrency_;
            if (current > concurrency_)
            {
                --current_concurrency_;
                continue;
            }
            int started = ++started_count_;
            if (started > total_count_)
            {
                --current_concurrency_;
                return;
            }

            try
            {
                if (use_future_)
                {
                    make_request_with_future();
                }
                else
                {
                    make_request_async();
                }
            }
            catch (const std::exception& ex)
            {
                if (debug_) YLOG_G(error) << "exception: " << ex.what();
                handle_request();
            }
            catch (...)
            {
                if (debug_) YLOG_G(error) << "unknown exception";
                handle_request();
            }
        }
    }

    void make_request_with_future()
    {
        auto ctx = boost::make_shared<context>();
        auto handler = make_shared<ymod_httpclient::string_data_handler>();
        auto host_info = http_client_->make_rm_info(url_);
        ymod_httpclient::future_void_t response;
        if (post_size_ == 0)
        {
            response = http_client_->get_url(ctx, handler, host_info, "");
        }
        else
        {
            response = http_client_->post_url(ctx, handler, host_info, "", post_str_);
        }
        auto ptr = this->shared_from_this();
        response.add_callback([response, this, handler, ptr] {
            try
            {
                response.get();
                if (debug_)
                {
                    YLOG_G(info) << "code: " << handler->code() << " body: '" << handler->body()
                                 << "'";
                }
            }
            catch (const std::exception& ex)
            {
                if (debug_) YLOG_G(info) << "error on response: " << ex.what();
            }
            weak_ptr<::yplatform::module> wptr = ptr;
            io()->post([this, wptr] {
                if (auto ptr = wptr.lock())
                {
                    handle_request();
                    run_request();
                }
            });
        });
    }

    void make_request_async()
    {
        using yhttp::request;
        auto ctx = boost::make_shared<context>();
        auto ptr = this->shared_from_this();
        auto callback = [this, ptr](const boost::system::error_code& ec, yhttp::response resp) {
            if (debug_ || ec)
            {
                YLOG_G(info) << "code: " << resp.status << " body: '" << resp.body << "'"
                             << " ec: '" << ec.message() << "'";
            }
            weak_ptr<::yplatform::module> wptr = ptr;
            io.post([this, wptr] {
                if (auto ptr = wptr.lock())
                {
                    handle_request();
                    run_request();
                }
            });
        };
        if (post_size_ == 0)
        {
            http_client_->async_run(ctx, request::GET(url_), std::move(callback));
        }
        else
        {
            http_client_->async_run(
                ctx, request::POST(url_, post_str_->substr(0)), std::move(callback));
        }
    }

    void handle_request()
    {
        --current_concurrency_;
        if (++processed_count_ == total_count_)
        {
            auto end_time = time_traits::clock::now();
            time_traits::float_seconds time_seconds = end_time - start_time_;
            YLOG_G(info) << "Test finished: "
                         << " use_future=" << (use_future_ ? "on " : "off")
                         << " total_requests=" << total_count_ << " concurrency=" << concurrency_
                         << " duration=" << time_seconds.count()
                         << " RPS=" << (total_count_ / time_seconds.count());
            io.post([] { kill(getpid(), SIGINT); });
        }
    }

private:
    boost::asio::io_service& io;
    int concurrency_;
    int total_count_;
    std::atomic_int processed_count_;
    std::atomic_int started_count_;
    std::atomic_int current_concurrency_;
    std::atomic_bool stopped_;
    time_traits::time_point start_time_;
    unsigned post_size_;
    shared_ptr<string> post_str_;
    std::atomic_bool use_future_;
    std::atomic_bool debug_;
    string url_;
    shared_ptr<yhttp::call> http_client_;
};

}

#include <yplatform/module_registration.h>
DEFINE_SERVICE_OBJECT(ymod_httpclient_test::stress_client)
