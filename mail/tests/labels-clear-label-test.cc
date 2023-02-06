#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/tests/mocking-labels.h>
#include "compare_labels.h"

namespace {
    using namespace macs;
    using namespace testing;

    struct LabelsClearLabelTest: public LabelsRepositoryTest {
        LabelsClearLabelTest(void) : label(labels.label("1", "label", "red")) {}

        Label label;
    };

    TEST_F(LabelsClearLabelTest, ClearsLabel) {
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));
        EXPECT_CALL(labels, syncClearLabel(label, _)).WillOnce(
                InvokeArgument<1>(macs::error_code(),
                        macs::OnUpdateMessages::second_argument_type{macs::Revision(), 0}));

        labels.clearLabel(label.lid());
    }

    TEST_F(LabelsClearLabelTest, ClearsNonexistingLabel) {
        EXPECT_CALL(labels, syncGetLabels(_)).WillOnce(GiveLabels(label));

        labels.clearLabel("2");
    }

    TEST_F(LabelsClearLabelTest, ClearsLabelWithEmptyLid) {
        labels.clearLabel("");
    }
}
