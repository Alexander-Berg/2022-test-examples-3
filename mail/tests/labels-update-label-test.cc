#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-labels.h>
#include "throw-wmi-helper.h"
#include "compare_labels.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct LabelsUpdateLabelTest: public LabelsRepositoryTest {
        LabelsUpdateLabelTest(void)
            : red(labels.label("1", "label", "red"))
        {}

        Label red;
    };

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelName) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.name("name");
        label = factory.product();

        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncModifyLabel(label, _)).WillOnce(
                InvokeArgument<1>(macs::error_code(), label));

        labels.updateLabel(label);
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelDirectly) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.name("name");
        factory.color("blue");
        label = factory.product();

        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncModifyLabel(label, _)).WillOnce(
                InvokeArgument<1>(macs::error_code(), label));

        labels.updateLabel(label);
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesSystemLabelName) {
        Label system = labels.system(labels.label("1", "label", "red"));
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.name("name");
        label = factory.product();

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(system));

        ASSERT_THROW_SYS(labels.updateLabel(label),
                         macs::error::cantModifyLabel,
                         "can't rename non-user label 1"
                         ": can not modify label");
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelNameToEmpty) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.name("");
        label = factory.product();

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        ASSERT_THROW_SYS(labels.updateLabel(label),
                         macs::error::invalidArgument,
                         "can't rename label 1 to empty name"
                         ": invalid argument");
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelColor) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.color("blue");
        label = factory.product();

        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncModifyLabel(label, _)).WillOnce(
                InvokeArgument<1>(macs::error_code(), label));

        labels.updateLabel(label);
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesNonexistingLabel) {
        std::vector<macs::Label> empty;
        EXPECT_CALL(labels, syncGetLabels(_)).Times(AnyNumber())
            .WillRepeatedly(GiveLabels(empty));

        ASSERT_THROW_SYS(labels.updateLabel(labels.label("1")),
                         macs::error::noSuchLabel,
                         "can't update label with lid '1'"
                         ": no such label");
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelWithLongName) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        std::string longName(macs::Label::maxLabelNameLength() + 1, 'x');
        factory.name(longName);
        label = factory.product();

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        ASSERT_THROW_SYS(labels.updateLabel(label),
                         macs::error::invalidArgument,
                         "can't rename label 1: too long name"
                         ": invalid argument");
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelWithInvalidName) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        string invalidName = "мет\x85ка";
        factory.name(invalidName);
        label = factory.product();

        ASSERT_EQ(label.name(), "мет?ка");
    }

    TEST_F(LabelsUpdateLabelTest, UpdatesLabelWithSpacesInName) {
        Label label = red;
        LabelFactory factory = LabelFactory().create(label);
        factory.name("  name  ");
        label = factory.product();

        ASSERT_EQ(label.name(), "name");
    }
}
