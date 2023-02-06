#include <crypta/rt_socdem/lib/cpp/model/socdem_evaluator.h>
#include <crypta/rt_socdem/lib/cpp/model/test_utils/utils.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <util/generic/hash.h>
#include <util/stream/file.h>

using namespace NCrypta::NRtSocdem::NBigb;

TEST(TSocdemEvaluator, Get) {
    const auto& profile = NTestUtils::PrepareTestProfile();
    const auto& model = NTestUtils::GetTestModel();

    TSocdemEvaluator evaluator(model, profile);

    const auto& gender = evaluator.GetGender();
    EXPECT_EQ(0u, gender.Class);
    const TVector<ui64> referenceGenderWeights{847773, 152226};
    EXPECT_EQ(referenceGenderWeights, gender.Weights);

    const auto& age = evaluator.GetAge();
    EXPECT_EQ(2u, age.Class);
    const TVector<ui64> referenceAgeWeights{2465, 62587, 539611, 255369, 103765, 36200};
    EXPECT_EQ(referenceAgeWeights, age.Weights);

    const auto& income = evaluator.GetIncome();
    EXPECT_EQ(3u, income.Class);
    const TVector<ui64> referenceIncomeWeights{32833, 119157, 218252, 467112, 162644};
    EXPECT_EQ(referenceIncomeWeights, income.Weights);

    const auto& income3 = evaluator.GetIncome3();
    EXPECT_EQ(2u, income3.Class);
    const TVector<ui64> referenceIncome3Weights{32833, 337409, 629756};
    EXPECT_EQ(referenceIncome3Weights, income3.Weights);
}
