#include <boost/asio.hpp>
#include <yplatform/coroutine.h>
#include <yplatform/yield.h>
#include <string>
#include <memory>

using yplatform::spawn;
using yplatform::yield_context;

struct coro
{
    boost::asio::io_service* io;
    int x = 0;

    void operator()(
        yield_context<coro> ctx,
        boost::system::error_code = boost::system::error_code())
    {
        reenter(ctx)
        {
            yield io->post(ctx);
            ++x;
            fork spawn(ctx);
            if (ctx.is_child()) ++x;
            if (ctx.is_parent()) ++x;
        }
    }
};

int main()
{
    boost::asio::io_service io;
    auto c = std::make_shared<coro>();
    c->io = &io;
    spawn(c);
    io.run();
    assert(c->x == 3);
    return 0;
}

#include <yplatform/unyield.h>
