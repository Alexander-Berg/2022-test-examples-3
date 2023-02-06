#include <gtest/gtest.h>
#include <macs/tests/mocking-labels.h>
#include "throw-wmi-helper.h"


namespace {
    using namespace ::testing;
    using namespace ::std;

    struct LabelsSaveLabelTest: public LabelsRepositoryTest {
        LabelsSaveLabelTest(void)
            : red(labels.label("1", "label", "red")),
              blue(labels.label("2", "name", "blue"))
        {}

        macs::Label red, blue;
    };

    TEST_F(LabelsSaveLabelTest, CreatesLabel) {
        InSequence s;

        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));

        EXPECT_CALL(labels, syncCreateLabel("name", "blue", _, _)).
                WillOnce(InvokeArgument<3>(macs::error_code(), blue));

        macs::Label label = labels.createLabel("name", "blue");

        ASSERT_EQ("name", label.name());
        ASSERT_EQ("blue", label.color());
    }

    TEST_F(LabelsSaveLabelTest, CreateLabelWithNoName) {
        ASSERT_THROW_SYS(labels.createLabel("","black"),
                         macs::error::invalidArgument,
                         "can't create label "
                         "with no name: invalid argument");
    }

    TEST_F(LabelsSaveLabelTest, CreateLabelWithLongName) {
        string longName(macs::Label::maxLabelNameLength() + 1, 'x');
        ASSERT_THROW_SYS(labels.createLabel(longName, "blue"),
                         macs::error::invalidArgument,
                         "Label name too long"
                         ": invalid argument");
    }

    TEST_F(LabelsSaveLabelTest, SaveLabelWithInvalidName) {
        const macs::Label correctedLabel(labels.label("2", "мет?ка", ""));
        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncCreateLabel("мет?ка", "", _, _)).
                WillOnce(InvokeArgument<3>(macs::error_code(), correctedLabel));

        string invalidName = "мет\x85ка";
        labels.createLabel(invalidName, "");
    }

    TEST_F(LabelsSaveLabelTest, SaveLabelWithSpacesInName) {
        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncCreateLabel("name", "blue", _, _)).
                WillOnce(InvokeArgument<3>(macs::error_code(), blue));


        labels.createLabel("   name    ", "blue");
    }
}
