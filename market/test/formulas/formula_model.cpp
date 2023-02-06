#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <library/cpp/json/json_reader.h>

#include "manager.h"

TEST(FormulaModel, TestLoadFromJson) {
    const TString formulaJsonStr = R"(
        {
            "normalize": {
                "class": "sigmoid"
            },
            "model": "0.42",
            "class": "model"
        }
    )";
    TStringStream formulaJsonStream(formulaJsonStr);
    NJson::TJsonValue formulaJson;
    NJson::ReadJsonTree(&formulaJsonStream, &formulaJson);

    TFormulaManagerMock manager;
    const auto calculator = manager.CreateFormula(formulaJson);
    EXPECT_NE(calculator.Get(), nullptr);
    EXPECT_NE(dynamic_cast<const NMarketReport::NFormula::TModelCalculator*>(calculator.Get()), nullptr);
}

TEST(FormulaModel, TestMockedValue) {
    const TString formulaJsonStr = R"(
        {
            "model": "0.42",
            "class": "model"
        }
    )";
    TStringStream formulaJsonStream(formulaJsonStr);
    NJson::TJsonValue formulaJson;
    NJson::ReadJsonTree(&formulaJsonStream, &formulaJson);

    TFormulaManagerMock manager;
    const auto calculator = manager.CreateFormula(formulaJson);
    EXPECT_DOUBLE_EQ(calculator->Calculate(nullptr).GetValue(), 0.42f);
}
