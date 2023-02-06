#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include "test_mocks.h"
#include <internal/server/handlers/create_list.h>
#include <yplatform/reactor/io_pool.h>

using namespace testing;

namespace york {
namespace tests {

struct CreateListIntegrationTest: public Test {
    std::shared_ptr<ContextMock> ctx = std::make_shared<ContextMock>();
    MacsMock<coro> macsMock;
    boost::asio::io_service ios;
    boost::shared_ptr<yplatform::reactor> old;
    CreateListIntegrationTest() {
        auto iop = std::make_shared<yplatform::io_pool>(ios, 1);
        old = boost::make_shared<yplatform::reactor>(iop);
        std::swap(old, yplatform::global_net_reactor);

        EXPECT_CALL(*ctx, getOptionalArg("uid")).WillOnce(Return(boost::optional<std::string>("1")));
        EXPECT_CALL(*ctx, getOptionalArg("shared_folder_name")).WillOnce(Return(boost::optional<std::string>("a")));
    }
    ~CreateListIntegrationTest() {
        std::swap(old, yplatform::global_net_reactor);
    }

    struct ConfigMock {
        const FolderArchivation archivation {"archive", 0, 0};
    };

    void doSmth() {
        ConfigMock cfg;
        auto handler = makeCreateListHandler(&cfg,
            [this](const std::string&, auto&, auto){ return &macsMock; });
        handler(ctx, log::none);
        ios.run();
    }
};

TEST_F(CreateListIntegrationTest, macsGetAllFoldersThrows_responses500) {
    EXPECT_CALL(macsMock, getAllFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(CreateListIntegrationTest, macsCreateFolderThrows_responses500) {
    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsMock, createFolder("a", _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(CreateListIntegrationTest, macsGetAllSharedFoldersThrows_responses500) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);

    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsMock, createFolder("a", _)).WillOnce(Return(f));
    EXPECT_CALL(macsMock, getAllSharedFolders(_))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}

TEST_F(CreateListIntegrationTest, macsCreateSharedFolderThrows_responses500) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);

    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsMock, createFolder("a", _)).WillOnce(Return(f));
    EXPECT_CALL(macsMock, getAllSharedFolders(_)).WillOnce(Return(std::vector<macs::Fid>()));
    EXPECT_CALL(macsMock, createSharedFolderWithArchivation("1", _, _, _, _))
        .WillOnce(Throw(boost::system::system_error(boost::system::error_code())));
    EXPECT_CALL(ctx->resp, internalError(_)).Times(1);
    doSmth();
}


TEST_F(CreateListIntegrationTest, positiveCase) {
    macs::Folder f = macs::FolderFactory().fid("1").name("a").parentId(macs::Folder::noParent).messages(1);

    EXPECT_CALL(macsMock, getAllFolders(_)).WillOnce(Return(macs::FolderSet{}));
    EXPECT_CALL(macsMock, createFolder("a", _)).WillOnce(Return(f));
    EXPECT_CALL(macsMock, getAllSharedFolders(_)).WillOnce(Return(std::vector<macs::Fid>()));
    EXPECT_CALL(macsMock, createSharedFolderWithArchivation("1", _, _, _, _)).Times(1);
    EXPECT_CALL(ctx->resp, ok(An<server::handlers::CreateListResult>())).Times(1);
    doSmth();
}

} //namespace tests
} //namespace york
