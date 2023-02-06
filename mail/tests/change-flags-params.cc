#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/envelope/repository.h>
#include <macs/label_factory.h>
#include <list>

namespace {

using namespace testing;
using namespace macs::pg;

struct ChangeLabelsParamsTest : public Test {
    template <typename T, typename P>
    static auto get(P&& v) { return std::get<T>(v).value; }

    template <typename T, typename P>
    static bool presents(P&& v) { return !!get<T>(v); }

    static macs::Label createSystem(const macs::Lid& fakeLid, const macs::Label::Symbol& symbol) {
        macs::LabelFactory labelFactory;
        labelFactory.create().lid(fakeLid).symbol(symbol);
        return labelFactory.product();
    }

    static macs::Label createUser(const macs::Lid& lid, const std::string& name, const macs::Label::Type& type) {
        macs::LabelFactory labelFactory;
        labelFactory.create().lid(lid).type(type).name(name);
        return labelFactory.product();
    }
};




TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_AddSeen) {
    std::list<macs::Label> labels {
        createSystem("FAKE_SEEN_LBL", macs::Label::Symbol::seen_label)
    };
    const auto params = createChangeLabelsParams(labels, true);
    EXPECT_TRUE(get<query::LabelIdList>(params).empty());
    EXPECT_TRUE(*get<query::SetSeen>(params));
    EXPECT_FALSE(presents<query::SetRecent>(params));
    EXPECT_FALSE(presents<query::SetDeleted>(params));
}

TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_RemoveSeen) {
    std::list<macs::Label> labels {
        createSystem("FAKE_SEEN_LBL", macs::Label::Symbol::seen_label)
    };
    const auto params = createChangeLabelsParams(labels, false);
    EXPECT_TRUE(get<query::LabelIdList>(params).empty());
    EXPECT_FALSE(*get<query::SetSeen>(params));
    EXPECT_FALSE(presents<query::SetRecent>(params));
    EXPECT_FALSE(presents<query::SetDeleted>(params));
}

TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_AddRecentDeleted) {
    std::list<macs::Label> labels {
        createSystem("FAKE_RECENT_LBL", macs::Label::Symbol::recent_label),
        createSystem("FAKE_DELETED_LBL", macs::Label::Symbol::deleted_label)
    };
    const auto params = createChangeLabelsParams(labels, true);
    EXPECT_TRUE(get<query::LabelIdList>(params).empty());
    EXPECT_FALSE(presents<query::SetSeen>(params));
    EXPECT_TRUE(*get<query::SetRecent>(params));
    EXPECT_TRUE(*get<query::SetDeleted>(params));
}

TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_RemoveRecentSeen) {
    std::list<macs::Label> labels {
        createSystem("FAKE_RECENT_LBL", macs::Label::Symbol::recent_label),
        createSystem("FAKE_SEEN_LBL", macs::Label::Symbol::seen_label)
    };
    const auto params = createChangeLabelsParams(labels, false);
    EXPECT_TRUE(get<query::LabelIdList>(params).empty());
    EXPECT_FALSE(*get<query::SetSeen>(params));
    EXPECT_FALSE(*get<query::SetRecent>(params));
    EXPECT_FALSE(presents<query::SetDeleted>(params));
}

TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_AddUserDeleted) {
    std::list<macs::Label> labels {
        createSystem("FAKE_DELETED_LBL", macs::Label::Symbol::deleted_label),
        createUser("13", "Work", macs::Label::Type::user),
        createUser("15", "Family", macs::Label::Type::user),
        createUser("10", "vkontakte", macs::Label::Type::social)
    };
    const auto params = createChangeLabelsParams(labels, true);
    EXPECT_FALSE(presents<query::SetSeen>(params));
    EXPECT_FALSE(presents<query::SetRecent>(params));
    EXPECT_TRUE(*get<query::SetDeleted>(params));
    ASSERT_THAT(get<query::LabelIdList>(params), ElementsAre("13", "15", "10"));
}

TEST_F(ChangeLabelsParamsTest, pg_change_labels_params_RemoveSeenRecentUser) {
    std::list<macs::Label> labels {
        createUser("13", "Work", macs::Label::Type::user),
        createSystem("FAKE_RECENT_LBL", macs::Label::Symbol::recent_label),
        createUser("10", "vkontakte", macs::Label::Type::social),
        createSystem("FAKE_SEEN_LBL", macs::Label::Symbol::seen_label)
    };
    const auto params = createChangeLabelsParams(labels, false);
    EXPECT_FALSE(*get<query::SetSeen>(params));
    EXPECT_FALSE(*get<query::SetRecent>(params));
    EXPECT_FALSE(presents<query::SetDeleted>(params));
    ASSERT_THAT(get<query::LabelIdList>(params), ElementsAre("13", "10"));
}

}

