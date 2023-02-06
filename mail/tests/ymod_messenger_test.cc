#include <yplatform/module.h>
#include <yplatform/module_registration.h>
#include <yplatform/ptree.h>
#include <yplatform/repository.h>
#include <yplatform/find.h>
#include <yplatform/zerocopy/streambuf.h>
#include <yplatform/time_traits.h>
#include <ymod_messenger/ymod_messenger.h>
#include "../src/timer.h"
#include "ymod_messenger/types.h"

namespace ymod_messenger_test {

using yplatform::ptree;
using boost::shared_ptr;
using std::string;
namespace time_traits = yplatform::time_traits;

void do_nothing()
{
}

class client : public yplatform::module
{
public:
    void init(const ptree& config)
    {
        messenger_ = yplatform::find<ymod_messenger::module>("ymod_messenger");
        messenger_->bind_messages<std::string>(boost::bind(&client::on_message, this, _1, _2));
        messenger_->bind_events(boost::bind(&client::on_event, this, _1, _2));

        size_ = config.get<unsigned>("size");
        count_ = config.get<unsigned>("count");
        parallel_ = config.get<unsigned>("parallel");
        server_address_ = config.get<string>("server");

        generate_data();
    }

    void start()
    {
        messenger_->connect(server_address_);
    }

    void stop()
    {
        auto now = time_traits::clock::now();
        auto diff = now - start_send_time_;
        auto msec = time_traits::duration_cast<time_traits::milliseconds>(diff);
        YLOG_G(debug) << "result: " << received_count_ << " for " << diff.count()
                      << " rps: " << 1000.0 * (double(received_count_) - 1) / msec.count();
        std::cerr << "result: " << received_count_ << " for " << diff.count()
                  << " rps: " << 1000.0 * (double(received_count_) - 1) / msec.count() << std::endl;
    }

private:
    void start_send()
    {
        received_count_ = 0;
        start_send_time_ = time_traits::clock::now();
        for (unsigned i = 0; i < parallel_; ++i)
            messenger_->send(server_address_, test_data_[0]);
    }

    void generate_data()
    {
        test_data_.resize(count_);
        for (size_t i = 0; i < count_; ++i)
        {
            test_data_[i].resize(size_);
            for (uint32_t j = 0; j < size_; j++)
            {
                test_data_[i][j] = char(int(65 + rand() % 30));
            }
        }
    }

    void on_message(const ymod_messenger::address_t& /*from*/, const std::string& /*msg*/)
    {
        boost::mutex::scoped_lock lock(mux_);
        received_count_++;
        lock.unlock();

        //        if (received_count_ < count_) {
        auto data = test_data_[rand() % test_data_.size()];
        messenger_->send(server_address_, data);
        //        }
    }

    void on_event(
        const ymod_messenger::address_t& /*from*/,
        const ymod_messenger::event_notification& event_msg)
    {
        if (event_msg.event == ymod_messenger::event_CONNECTED)
        {
            yplatform::global_net_reactor->io()->post([this]() { start_send(); });
        }
        else if (event_msg.event == ymod_messenger::event_POOL_LOST)
        {
        }
    }

    time_traits::time_point start_send_time_;
    unsigned count_;
    unsigned size_;
    unsigned parallel_;
    uint64_t sent_count_;
    uint64_t received_count_;
    boost::mutex mux_;
    bool send_active;
    string server_address_;

    shared_ptr<ymod_messenger::module> messenger_;
    std::vector<std::string> test_data_;
    ymod_messenger::timer_ptr timer_;
};

class server : public yplatform::module
{
public:
    virtual void init(const ptree& /*__xml*/)
    {
        messenger_ = yplatform::find<ymod_messenger::module>("ymod_messenger");
        messenger_->bind_messages<std::string>(boost::bind(&server::on_message, this, _1, _2));
        messenger_->bind_events(boost::bind(&server::on_event, this, _1, _2));
    }

    virtual void start()
    {
    }

    virtual void stop()
    {
    }

private:
    void on_message(const ymod_messenger::address_t& from, const std::string& msg)
    {
        messenger_->send(from, msg);
    }

    void on_event(
        const ymod_messenger::address_t& /*from*/,
        const ymod_messenger::event_notification& event_msg)
    {
        if (event_msg.event == ymod_messenger::event_CONNECTED)
        {
            YLOG_G(debug) << "* connected";
        }
        else if (event_msg.event == ymod_messenger::event_POOL_LOST)
        {
            YLOG_G(debug) << "* pool lost";
        }
    }

    shared_ptr<ymod_messenger::module> messenger_;
};

}

DEFINE_SERVICE_OBJECT_BEGIN()
DEFINE_SERVICE_OBJECT_MODULE(ymod_messenger_test::client)
DEFINE_SERVICE_OBJECT_MODULE(ymod_messenger_test::server)
DEFINE_SERVICE_OBJECT_END()
