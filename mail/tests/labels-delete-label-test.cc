#include <gtest/gtest.h>
#include <macs/tests/mocking-labels.h>
#include "throw-wmi-helper.h"

namespace {
    using namespace ::testing;
    using namespace ::macs;
    using namespace ::std;

    struct LabelsDeleteLabelTest: public LabelsRepositoryTest {
        LabelsDeleteLabelTest()
            : red(labels.label("1", "label", "red")),
              sysred(labels.system(labels.label("1", "label", "red"))),
              threadWideRed(labels.withType(labels.label("1", "label", "red"), Label::Type::threadWide))
        {}

        macs::Label red;
        macs::Label sysred;
        macs::Label threadWideRed;
    };

    TEST_F(LabelsDeleteLabelTest, DeletesLabel) {
        InSequence s;
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(red));
        EXPECT_CALL(labels, syncEraseLabel(red.lid(), _)).WillOnce(
                InvokeArgument<1>(macs::error_code(), macs::NULL_REVISION));

        labels.deleteLabel("1");
    }

    TEST_F(LabelsDeleteLabelTest, DeletesLabelWithoutLid) {
        EXPECT_THROW_SYS(labels.deleteLabel(""),
                         macs::error::invalidArgument,
                         "can't delete label with empty lid"
                         ": invalid argument");
    }

    TEST_F(LabelsDeleteLabelTest, DeletesSystemLabel) {
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(sysred));
        EXPECT_THROW_SYS(labels.deleteLabel("1"),
                         macs::error::cantModifyLabel,
                         "can't delete label 1 "
                         "with type 'system': can not modify label");
    }

    TEST_F(LabelsDeleteLabelTest, DeletesThreadWideLabel) {
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(threadWideRed));
        EXPECT_THROW_SYS(labels.deleteLabel("1"),
                         macs::error::cantModifyLabel,
                         "can't delete label 1 "
                         "with type 'threadWide': can not modify label");
    }
}
