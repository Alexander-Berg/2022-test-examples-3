#include <crypta/prism/lib/model/validate_model.h>
#include <crypta/prism/lib/model/test_utils/utils.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <util/generic/hash.h>
#include <util/stream/file.h>

using namespace NCrypta;
using namespace NCrypta::NPrism;

TEST(ValidateModel, ValidModel) {
    const auto& model = NTestUtils::GetTestModel();

    ASSERT_NO_THROW(ValidateModel(model));
}

TEST(ValidateModel, InvalidMapping) {
    auto model = NTestUtils::GetTestModel();
    model.CatboostFeaturesCalculator = TCatboostFeaturesCalculator({}, {}, {});

    ASSERT_THROW(ValidateModel(model), yexception);
}

TEST(ValidateModel, InvalidThresholds) {
    auto model = NTestUtils::GetTestModel();
    model.Thresholds.Clear();

    ASSERT_THROW(ValidateModel(model), yexception);
}

