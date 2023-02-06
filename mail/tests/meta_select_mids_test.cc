#include <mail/macs_pg/tests/mocks/macs_service_mock.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/mailbox_meta.h>
#include <macs/tests/mocking-folders.h>

using namespace testing;
using namespace mbox_oper;
using namespace macs::tests;

namespace {

struct MetaSelectMidsTest : FoldersRepositoryTest {
    MetaSelectMidsTest()
        : metadataMock_(boost::make_shared<Metadata>()),
          meta(metadataMock_, nullptr),
          items({ item("1", "1"), item("2", "2"), item("3", "3") })
    {
        EXPECT_CALL(*metadataMock_, folders()).WillRepeatedly(ReturnRef(folders));
    }

    static macs::ThreadMailboxItem item(macs::Mid mid, macs::Fid fid) {
        macs::ThreadMailboxItem item;
        item.mid = std::move(mid);
        item.fid = std::move(fid);
        return item;
    }

    MetadataPtr metadataMock_;
    MailboxMetaImpl meta;
    macs::ThreadMailboxItems items;
};

TEST_F(MetaSelectMidsTest, when_there_is_no_skip_folders_all_mids_should_be_returned) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        EXPECT_CALL(folders, syncGetFolders(_)).Times(Exactly(0));
        const auto res = meta.selectMids(items, SkipFolders(), yield);
        EXPECT_THAT(res, UnorderedElementsAre("1", "2", "3"));
    });
    io.run();
}

TEST_F(MetaSelectMidsTest, when_skip_folders_is_not_empty_mids_from_that_folders_should_be_filtered) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        const auto inbox = folders.factory().fid("1").name("inbox")
            .symbol(macs::Folder::Symbol::inbox).product();
        const auto spam = folders.factory().fid("2").name("spam")
            .symbol(macs::Folder::Symbol::spam).product();
        const auto custom = folders.factory().fid("3").name("custom")
            .symbol(macs::Folder::Symbol::none).product();
        EXPECT_CALL(folders, syncGetFolders(_)).WillOnce(GiveFolders(inbox, spam, custom));

        const SkipFolders skipFolders({ macs::Folder::Symbol::inbox, macs::Folder::Symbol::spam });
        const auto res = meta.selectMids(items, skipFolders, yield);
        EXPECT_THAT(res, UnorderedElementsAre("3"));
    });
    io.run();
}

} // namespace
