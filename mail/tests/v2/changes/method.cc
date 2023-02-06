#include <mail/hound/include/internal/v2/changes/method.h>
#include <macs_pg/changelog/factory.h>
#include <macs/label_factory.h>

#include <yamail/data/serialization/yajl.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace macs {

static bool operator == (const Change& lhs, const Change& rhs) {
    return lhs.changeId() == rhs.changeId();
}

} // namespace macs

namespace {

namespace changes = hound::v2::changes;
namespace changelog = changes::changelog;
namespace delta = changes::delta;

using namespace ::testing;
using namespace std::literals;

struct ChangeComposeMock {
    MOCK_METHOD(void, call, (boost::iterator_range<std::reverse_iterator<const macs::Change *>>, std::back_insert_iterator<std::vector<changes::Change>>), (const));
};

struct MailboxMock {
    MOCK_METHOD(std::vector<macs::Change>, getChanges, (macs::Revision, std::int64_t), (const));
};

struct TestMailbox {
    ChangeComposeMock& composer;
    MailboxMock& mailbox;

    template <typename Out>
    void getChanges(macs::Revision r, std::int64_t limit, Out out) const {
        boost::copy(mailbox.getChanges(r, limit), out);
    }

    friend auto makeChangeComposer(const TestMailbox& self) {
        decltype(auto) composer = self.composer;
        return [&](auto&& in, auto&& out) {
            return composer.call(in, out);
        };
    }
};

struct GetMailboxMock {
    MOCK_METHOD(TestMailbox, call, (macs::Uid uid), (const));
};

struct MailboxGetter {
    GetMailboxMock& mock;
    auto operator()(macs::Uid uid) const { return mock.call(uid);}
};

struct changes_method : Test {
    ChangeComposeMock composeChange;
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    changes::Method<MailboxGetter> method{MailboxGetter{getMailbox}};

    using ChangeType = macs::pg::ChangeType;

    static auto makeChange(macs::ChangeId cid, macs::Revision r, ChangeType type) {
        return macs::ChangeFactory{}
            .changeId(cid).uid("uid")
            .type(type)
            .revision(r)
            .release();
    }
};

TEST_F(changes_method, should_return_invalid_argument_for_max_count_equal_to_zero) {
    auto res = method(changes::Request{"uid", 10, 0});
    EXPECT_EQ(res.error(), changes::error_code{changes::error::invalidArgument});
}

TEST_F(changes_method, should_return_invalid_argument_for_max_count_less_than_zero) {
    auto res = method(changes::Request{"uid", 10, -1});
    EXPECT_EQ(res.error(), changes::error_code{changes::error::invalidArgument});
}

TEST_F(changes_method, should_return_invalid_argument_for_empty_uid) {
    auto res = method(changes::Request{"", 10, 1});
    EXPECT_EQ(res.error(), changes::error_code{changes::error::invalidArgument});
}

TEST_F(changes_method, should_return_empty_response_for_empty_changelog) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    EXPECT_CALL(mailbox, getChanges(macs::Revision(10), 1))
        .WillOnce(Return(std::vector<macs::Change>{}));
    const auto res = method(changes::Request{"uid", 10, 1});
    EXPECT_TRUE(res.value().changes.empty());
}

TEST_F(changes_method, should_return_revisionNotFound_for_changelog_with_lowest_revision_greater_than_start) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    EXPECT_CALL(mailbox, getChanges(macs::Revision(10), 2))
        .WillOnce(Return(std::vector<macs::Change>{
            makeChange(12, 13, ChangeType::store),
            makeChange(10, 12, ChangeType::store),
        }));
    const auto res = method(changes::Request{"uid", 10, 2});
    EXPECT_EQ(res.error(), changes::error_code{changes::error::revisionNotFound});
}

TEST_F(changes_method, should_return_deltas_with_changes_except_revision_requested) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));

    const std::vector<macs::Change> inChanges = {
        makeChange(1003, 13, ChangeType::store),
        makeChange(1002, 12, ChangeType::store),
        makeChange(1001, 10, ChangeType::store),
    };

    EXPECT_CALL(mailbox, getChanges(macs::Revision(10), 3))
        .WillOnce(Return(inChanges));

    auto inRange = boost::make_iterator_range(std::next(inChanges.rbegin()), inChanges.rend());
    EXPECT_CALL(composeChange, call(inRange, _))
    .WillOnce(Invoke([](auto&&, std::back_insert_iterator<std::vector<changes::Change>> out) {
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(12, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(13, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
    }));

    const auto res = method(changes::Request{"uid", 10, 3});

    ASSERT_EQ(res.value().changes.size(), 2ul);
    EXPECT_EQ(res.value().changes[0].revision, 12);
    EXPECT_EQ(res.value().changes[1].revision, 13);
}

} // namespace
