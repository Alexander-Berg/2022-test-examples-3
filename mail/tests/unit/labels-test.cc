#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/meta/labels.h>
#include <macs/label_factory.h>
#include "label_cmp.h"
#include "labels.h"

namespace {

using namespace ::testing;
using namespace ::doberman::meta::labels;
using namespace ::doberman::testing;
using namespace ::doberman::testing::labels;

using Symbol = ::macs::Label::Symbol;
using Type = ::macs::Label::Type;

using ::macs::Label;
using ::macs::Lid;
using Lids = std::vector<Lid>;
using Labels = std::vector<Label>;

struct LabelsTest : public Test {
};

struct CreateSimilarLabelMock {
    MOCK_METHOD(Label, call, (const Label&), (const));
};

TEST_F(LabelsTest, lids2Lebels_withExistingLabels_returnsOutLabels) {
    const auto labels = makeSet(label("1"), label("2"));
    const Lids lids{"1", "2"};
    std::vector<decltype(std::begin(labels))> outLabels;
    std::tie(outLabels, std::ignore) = lids2Labels(labels, lids);
    EXPECT_THAT(outLabels | boost::adaptors::indirected | boost::adaptors::map_values,
            ElementsAre(label("1"), label("2")));
}

TEST_F(LabelsTest, lids2Lebels_withNonExistingLabels_returnsNotFound) {
    const auto labels = makeSet();
    const Lids lids{"1", "2"};
    std::vector<decltype(std::begin(Lids{}))> notFoundIters;
    std::vector<decltype(std::begin(labels))> outLabels;
    std::tie(outLabels, notFoundIters) = lids2Labels(labels, lids);
    EXPECT_TRUE(outLabels.empty());
    EXPECT_THAT(notFoundIters | boost::adaptors::indirected, ElementsAre("1", "2"));
}

TEST_F(LabelsTest, replicate_withExistingLabels_returnsReplicatedLabels) {
    StrictMock<CreateSimilarLabelMock> mock;

    auto srcLabels = Labels{
        label("1", Symbol::seen_label),
        label("2", "so_by_name_and_type", Type::spamDefense)
    };
    auto dstLabelSet = makeSet(
        label("11", Symbol::seen_label),
        label("12", "so_by_name_and_type", Type::spamDefense),
        label("13", "dummy")
    );
    auto res = replicate(dstLabelSet, srcLabels, [&mock](auto arg){ return mock.call(arg);},
            [](const Label& l){ return !l.isUser(); });
    EXPECT_THAT(res, ElementsAre(
            label("11", Symbol::seen_label),
            label("12", "so_by_name_and_type", Type::spamDefense)));
}

TEST_F(LabelsTest, replicate_withNonExistingLabels_callsCreateSimilarLabel) {
    StrictMock<CreateSimilarLabelMock> mock;

    auto srcLabels = Labels{
        label("1", Symbol::seen_label),
        label("2", "so_by_name_and_type", Type::spamDefense)
    };
    auto dstLabelSet = makeSet(
        label("11", Symbol::seen_label),
        label("13", "dummy")
    );

    EXPECT_CALL(mock, call(label("2", "so_by_name_and_type", Type::spamDefense)))
        .WillOnce(Return(label("12", "so_by_name_and_type", Type::spamDefense)));

    auto res = replicate(dstLabelSet, srcLabels, [&mock](auto arg){ return mock.call(arg);},
            [](const Label& l){ return !l.isUser(); });

    EXPECT_THAT(res, ElementsAre(
            label("11", Symbol::seen_label),
            label("12", "so_by_name_and_type", Type::spamDefense)));
}

TEST_F(LabelsTest, replicate_withUserLabels_returnsEmptyLabels) {
    StrictMock<CreateSimilarLabelMock> mock;

    auto srcLabels = Labels{
        label("1", "User1"),
        label("2", "User2")
    };
    auto dstLabelSet = makeSet(
        label("11", "User1"),
        label("12", "User2")
    );

    auto res = replicate(dstLabelSet, srcLabels, [&mock](auto arg){ return mock.call(arg);},
            [](const Label& l){ return !l.isUser(); });
    EXPECT_TRUE(res.empty());
}

TEST_F(LabelsTest, convertLabels_withSourceLabels_setsAppropriatedDestinationLabels) {
    StrictMock<CreateSimilarLabelMock> mock;

    const auto srcLabelsSet = makeSet(
        label("1", Symbol::seen_label),
        label("2", "so", Type::spamDefense),
        label("3", "User2")
    );
    const auto dstLabelSet = makeSet(
        label("11", Symbol::seen_label),
        label("13", "dummy")
    );

    EXPECT_CALL(mock, call(label("2", "so", Type::spamDefense)))
        .WillOnce(Return(label("12", "so", Type::spamDefense)));

    auto envelope = ::macs::EnvelopeFactory{}.addLabelIDs({"1", "2", "3"}).release();
    auto result = convertLabels(envelope, srcLabelsSet, dstLabelSet,
                [&mock](auto arg){ return mock.call(arg);},
                [](const Label& l){ return !l.isUser(); });

    EXPECT_THAT(std::get<0>(result).labels(), ElementsAre("11", "12"));
    EXPECT_THAT(std::get<1>(result), IsEmpty());
}

TEST_F(LabelsTest, convertLabels_withSourceLabels_returnsNotFoundLids) {
    StrictMock<CreateSimilarLabelMock> mock;

    auto envelope = ::macs::EnvelopeFactory{}.addLabelIDs({"1", "2", "3"}).release();
    auto result = convertLabels(envelope, {}, {},
                [&mock](auto arg){ return mock.call(arg);},
                [](const Label& l){ return !l.isUser(); });

    EXPECT_THAT(std::get<0>(result).labels(), IsEmpty());
    EXPECT_THAT(std::get<1>(result), UnorderedElementsAre("1", "2", "3"));
}

TEST(LabelFilter, labelFilterReplicable_returnsFalseOnFilteredSymbol) {
    NotReplicableLabels filtered {
        {"seen_label"},
        {}
    };
    LabelFilter filter(filtered);

    EXPECT_FALSE(filter.replicable(label("1", Symbol::seen_label)));
}

TEST(LabelFilter, labelFilterReplicable_returnsTrueOnNotFilteredSymbol) {
    NotReplicableLabels filtered {
        {"seen_label"},
        {}
    };
    LabelFilter filter(filtered);

    EXPECT_TRUE(filter.replicable(label("1", Symbol::attached_label)));
}

TEST(LabelFilter, labelFilterReplicable_returnsFalseOnFilteredType) {
    NotReplicableLabels filtered {
        {},
        {"user"}
    };
    LabelFilter filter(filtered);

    EXPECT_FALSE(filter.replicable(label("1", "usr", Type::user)));
}

TEST(LabelFilter, labelFilterReplicable_returnsTrueOnNotFilteredType) {
    NotReplicableLabels filtered {
        {},
        {"user"}
    };
    LabelFilter filter(filtered);

    EXPECT_TRUE(filter.replicable(label("1", "so", Type::spamDefense)));
}

}

