#include <yplatform/object.h>
#include <yplatform/find.h>
#include <yplatform/net/types.h>
#include <ymod_cache/cache.h>
#include <ymod_cache/error.h>

#include <boost/make_shared.hpp>
#include <ostream>

namespace ymod_cache_test {

class my_context : public yplatform::task_context
{
    const std::string& get_name() const
    {
        static std::string n("test context");
        return n;
    }
};

std::ostream& operator<<(std::ostream& stream, const yplatform::zerocopy::segment& seg)
{
    for (yplatform::zerocopy::segment::iterator it = seg.begin();
        it != seg.end(); ++it)
    {
        stream.put(*it);
    }
    return stream;
}

#define CATCH_BLOCK(operation) \
    catch(const yplatform::exception& e) \
    { \
        const std::string* cl = boost::get_error_info<yplatform::error_class_info>(e); \
        const std::string* priv = boost::get_error_info<yplatform::error_private_info>(e); \
        std::cout << #operation " '" << key << "' failed: " \
            << (cl ? *cl : "unknown error") << ": " \
            << (priv ? *priv : "no details available") << std::endl; \
    }

template <class K, class V>
void set_synchronous(ymod_cache::cache* c, const K& key, const V& value)
{
    ymod_cache::future_result res = c->set(boost::make_shared<my_context>(), key, value);
    res.wait();
    try
    {
        res.get();
        std::cout << "'" << key << "' stored, value: " << value << std::endl;
    }
    catch(const ymod_cache::not_stored& e)
    {
        const std::string* priv = boost::get_error_info<yplatform::error_private_info>(e);
        std::cout << "'" << key << "' not stored: "
            << (priv ? *priv : "no details available") << std::endl;
    }
    CATCH_BLOCK(set)
}

template <class K>
void get_synchronous(ymod_cache::cache* c, const K& key)
{
    ymod_cache::future_segment res = c->get(boost::make_shared<my_context>(), key);
    res.wait();
    try
    {
        if (res.get())
            std::cout << "'" << key << "': " << res.get().get() << std::endl;
        else
            std::cout << "'" << key << "' not found" << std::endl;
    }
    CATCH_BLOCK(get)
}

template <class K>
void has_synchronous(ymod_cache::cache* c, const K& key)
{
    ymod_cache::future_bool res = c->has(boost::make_shared<my_context>(), key);
    res.wait();
    try
    {
        if (res.get())
            std::cout << "'" << key << "' found" << std::endl;
        else
            std::cout << "'" << key << "' not found" << std::endl;
    }
    CATCH_BLOCK(has)
}

template <class K>
void remove_synchronous(ymod_cache::cache* c, const K& key)
{
    ymod_cache::future_result res = c->remove(boost::make_shared<my_context>(), key);
    res.wait();
    try
    {
        res.get();
        std::cout << "'" << key << "' removed" << std::endl;
    }
    CATCH_BLOCK(remove)
}

class test : public yplatform::module
{
public:
    void init(const yplatform::ptree& xml)
    {}
    void fini()
    {}
    void start()
    {
        tm.reset(new yplatform::net::timer_t(*yplatform::global_net_reactor->io(), boost::posix_time::seconds(1)));
        tm->async_wait(boost::bind(&test::do_test, this));
    }
    void stop()
    {
        tm->cancel();
    }
    void do_test()
    {
        boost::shared_ptr<ymod_cache::cache> cache_module =
            yplatform::find<ymod_cache::cache>("cache");
        ymod_cache::cache* c = cache_module.get();
        set_synchronous(c, "foo", 3);
        set_synchronous(c, "bar", true);
        has_synchronous(c, "foo");

        get_synchronous(c, "foo");
        remove_synchronous(c, "foo");
        get_synchronous(c, "foo");
        set_synchronous(c, "foo", "baz");
        get_synchronous(c, "foo");

        get_synchronous(c, "bar");
        set_synchronous(c, "bar", 42);
        get_synchronous(c, "bar");

        has_synchronous(c, 42);

        remove_synchronous(c, -1);

        set_synchronous(c, 1, 0);
        set_synchronous(c, true, 1);
        set_synchronous(c, "1", 2);
        get_synchronous(c, 1);

        set_synchronous(c, "4th value", 0);

        set_synchronous(c, "bigstuff", std::string(1234, 'x'));
        get_synchronous(c, "bigstuff");

        tm->expires_from_now(boost::posix_time::seconds(10));
        tm->async_wait(boost::bind(&test::do_test, this));
    }
private:
    yplatform::net::timer_ptr tm;
};

}

#include <yplatform/module.h>
DEFINE_SERVICE_OBJECT(ymod_cache_test::test)
