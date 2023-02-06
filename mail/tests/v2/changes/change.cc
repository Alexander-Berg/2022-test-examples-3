#include <mail/hound/include/internal/v2/changes/change.h>
#include <macs_pg/changelog/factory.h>
#include <macs/label_factory.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

namespace changes = hound::v2::changes;
namespace changelog = changes::changelog;
namespace delta = changes::delta;

using namespace ::testing;
using namespace std::literals;

struct MailboxMock {
    MOCK_METHOD(std::vector<macs::Envelope>, envelopes_, (const std::vector<macs::Mid>&), (const));
    template <typename Range, typename Out>
    void envelopes(Range&& mids, Out out) const {
        boost::copy(envelopes_({std::begin(mids), std::end(mids)}), out);
    }

    MOCK_METHOD(std::vector<macs::Label>, labels_, (const std::vector<macs::Lid>&), (const));
    template <typename Range, typename Out>
    void labels(Range&& lids, Out out) const {
        boost::copy(labels_({std::begin(lids), std::end(lids)}), out);
    }

    MOCK_METHOD(std::vector<macs::Folder>, folders_, (const std::vector<macs::Fid>&), (const));
    template <typename Range, typename Out>
    void folders(Range&& fids, Out out) const {
        boost::copy(folders_({std::begin(fids), std::end(fids)}), out);
    }

    MOCK_METHOD(std::vector<macs::Tab>, tabs_, (const std::vector<std::string>&), (const));
    template <typename Range, typename Out>
    void tabs(Range&& types, Out out) const {
        boost::copy(tabs_({std::begin(types), std::end(types)}), out);
    }

    MOCK_METHOD(macs::LabelSet, getLabelsDict, (), (const));
};


struct ChangeComposer : public Test {

    using Sym = macs::Label::Symbol;

    macs::Label fake(const std::string& lid, const Sym & sym) {
        return macs::LabelFactory().type(macs::Label::Type::system).symbol(sym)
                .name(lid).lid(lid).product();
    }

    ChangeComposer() {
        labelsDict.insert({"DELETED", fake("DELETED", Sym::deleted_label)});
        labelsDict.insert({"RECENT", fake("RECENT", Sym::recent_label)});
        labelsDict.insert({"SEEN", fake("SEEN", Sym::seen_label)});
    }

    using change_type = macs::pg::ChangeType;

    auto makeChange(macs::ChangeId cid, macs::Revision r, change_type type,
            boost::optional<std::string> changed,
            boost::optional<std::string> arguments) {
        return macs::ChangeFactory{}
            .changeId(cid).uid("333")
            .type(type)
            .revision(r)
            .changed(std::move(changed))
            .arguments(std::move(arguments))
            .release();
    }
    macs::LabelSet labelsDict;
};

TEST_F(ChangeComposer, should_return_none_for_uninteresting_change) {
    const auto store = makeChange(1, 10, change_type::removeArchivationRule,
        R"json([])json"s,
        boost::none);

    MailboxMock mailbox;
    const auto compose = changes::makeChangeComposer(mailbox);
    std::vector<hound::v2::changes::Change> result;
    compose(std::vector<macs::Change>{store}, std::back_inserter(result));
    EXPECT_TRUE(result.empty());
}

TEST_F(ChangeComposer, should_compose_store_delta_for_store_change) {
    const auto store = makeChange(1, 10, change_type::store,
        R"json([{
            "mid": 100500,
            "tid": 1200,
            "fid": 1,
            "imap_id": 999,
            "seen": true,
            "recent": false,
            "deleted": false,
            "lids": [1, 3, 5, 6, 30],
            "hdr_message_id":"hdr_message_id",
            "fresh_count": 1,
            "revision": 10
        }])json"s,
        boost::none);

    MailboxMock mailbox;
    const auto compose = changes::makeChangeComposer(mailbox);
    EXPECT_CALL(mailbox, envelopes_(ElementsAre("100500")))
        .WillOnce(Return(std::vector<macs::Envelope>{macs::EnvelopeFactory().mid("100500").release()}));

    std::vector<hound::v2::changes::Change> result;
    compose(std::vector<macs::Change>{store}, std::back_inserter(result));
    EXPECT_EQ(1ul, result.size());
    EXPECT_EQ(result[0].revision, 10);
    EXPECT_EQ(result[0].type, "store");
    EXPECT_TRUE(boost::get<delta::Value<delta::Store>>(&(result[0].value)));
}

TEST_F(ChangeComposer, should_skip_delta_for_store_change_with_no_envelope_found) {
    const auto store = makeChange(1, 10, change_type::store,
        R"json([{
            "mid": 100500,
            "tid": 1200,
            "fid": 1,
            "imap_id": 999,
            "seen": true,
            "recent": false,
            "deleted": false,
            "lids": [1, 3, 5, 6, 30],
            "hdr_message_id":"hdr_message_id",
            "fresh_count": 1,
            "revision": 10
        }])json"s,
        boost::none);

    MailboxMock mailbox;
    const auto compose = changes::makeChangeComposer(mailbox);
    EXPECT_CALL(mailbox, envelopes_(ElementsAre("100500")))
        .WillOnce(Return(std::vector<macs::Envelope>{}));

    std::vector<hound::v2::changes::Change> result;
    compose(std::vector<macs::Change>{store}, std::back_inserter(result));
    EXPECT_TRUE(result.empty());
}

TEST_F(ChangeComposer, should_compose_tab_delta_for_tab_create_change) {
    const auto create = makeChange(1, 10, change_type::tabCreate,
        R"json([{ "tab": "relevant" }])json"s,
        boost::none);

    MailboxMock mailbox;
    const auto compose = changes::makeChangeComposer(mailbox);
    EXPECT_CALL(mailbox, tabs_(ElementsAre("relevant")))
        .WillOnce(Return(std::vector<macs::Tab>{macs::Tab{}}));

    std::vector<hound::v2::changes::Change> result;
    compose(std::vector<macs::Change>{create}, std::back_inserter(result));
    EXPECT_FALSE(result.empty());
    EXPECT_EQ(result[0].revision, 10);
    EXPECT_EQ(result[0].type, "tab-create");
    EXPECT_TRUE(boost::get<delta::Value<delta::TabCreate>>(&(result[0].value)));
}

TEST_F(ChangeComposer, should_skip_delta_for_tab_create_change_with_no_tab_found) {
    const auto create = makeChange(1, 10, change_type::tabCreate,
        R"json([{ "tab": "news" }])json"s,
        boost::none);

    MailboxMock mailbox;
    const auto compose = changes::makeChangeComposer(mailbox);
    EXPECT_CALL(mailbox, tabs_(ElementsAre("news")))
        .WillOnce(Return(std::vector<macs::Tab>{}));


    std::vector<hound::v2::changes::Change> result;
    compose(std::vector<macs::Change>{create}, std::back_inserter(result));
    EXPECT_TRUE(result.empty());
}

} // namespace
