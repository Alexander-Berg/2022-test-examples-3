#include <mail/hound/include/internal/v2/changes/changelog.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs_pg/changelog/factory.h>
#include <macs/label_factory.h>

namespace {

namespace changes = hound::v2::changes;
namespace changelog = changes::changelog;

struct FormJsonMock {
    MOCK_METHOD(void, call, (), (const));
};

struct TestChangeDataType {
    FormJsonMock* mock = nullptr;
    friend void fromJson(const std::string&, TestChangeDataType self) {
        self.mock->call();
    }
};

struct TestChangeArgumentsOnly {
    TestChangeDataType arguments;
};

static_assert(changelog::has_arguments<TestChangeArgumentsOnly>::value,
    "has_arguments check failed");

static_assert(!changelog::has_changed<TestChangeArgumentsOnly>::value,
    "has_changed check failed");

struct TestChangeChangedOnly {
    TestChangeDataType changed;
};

static_assert(!changelog::has_arguments<TestChangeChangedOnly>::value,
    "has_arguments check failed");

static_assert(changelog::has_changed<TestChangeChangedOnly>::value,
    "has_changed check failed");

using namespace ::testing;

TEST(parse, should_call_fromJson_for_arguments_for_change_with_arguments_field) {
    using namespace std::literals;
    FormJsonMock mock;
    TestChangeArgumentsOnly change{{&mock}};
    macs::Change in = macs::ChangeFactory().arguments("{}"s).release();
    EXPECT_CALL(mock, call()).WillOnce(Return());
    changelog::parse(in, change);
}

TEST(parse, should_call_fromJson_for_changed_for_change_with_changed_field) {
    using namespace std::literals;
    FormJsonMock mock;
    TestChangeChangedOnly change{{&mock}};
    macs::Change in = macs::ChangeFactory().changed("{}"s).release();
    EXPECT_CALL(mock, call()).WillOnce(Return());
    changelog::parse(in, change);
}

TEST(parse, should_throw_exception_if_arguments_is_empty_for_change_with_arguments_field) {
    FormJsonMock mock;
    TestChangeArgumentsOnly change{{&mock}};
    macs::Change in;
    EXPECT_THROW(changelog::parse(in, change), std::invalid_argument);
}

TEST(parse, should_throw_exception_if_changed_is_empty_for_change_with_changed_field) {
    FormJsonMock mock;
    TestChangeChangedOnly change{{&mock}};
    macs::Change in;
    EXPECT_THROW(changelog::parse(in, change), std::invalid_argument);
}

struct extractLids : public Test {

    using Sym = macs::Label::Symbol;

    macs::Label fake(const std::string& lid, const Sym & sym) {
        return macs::LabelFactory().type(macs::Label::Type::system).symbol(sym)
                .name(lid).lid(lid).product();
    }

    extractLids() {
        labelsDict.insert({"DELETED", fake("DELETED", Sym::deleted_label)});
        labelsDict.insert({"RECENT", fake("RECENT", Sym::recent_label)});
        labelsDict.insert({"SEEN", fake("SEEN", Sym::seen_label)});
    }

    macs::LabelSet labelsDict;
};


TEST_F(extractLids, should_extract_labels_lids_from_change) {
    changelog::ShortChangedMessage data{0, 0, 0, false, false, false, {1,2,3,6}, ""};
    const auto lids = changelog::extractLids(data, labelsDict);
    EXPECT_THAT(lids, ElementsAre("1", "2", "3", "6"));
}

TEST_F(extractLids, should_extract_seen_flag_label_lid_from_change) {
    changelog::ShortChangedMessage data{0, 0, 0, true, false, false, {}, ""};
    const auto lids = changelog::extractLids(data, labelsDict);
    EXPECT_THAT(lids, ElementsAre("SEEN"));
}

TEST_F(extractLids, should_extract_recent_flag_label_lid_from_change) {
    changelog::ShortChangedMessage data{10, 20, 30, false, true, false, {}, ""};
    const auto lids = changelog::extractLids(data, labelsDict);
    EXPECT_THAT(lids, ElementsAre("RECENT"));
}

TEST_F(extractLids, should_extract_deleted_flag_label_lid_from_change) {
    changelog::ShortChangedMessage data{0, 0, 0, false, false, true, {}, ""};
    const auto lids = changelog::extractLids(data, labelsDict);
    EXPECT_THAT(lids, ElementsAre("DELETED"));
}

}

