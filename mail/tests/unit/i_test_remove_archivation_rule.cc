#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/remove_archivation_rule.h>
#include <yplatform/reactor/io_pool.h>

using namespace testing;

namespace york {
namespace tests {

struct RemoveArchivationRuleIntegrationTest: public Test {
    std::shared_ptr<ContextMock> ctx = std::make_shared<ContextMock>();
    MacsMock<coro> macsMock;
    boost::asio::io_service ios;
    boost::shared_ptr<yplatform::reactor> old;

    RemoveArchivationRuleIntegrationTest() {
        auto iop = std::make_shared<yplatform::io_pool>(ios, 1);
        old = boost::make_shared<yplatform::reactor>(iop);
        std::swap(old, yplatform::global_net_reactor);

        EXPECT_CALL(*ctx, getOptionalArg("uid")).WillOnce(Return(boost::optional<std::string>("uid")));
    }
    ~RemoveArchivationRuleIntegrationTest() {
        std::swap(old, yplatform::global_net_reactor);
    }

    void doSmth() {
        auto handler = makeRemoveArchivationRuleHandler(nullptr,
            [this](const std::string&, auto&, ConfigPtr){ return &macsMock; });
        handler(ctx, log::none);
        ios.run();
    }
};

TEST_F(RemoveArchivationRuleIntegrationTest, macsGetAllFoldersThrows_responses500) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::none));
    EXPECT_CALL(macsMock, getAllFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(RemoveArchivationRuleIntegrationTest, macsRemoveArchivationRuleThrows_responses500) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>("fid")));
    EXPECT_CALL(macsMock, removeArchivationRule("fid", _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}


TEST_F(RemoveArchivationRuleIntegrationTest, positiveCase) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>("fid")));
    EXPECT_CALL(macsMock, removeArchivationRule("fid", _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::RemoveArchivationRuleResult>())).Times(1);
    doSmth();
}

} //namespace tests
} //namespace york
