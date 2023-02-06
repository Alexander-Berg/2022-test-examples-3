#include <mail/hound/include/internal/v2/changelog/method.h>
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
namespace changelog = hound::v2::changelog;
namespace delta = changes::delta;
using ChangeType = macs::pg::ChangeType;
using CheckRevisionPolicy = hound::v2::changelog::CheckRevisionPolicy;

using namespace ::testing;
using namespace std::literals;

struct ChangeComposeMock {
    MOCK_METHOD(void, call, (const std::vector<macs::Change>&, std::back_insert_iterator<std::vector<changes::Change>>), (const));
};

struct MailboxMock {
    MOCK_METHOD(bool, checkUidRevisionExists, (macs::Revision), (const));
    MOCK_METHOD(std::vector<macs::Change>, getChangelog, (macs::Revision, std::int64_t), (const));
    MOCK_METHOD(std::vector<macs::Change>, getChangelogByType, (macs::Revision, std::int64_t,
                                                                     const std::vector<ChangeType>&), (const));
};

struct TestMailbox {
    ChangeComposeMock& composer;
    MailboxMock& mailbox;

    bool checkUidRevisionExists(macs::Revision r) const {
        return mailbox.checkUidRevisionExists(r);
    }

    template <typename Out>
    void getChangelog(macs::Revision r, std::int64_t limit, Out out) const {
        boost::copy(mailbox.getChangelog(r, limit), out);
    }

    template <typename Out>
    void getChangelogByType(macs::Revision r, std::int64_t limit,
                          const std::vector<ChangeType>& change_types, Out out) const {
        boost::copy(mailbox.getChangelogByType(r, limit, change_types), out);
    }

    friend auto makeChangeComposer(const TestMailbox& self) {
        decltype(auto) composer = self.composer;
        return [&](const std::vector<macs::Change>& in, auto&& out) {
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

struct changelog_method : Test {
    ChangeComposeMock composeChange;
    MailboxMock mailbox;
    GetMailboxMock getMailbox;
    changelog::Method<MailboxGetter> method{MailboxGetter{getMailbox}};

    static auto makeChange(macs::ChangeId cid, macs::Revision r, ChangeType type) {
        return macs::ChangeFactory{}
            .changeId(cid).uid("uid")
            .type(type)
            .revision(r)
            .release();
    }
};

TEST_F(changelog_method, should_return_invalid_argument_for_max_count_equal_to_zero) {
    auto res = method(changelog::Request{"uid", 10, 0, {ChangeType::store}, CheckRevisionPolicy::loyal});
    EXPECT_EQ(res.error(),
        changelog::error_code{changelog::error::invalidArgument});
}

TEST_F(changelog_method, should_return_invalid_argument_for_max_count_less_than_zero) {
    auto res = method(changelog::Request{"uid", 10, -1, {ChangeType::store}, CheckRevisionPolicy::loyal});
    EXPECT_EQ(res.error(),
        changelog::error_code{changelog::error::invalidArgument});
}

TEST_F(changelog_method, should_return_invalid_argument_for_empty_uid) {
    auto res = method(changelog::Request{"", 10, 1, {ChangeType::store}, CheckRevisionPolicy::loyal});
    EXPECT_EQ(res.error(),
        changelog::error_code{changelog::error::invalidArgument});
}

TEST_F(changelog_method, should_return_invalid_argument_for_empty_change_types) {
    auto res = method(changelog::Request{"", 10, 1, {}, CheckRevisionPolicy::loyal});
    EXPECT_EQ(res.error(),
        changelog::error_code{changelog::error::invalidArgument});
}

TEST_F(changelog_method, should_return_revisionNotFound_for_changelog_with_lowest_revision_greater_than_start) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    EXPECT_CALL(mailbox, checkUidRevisionExists(macs::Revision(10)))
        .WillOnce(Return(false));
    const auto res = method(changelog::Request{"uid", 10, 2, {ChangeType::store}, CheckRevisionPolicy::strict});
    EXPECT_EQ(res.error(),
        changelog::error_code{changelog::error::revisionNotFound});
}

TEST_F(changelog_method, should_check_revision_and_return_deltas_with_changes_when_CheckRevisionPolicy_strict) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    EXPECT_CALL(mailbox, checkUidRevisionExists(macs::Revision(10)))
        .WillOnce(Return(true));
    const std::vector<macs::Change> inChanges {
        makeChange(1002, 12, ChangeType::store),
        makeChange(1003, 13, ChangeType::store),
    };

    EXPECT_CALL(mailbox, getChangelogByType(macs::Revision(10), 2, std::vector<ChangeType>{ChangeType::store}))
        .WillOnce(Return(inChanges));
    EXPECT_CALL(composeChange, call(inChanges, _))
        .WillOnce(Invoke([](auto&&, std::back_insert_iterator<std::vector<changes::Change>> out) {
            if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(12, delta::Store{{macs::Envelope{}}})) {
                out++ = *opt;
            }
            if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(13, delta::Store{{macs::Envelope{}}})) {
                out++ = *opt;
            }
        }));

    const auto res = method(changelog::Request{"uid", 10, 2, {ChangeType::store}, CheckRevisionPolicy::strict});

    ASSERT_EQ(res.value().changes.size(), 2ul);
    EXPECT_EQ(res.value().changes[0].revision, 12);
    EXPECT_EQ(res.value().changes[1].revision, 13);
}

TEST_F(changelog_method, should_do_not_check_revision_and_return_deltas_with_changes_when_CheckRevisionPolicy_loyal) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    const std::vector<macs::Change> inChanges {
            makeChange(1002, 12, ChangeType::store),
            makeChange(1003, 13, ChangeType::store),
    };
    EXPECT_CALL(mailbox, getChangelogByType(macs::Revision(10), 2, std::vector<ChangeType>{ChangeType::store}))
        .WillOnce(Return(inChanges));

    EXPECT_CALL(composeChange, call(inChanges, _))
    .WillOnce(Invoke([](auto&&, std::back_insert_iterator<std::vector<changes::Change>> out) {
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(12, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(13, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
    }));

    const auto res = method(changelog::Request{"uid", 10, 2, {ChangeType::store}, CheckRevisionPolicy::loyal});

    ASSERT_EQ(res.value().changes.size(), 2ul);
    EXPECT_EQ(res.value().changes[0].revision, 12);
    EXPECT_EQ(res.value().changes[1].revision, 13);
}

TEST_F(changelog_method, should_get_all_changes_when_change_types_is_empty) {
    EXPECT_CALL(getMailbox, call("uid"))
        .WillOnce(Return(TestMailbox{composeChange, mailbox}));
    const std::vector<macs::Change> inChanges {
            makeChange(1002, 12, ChangeType::store),
            makeChange(1003, 13, ChangeType::store),
    };
    EXPECT_CALL(mailbox, getChangelog(macs::Revision(10), 2))
        .WillOnce(Return(inChanges));
    EXPECT_CALL(composeChange, call(inChanges, _))
    .WillOnce(Invoke([](auto&&, std::back_insert_iterator<std::vector<changes::Change>> out) {
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(12, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
        if (std::optional<hound::v2::changes::Change> opt = changes::makeChange(13, delta::Store{{macs::Envelope{}}})) {
            out++ = *opt;
        }
    }));

    const auto res = method(changelog::Request{"uid", 10, 2, {}, CheckRevisionPolicy::loyal});

    ASSERT_EQ(res.value().changes.size(), 2ul);
    EXPECT_EQ(res.value().changes[0].revision, 12);
    EXPECT_EQ(res.value().changes[1].revision, 13);
}

} // namespace
