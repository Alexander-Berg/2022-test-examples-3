#include <mail/macs_pg/tests/mocks/macs_service_mock.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/mailbox_meta.h>
#include <macs/tests/mocking-folders.h>

using namespace testing;
using namespace mbox_oper;
using namespace macs::tests;

namespace {

struct MetaSelectFoldersCascadeTest : FoldersRepositoryTest {
    MetaSelectFoldersCascadeTest()
        : metadataMock_(boost::make_shared<Metadata>()),
          meta(metadataMock_, nullptr) {
        root = folders.factory().fid("1").name("root")
            .symbol(macs::Folder::Symbol::none).product();
        child = folders.factory().fid("2").name("root|child")
            .symbol(macs::Folder::Symbol::none).parentId("1").product();
        EXPECT_CALL(*metadataMock_, folders()).WillRepeatedly(ReturnRef(folders));
    }

    static auto byFidFilter(const std::string& fid) {
        return [fid](const macs::Folder& folder) {
            return folder.fid() == fid;
        };
    }

    static auto byParentFilter(const std::string& parentFid) {
        return [parentFid](const macs::Folder& folder) {
            return folder.parentId() == parentFid;
        };
    }

    MetadataPtr metadataMock_;
    MailboxMetaImpl meta;
    macs::Folder root;
    macs::Folder child;
};

TEST_F(MetaSelectFoldersCascadeTest, select_from_empty_folder_should_return_only_that_folder) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(root));

        const auto res = meta.selectFoldersCascade(Fid(root.fid()), ExcludeFilters({ byFidFilter("42") }), yield);
        EXPECT_THAT(res, ElementsAre(matchFolder(root)));
    });
    io.run();
}

TEST_F(MetaSelectFoldersCascadeTest, root_folder_should_not_be_filtered) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(root));

        const auto res = meta.selectFoldersCascade(Fid(root.fid()), ExcludeFilters({ byFidFilter("1") }), yield);
        EXPECT_THAT(res, ElementsAre(matchFolder(root)));
    });
    io.run();
}

TEST_F(MetaSelectFoldersCascadeTest, select_with_empty_filter_should_return_root_folder_and_all_childs) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(root, child));

        const auto res = meta.selectFoldersCascade(Fid(root.fid()), ExcludeFilters(), yield);
        EXPECT_THAT(res, UnorderedElementsAre(matchFolder(root), matchFolder(child)));
    });
    io.run();
}

TEST_F(MetaSelectFoldersCascadeTest, select_should_drop_filtered_folders) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        const auto child2 = folders.factory().fid("3").name("root|child2")
            .symbol(macs::Folder::Symbol::none).parentId("1").product();
        const auto childOfChild2 = folders.factory().fid("4").name("root|child2|child")
            .symbol(macs::Folder::Symbol::none).parentId("3").product();
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders({ root, child, child2, childOfChild2 }));

        const ExcludeFilters filters({ byFidFilter("2") , byParentFilter("3") });
        const auto res = meta.selectFoldersCascade(Fid(root.fid()), filters, yield);
        EXPECT_THAT(res, UnorderedElementsAre(matchFolder(root), matchFolder(child2)));
    });
    io.run();
}

} // namespace
