#include <mail/sharpei/tests/mocks.h>

#include <internal/db/pools/meta_pool.h>
#include <internal/db/ep/single_endpoint_provider.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

namespace sharpei::db {

bool operator==(const ConnectionInfo& lhs, const ConnectionInfo& rhs) {
    return !(lhs != rhs);
}

}  // namespace sharpei::db

namespace {

using namespace sharpei;
using namespace sharpei::db;
using namespace ::testing;

class MetaPoolTest : public Test {
public:
    MetaPoolTest() {
        apqPool_.reset(new ApqConnectionPoolMock(io_));
        pool_.reset(new MockConnectionPool());
        auto endpointProvider = std::make_shared<SingleEndpointProvider>(getConninfo());
        metaPool_.reset(new MetaPool(pool_, std::move(endpointProvider)));
    }

    static ConnectionInfo getConninfo() {
        auto authInfo = AuthInfo().password("pwd").user("usr").sslmode("verify-full");
        return ConnectionInfo("localhost", 42, "db", authInfo);
    }

    boost::asio::io_context io_;
    std::shared_ptr<ApqConnectionPoolMock> apqPool_;
    std::shared_ptr<MockConnectionPool> pool_;
    std::shared_ptr<MetaPool> metaPool_;
};

TEST_F(MetaPoolTest, async_request) {
    EXPECT_CALL(*pool_, get(getConninfo())).WillOnce(Return(apqPool_));
    EXPECT_CALL(*apqPool_, async_request(_, _, _, _));

    apq::query q{"query"};
    apq::connection_pool::request_handler_t handler;
    metaPool_->async_request(q, handler, apq::result_format::result_format_binary, {});
}

TEST_F(MetaPoolTest, async_update) {
    EXPECT_CALL(*pool_, get(getConninfo())).WillOnce(Return(apqPool_));
    EXPECT_CALL(*apqPool_, async_update(_, _, _));

    apq::query q{"query"};
    apq::connection_pool::update_handler_t handler;
    metaPool_->async_update(q, handler, {});
}

}  // namespace
