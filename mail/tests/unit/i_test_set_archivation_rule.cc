#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/set_archivation_rule.h>
#include <yplatform/reactor/io_pool.h>

using namespace testing;

namespace york {
namespace tests {

struct SetArchivationRuleIntegrationTest: public Test {
    std::shared_ptr<ContextMock> ctx = std::make_shared<ContextMock>();
    MacsMock<coro> macsMock;
    boost::asio::io_service ios;
    boost::shared_ptr<yplatform::reactor> old;

    SetArchivationRuleIntegrationTest() {
        auto iop = std::make_shared<yplatform::io_pool>(ios, 1);
        old = boost::make_shared<yplatform::reactor>(iop);
        std::swap(old, yplatform::global_net_reactor);

        EXPECT_CALL(*ctx, getOptionalArg("uid")).WillOnce(Return(boost::optional<std::string>("uid")));
        EXPECT_CALL(*ctx, getOptionalArg("type")).WillOnce(Return(boost::optional<std::string>("clean")));
        EXPECT_CALL(*ctx, getOptionalArg("keep_days")).WillOnce(Return(boost::optional<std::string>("42")));
        EXPECT_CALL(*ctx, getOptionalArg("max_size")).WillOnce(Return(boost::optional<std::string>("1000")));
    }
    ~SetArchivationRuleIntegrationTest() {
        std::swap(old, yplatform::global_net_reactor);
    }

    struct ConfigMock {
        const FolderArchivation archivation {"", 0, 0};
    };

    void doSmth() {
        ConfigMock cfg;
        auto handler = makeSetArchivationRuleHandler(&cfg,
            [this](const std::string&, auto&, auto){ return &macsMock; });
        handler(ctx, log::none);
        ios.run();
    }
};

TEST_F(SetArchivationRuleIntegrationTest, macsGetAllFoldersThrows_responses500) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::none));
    EXPECT_CALL(macsMock, getAllFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SetArchivationRuleIntegrationTest, macsGetAllSharedFoldersThrows_responses500) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>("fid")));
    EXPECT_CALL(macsMock, getAllSharedFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(SetArchivationRuleIntegrationTest, macsSetArchivationRuleThrows_responses500) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>("fid")));
    EXPECT_CALL(macsMock, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsMock, setArchivationRule("fid", _, _, _, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}


TEST_F(SetArchivationRuleIntegrationTest, positiveCase) {
    EXPECT_CALL(*ctx, getOptionalArg("shared_folder_fid")).WillOnce(Return(boost::optional<std::string>("fid")));
    EXPECT_CALL(macsMock, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));
    EXPECT_CALL(macsMock, setArchivationRule("fid", _, _, _, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::SetArchivationRuleResult>())).Times(1);
    doSmth();
}

} //namespace tests
} //namespace york
