#include <mail/macs_pg/tests/mocks/macs_service_mock.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/mailbox_meta.h>
#include <macs/tests/mocking-macs.h>
#include <macs/tests/mocking-folders.h>

using namespace testing;
using namespace mbox_oper;
using namespace macs::tests;

namespace {

struct MetaResolveMidsTest : Test {
    using Folders = StrictMock<MockFoldersRepository>;
    using FoldersPtr = std::shared_ptr<Folders>;
    using Threads = StrictMock<MockThreadsRepository>;
    using ThreadsPtr = std::shared_ptr<Threads>;

    MetaResolveMidsTest()
        : folders(std::make_shared<Folders>()),
          threads(std::make_shared<Threads>()),
          metadata(boost::make_shared<Metadata>()),
          meta(metadata, nullptr),
          mids({ "1", "2", "3" }),
          tids({ "10", "20", "30" }),
          resultStub({ "42", "43" })
    {
        EXPECT_CALL(*metadata, folders()).WillRepeatedly(ReturnRef(*folders));
        EXPECT_CALL(*metadata, threads()).WillRepeatedly(ReturnRef(*threads));
    }

    FoldersPtr folders;
    ThreadsPtr threads;
    MetadataPtr metadata;
    MailboxMetaImpl meta;
    Mids mids;
    Tids tids;
    macs::Lids lidsToCheck;
    macs::Mids resultStub;
};

TEST_F(MetaResolveMidsTest, resolve_without_skip_folers_without_exclude_status) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(*threads, syncFillIdsList(mids, tids, _)).WillOnce(InvokeArgument<2>(resultStub));
        meta.resolveMids(mids, tids, ResolveOptions(), yield);
    });
    io.run();
}

TEST_F(MetaResolveMidsTest, resolve_with_skip_folers_without_exclude_status) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        const auto folder = folders->factory().fid("1").name("sent")
            .symbol(macs::Folder::Symbol::sent).product();
        const ResolveOptions resolveOptions{ { macs::Folder::Symbol::sent } };

        const macs::ThreadMailboxItems items;
        EXPECT_CALL(*threads, syncFillIdsMap(mids, tids, lidsToCheck, _)).WillOnce(InvokeArgument<3>(items));
        EXPECT_CALL(*folders, syncGetFolders(_)).WillOnce(FoldersRepositoryTest::GiveFolders(folder));

        meta.resolveMids(mids, tids, resolveOptions, yield);
    });
    io.run();

}

TEST_F(MetaResolveMidsTest, resolve_without_skip_folers_with_exclude_status) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(*threads, syncFillIdsListWithoutStatus(mids, tids, macs::Envelope::Status_read, _))
            .WillOnce(InvokeArgument<3>(resultStub));

        const ResolveOptions resolveOptions{ {}, macs::Envelope::Status_read };
        meta.resolveMids(mids, tids, resolveOptions, yield);
    });
    io.run();
}

TEST_F(MetaResolveMidsTest, resolve_with_skip_folers_with_exclude_status) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        const auto folder = folders->factory().fid("1").name("inbox")
            .symbol(macs::Folder::Symbol::inbox).product();

        const macs::ThreadMailboxItems items;
        EXPECT_CALL(*threads, syncFillIdsListWithoutStatus(mids, tids, macs::Envelope::Status_unread, _))
            .WillOnce(InvokeArgument<3>(resultStub));
        EXPECT_CALL(*threads, syncFillIdsMap(resultStub, IsEmpty(), _, _))
            .WillOnce(InvokeArgument<3>(items));
        EXPECT_CALL(*folders, syncGetFolders(_)).WillOnce(FoldersRepositoryTest::GiveFolders(folder));

        const ResolveOptions resolveOptions{ { macs::Folder::Symbol::inbox }, macs::Envelope::Status_unread };
        meta.resolveMids(mids, tids, resolveOptions, yield);
    });
    io.run();
}

} // namespace
