#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/params.h>
#include "mailbox_meta_mock.h"

using namespace testing;
using namespace mbox_oper;

inline std::ostream& operator<< (std::ostream& os, const OptEnvelope& envelope) {
    if (envelope) {
        os << *envelope;
    }
    return os;
}

namespace {

const Fid spamFid = Fid("0");
const Fid trashFid = Fid("1");
const Fid testFid = Fid("42");

struct CompleMoveParamsTest : Test {
    CompleMoveParamsTest() : meta() {
        EXPECT_CALL(meta, getFid(macs::Folder::Symbol::spam, _)).WillOnce(Return(spamFid));
        EXPECT_CALL(meta, getFid(macs::Folder::Symbol::trash, _)).WillOnce(Return(trashFid));
    }

    ResolveOptions resolveOptions(const Fid& destFid, const OptBool& withSent) {
        const MailboxOperParams commonParams;
        const ComplexMoveParams params(destFid, withSent);
        ResolveOptions retval;
        boost::asio::io_context io;
        boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
            retval = params.resolveOptions(meta, commonParams, yield);
        });
        io.run();
        return retval;
    }

    StrictMock<MailboxMetaMock> meta;
};

TEST_F(CompleMoveParamsTest, resolveOptions_without_WithSent_with_spam_DestFid) {
    const auto res = resolveOptions(spamFid, boost::none);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox));
}

TEST_F(CompleMoveParamsTest, resolveOptions_without_WithSent_with_trash_DestFid) {
    const auto res = resolveOptions(trashFid, boost::none);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox));
}

TEST_F(CompleMoveParamsTest, resolveOptions_without_WithSent_with_not_spam_or_trash_DestFid) {
    const auto res = resolveOptions(testFid, boost::none);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox,
                                                        macs::Folder::Symbol::sent));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_false_with_spam_DestFid) {
    const auto res = resolveOptions(spamFid, false);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox,
                                                        macs::Folder::Symbol::sent));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_false_with_trash_DestFid) {
    const auto res = resolveOptions(trashFid, false);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox,
                                                        macs::Folder::Symbol::sent));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_false_with_not_spam_or_trash_DestFid) {
    const auto res = resolveOptions(testFid, false);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox,
                                                        macs::Folder::Symbol::sent));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_true_with_spam_DestFid) {
    const auto res = resolveOptions(spamFid, true);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_true_with_trash_DestFid) {
    const auto res = resolveOptions(trashFid, true);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox));
}

TEST_F(CompleMoveParamsTest, resolveOptions_with_WithSent_true_with_not_spam_or_trash_DestFid) {
    const auto res = resolveOptions(testFid, true);
    EXPECT_THAT(res.skipFolders(), UnorderedElementsAre(macs::Folder::Symbol::outbox));
}

} // namespace
