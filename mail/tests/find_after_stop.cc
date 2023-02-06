#include "yplatform/application.h"
#include <yplatform/module.h>
#include <yplatform/module_registration.h>
#include <yplatform/ptree.h>

#include <iostream>
#include <cstring>

class test_module : public yplatform::module
{
public:
    struct Handler
    {
        ~Handler()
        {
            // Only perform check with final handler, not during it's moving around.
            if (moved_from) return;
            try
            {
                yplatform::find<test_module>("test");
                assert(false);
            }
            catch (...)
            {
            }
        }

        void operator()(const boost::system::error_code&)
        {
            owner->handler_called = true;
        }

        Handler(
            std::shared_ptr<yplatform::time_traits::timer> timer,
            std::shared_ptr<test_module> owner)
            : timer(timer), owner(owner)
        {
        }

        // Needed to trick timer into accepting move-only handler.
        Handler(const Handler& other);

        Handler(Handler&& other) : timer(other.timer), owner(other.owner)
        {
            other.moved_from = true;
        }

        std::shared_ptr<yplatform::time_traits::timer> timer;
        std::shared_ptr<test_module> owner;
        bool moved_from = false;
    };

    void init(const yplatform::ptree&)
    {
        reactor_ = yplatform::find<yplatform::reactor>("global");
    }

    void stop()
    {
        // Application will stop reactor soon.
        // Handler will be destroyed without execution.
        auto timer = std::make_shared<yplatform::time_traits::timer>(*reactor_->io());
        timer->expires_from_now(yplatform::time_traits::seconds(1));
        timer->async_wait(Handler{ timer, shared_from(this) });
    }

    yplatform::reactor_ptr reactor_;
    bool handler_called = false;
};

DEFINE_SERVICE_OBJECT(test_module)

int main()
{
    yplatform::configuration conf;
    conf.load_from_file("app_run.yml");
    std::shared_ptr<test_module> module;
    {
        yplatform::application app(conf);
        app.run();
        module = yplatform::find<test_module, std::shared_ptr>("test");
        app.stop(10);
    }
    assert(!module->handler_called);

    return 0;
}
