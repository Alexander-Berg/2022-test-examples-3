#include <mail/hound/include/internal/v2/changes/changelog_to_delta.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

namespace changes = hound::v2::changes;
namespace changelog = changes::changelog;
namespace delta = changes::delta;

using namespace ::testing;

struct MailboxMock {
    MOCK_METHOD(std::vector<macs::Envelope>, envelopes_, (std::vector<macs::Mid>), (const));
    template <typename Range, typename Out>
    void envelopes(Range&& mids, Out&& out) const {
        boost::copy(envelopes_({std::begin(mids), std::end(mids)}), out);
    }
};

TEST(changelogToDelta, should_convert_changelog_save_into_delta_save) {
    MailboxMock mailbox;
    changelog::Store change;
    using changelog::MidOnly;
    change.changed = decltype(change.changed){MidOnly{1}, MidOnly{2}, MidOnly{3}};
    const delta::Store res = changes::changelogToDelta(change, mailbox, std::unordered_map<macs::Mid, macs::Envelope>{{"1", macs::Envelope{}}});
    EXPECT_EQ(res.value.size(), 1ul);
}

} // namespace
