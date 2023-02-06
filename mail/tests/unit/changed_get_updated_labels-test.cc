#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs_pg/changelog/factory.h>
#include <src/meta/changed.h>
#include "macs_change_io.h"
#include "labels.h"
#include "label_cmp.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing::labels;
using namespace ::doberman::meta::changed;
using namespace ::doberman::meta::changed::detail;

struct GetUpdatedLabelsTest: public Test {
    auto change(std::string arguments) const {
        return ::macs::ChangeFactory().
            changeId(42).
            arguments(arguments).
            release();
    }

    auto seenLabel() const {
        return label("SEEN_LID", Symbol::seen_label);
    }
    auto recentLabel() const {
        return label("RECENT_LID", Symbol::recent_label);
    }
    auto deletedLabel() const {
        return label("DELETED_LID", Symbol::deleted_label);
    }
    auto labelWithLid10() const {
        return label("10", "I like that label");
    }
    auto labelWithLid200() const {
        return label("200", "I hate that label");
    }

    auto labelsDict() const {
        return makeSet(
            seenLabel(),
            recentLabel(),
            deletedLabel(),
            labelWithLid10(),
            labelWithLid200()
        );
    }

    auto get(const std::string& argsJson) {
        return getUpdatedLabels(change(argsJson), labelsDict());
    }
};

TEST_F(GetUpdatedLabelsTest, forChangeWithSetSeenIsTrueReturnAddSeenLabel) {
    const std::string argsJson = R"json({
        "seen": true,
        "recent": null,
        "deleted": null,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), ElementsAre(seenLabel()));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithSetSeenIsFalseReturnRemoveSeenLabel) {
    const std::string argsJson = R"json({
        "seen": false,
        "recent": null,
        "deleted": null,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), IsEmpty());
    EXPECT_THAT(std::get<1>(result), ElementsAre(seenLabel()));
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithSetRecentIsTrueReturnAddRecentLabel) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": true,
        "deleted": null,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), ElementsAre(recentLabel()));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithSetRecentIsFalseReturnRemoveRecentLabel) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": false,
        "deleted": null,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), IsEmpty());
    EXPECT_THAT(std::get<1>(result), ElementsAre(recentLabel()));
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithSetDeletedIsTrueReturnAddDeletedLabel) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": true,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), ElementsAre(deletedLabel()));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithSetDeletedIsFalseReturnRemoveDeletedLabel) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": false,
        "lids_add": [],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), IsEmpty());
    EXPECT_THAT(std::get<1>(result), ElementsAre(deletedLabel()));
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithLidsAddReturnThem) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": null,
        "lids_add": [10, 200],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), UnorderedElementsAre(labelWithLid10(), labelWithLid200()));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithLidsAddReturnThemWithNotFound) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": null,
        "lids_add": [10, 5555],
        "lids_del": []
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), ElementsAre(labelWithLid10()));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
    EXPECT_THAT(std::get<2>(result), ElementsAre("5555"));
}

TEST_F(GetUpdatedLabelsTest, forChangeWithLidsDelReturnThem) {
    const std::string argsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": null,
        "lids_add": [],
        "lids_del": [10, 200]
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), IsEmpty());
    EXPECT_THAT(std::get<1>(result), UnorderedElementsAre(labelWithLid10(), labelWithLid200()));
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

TEST_F(GetUpdatedLabelsTest, forChangeWithFlagsAndLidsComposeThem) {
    const std::string argsJson = R"json({
        "seen": true,
        "recent": false,
        "deleted": null,
        "lids_add": [10],
        "lids_del": [200]
    })json";
    auto result = get(argsJson);
    EXPECT_THAT(std::get<0>(result), UnorderedElementsAre(seenLabel(), labelWithLid10()));
    EXPECT_THAT(std::get<1>(result), UnorderedElementsAre(recentLabel(), labelWithLid200()));
    EXPECT_THAT(std::get<2>(result), IsEmpty());
}

}
