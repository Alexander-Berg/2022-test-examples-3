#include "mailbox_mocks.h"

#include <src/common/context.h>
#include <src/mailbox/data_types/cache_mailbox.h>
#include <src/xeno/load_cache_op.h>
#include <catch.hpp>

struct cache_loader_test
{
    cache_loader_test()
        : ctx{ boost::make_shared<xeno::context>() }
        , loc_mailbox{ create_from_json(LOCAL_MAILBOX_PATH, local_mock_type::type_normal) }
    {
    }

    boost::asio::io_service io;
    xeno::context_ptr ctx;
    loc_mb::loc_mailbox_mock_ptr loc_mailbox;
};

TEST_CASE_METHOD(cache_loader_test, "don't load cache when empty auth data")
{
    loc_mailbox->set_account(xeno::account_t());
    io.post([this]() {
        xeno::load_cache_op(ctx, loc_mailbox, [](error ec, mb::cache_mailbox_ptr /*res*/) {
            REQUIRE(ec == xeno::code::no_auth_data);
        })();
    });
    io.run();
    io.reset();
}
