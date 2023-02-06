#include <gtest/gtest.h>
#include <macs/tests/mocking-labels.h>
#include "throw-wmi-helper.h"


namespace {
using namespace ::testing;

struct LabelsGetOrCreateLabelTest: public LabelsRepositoryTest {
    LabelsGetOrCreateLabelTest() {}

    void SetUp() override {
        red = labels.label("1", "label", "red");
        blue = labels.label("2", "name", "blue");
    }

    macs::Label red;
    macs::Label blue;
};

TEST_F(LabelsGetOrCreateLabelTest, shouldCreateLabel) {
    InSequence s;
    EXPECT_CALL(labels, syncGetOrCreateLabel("name", "blue", _, _)).
            WillOnce(InvokeArgument<3>(macs::error_code(), blue));

    macs::Label label = labels.getOrCreateLabel("name", "blue");

    ASSERT_EQ("name", label.name());
}

TEST_F(LabelsGetOrCreateLabelTest, shouldGetExistingLabel) {
    InSequence s;
    EXPECT_CALL(labels, syncGetLabels(_)).
            WillOnce(GiveLabels(blue));

    const auto labelsCache = labels.getAllLabels();
    macs::Label label = labels.getOrCreateLabel("name", "blue");

    ASSERT_EQ("name", label.name());
}

TEST_F(LabelsGetOrCreateLabelTest, shouldThrowAnExceptionOnCreatingLabelWithoutName) {
    ASSERT_THROW_SYS(labels.getOrCreateLabel("","black"),
                     macs::error::invalidArgument,
                     "can't create label "
                     "with no name: invalid argument");
}

TEST_F(LabelsGetOrCreateLabelTest, shouldThrowAnExceptionOnCreatingLabelWithLongName) {
    std::string longName(macs::Label::maxLabelNameLength() + 1, 'x');
    ASSERT_THROW_SYS(labels.getOrCreateLabel(longName, "blue"),
                     macs::error::invalidArgument,
                     "Label name too long"
                     ": invalid argument");
}

TEST_F(LabelsGetOrCreateLabelTest, shouldCreateLabelWithCorrectedInvalidName) {
    const macs::Label correctedLabel(labels.label("2", "мет?ка", ""));
    InSequence s;
    EXPECT_CALL(labels, syncGetOrCreateLabel("мет?ка", "", _, _)).
            WillOnce(InvokeArgument<3>(macs::error_code(), correctedLabel));

    std::string invalidName = "мет\x85ка";
    labels.getOrCreateLabel(invalidName, "");
}

TEST_F(LabelsGetOrCreateLabelTest, shouldCreateLabelWithTrimmedSpacesInName) {
    InSequence s;
    EXPECT_CALL(labels, syncGetOrCreateLabel("name", "blue", _, _)).
            WillOnce(InvokeArgument<3>(macs::error_code(), blue));

    labels.getOrCreateLabel("   name    ", "blue");
}

}
