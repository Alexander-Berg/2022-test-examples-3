#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-labels.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace macs;
    using namespace testing;
    using namespace std;

    struct LabelsGettingLabelTest: public LabelsRepositoryTest {
    };

    TEST_F(LabelsGettingLabelTest, FindsLabelByLid) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_THAT(labels.getLabelByLid("1"), matchLabel(label));
    }

    TEST_F(LabelsGettingLabelTest, FindsNonexistingLabelByLid) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_THROW_SYS(labels.getLabelByLid("2"), macs::error::noSuchLabel,
                         "LabelSet::at: no lid '2': no such label");
    }

    TEST_F(LabelsGettingLabelTest, FindsLabelsByLids) {
        Label label1 = labels.label("1", "label1", "red");
        Label label2 = labels.label("2", "label2", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label1, label2));

        std::vector<std::string> lids;
        lids.push_back("1");
        lids.push_back("2");
        std::vector<Label> lbls;
        labels.getLabelsByLids(lids.begin(), lids.end(), std::back_inserter(lbls));
        ASSERT_THAT(lbls, ElementsAre(matchLabel(label1), matchLabel(label2)));
    }

    TEST_F(LabelsGettingLabelTest, FindsNonexistingLabelsByLids) {
        Label label1 = labels.label("1", "label1", "red");
        Label label2 = labels.label("2", "label2", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label1, label2));

        std::vector<std::string> lids;
        lids.push_back("1");
        lids.push_back("2");
        lids.push_back("3");
        std::vector<Label> lbls;
        ASSERT_THROW_SYS(labels.getLabelsByLids(lids.begin(), lids.end(), std::back_inserter(lbls)),
                         macs::error::noSuchLabel,
                         "LabelSet::at: no lid '3': no such label");
    }

    TEST_F(LabelsGettingLabelTest, FindsLabelLidByName) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_EQ("1", labels.getLabelLidByNameAndType("label", Label::Type::user));
    }

    TEST_F(LabelsGettingLabelTest, CheckIfLabelNameExistsDirectly) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_TRUE(labels.existLabelNameAndType("label", Label::Type::user));
    }

    TEST_F(LabelsGettingLabelTest, CheckIfLabelNameNotExistsDirectly) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_FALSE(labels.existLabelNameAndType("zzz", Label::Type::user));
    }

    TEST_F(LabelsGettingLabelTest, GetsOnlyLabels) {
        Label label = labels.label("1", "label", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        ASSERT_THAT(labels.getAllLabels(),
                    ElementsAre(Field(&pair<string, Label>::second,
                                      matchLabel(label))));
    }

    TEST_F(LabelsGettingLabelTest, FindsLabelLidBySymbol) {
        Label label = labels.label("1", "seen", "red").symbol(Label::Symbol::seen_label);

        EXPECT_CALL(labels, syncGetLabels(_)).WillRepeatedly(GiveLabels(label));

        ASSERT_EQ("1", labels.getLabelLidBySymbol(Label::Symbol::seen_label));
    }

    TEST_F(LabelsGettingLabelTest, FindsLabelBySymbol) {
        Label label = labels.label("1", "seen", "red").symbol(Label::Symbol::seen_label);

        EXPECT_CALL(labels, syncGetLabels(_)).WillRepeatedly(GiveLabels(label));

        ASSERT_THAT(labels.getLabelBySymbol(Label::Symbol::seen_label),
                    matchLabel(label));
    }

    TEST_F(LabelsGettingLabelTest, FindsNonexistingLabelBySymbol) {
        Label label = labels.label("1", "seen", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillRepeatedly(GiveLabels(label));

        ASSERT_THROW_SYS(labels.getLabelBySymbol(Label::Symbol::seen_label),
                         macs::error::noSuchLabel,
                         "LabelSet::at: no symbol 'seen_label': no such label");
    }

    TEST_F(LabelsGettingLabelTest, FindsNonexistingSymbol) {
        Label label = labels.label("1", "seen", "red");

        EXPECT_CALL(labels, syncGetLabels(_)).WillRepeatedly(GiveLabels(label));

        ASSERT_EQ("", labels.getLabelLidBySymbol(Label::Symbol::recent_label));
    }

    TEST_F(LabelsGettingLabelTest, FindsSymbolNone) {
        ASSERT_EQ("", labels.getLabelLidBySymbol(Label::Symbol::none));
    }
}
