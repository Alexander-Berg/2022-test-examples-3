#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <library/cpp/json/json_reader.h>

#include "manager.h"

TEST(FormulaMultiplied, TestLoadFromJson) {
    const TString formulaJsonStr = R"(
        {
            "normalize": {
                "nativeScale": 1.421428747e-11,
                "bias": 0.2799630169,
                "nativeBias": 0.2768417291,
                "class": "doubleLinear",
                "scale": 1.513414523e-11
            },
            "properties": {
                "filter": true
            },
            "class": "multiplied",
            "trace": "MATRIXNET_SUM_VALUE",
            "children": [
                {
                    "threshold": 0.415,
                    "model": "0.3",
                    "class": "model",
                    "trace": "MATRIXNET_ASSESSOR_VALUE"
                },
                {
                    "model": "0.5",
                    "class": "model",
                    "trace": "MATRIXNET_CLICK_VALUE"
                }
            ]
        }
    )";
    TStringStream formulaJsonStream(formulaJsonStr);
    NJson::TJsonValue formulaJson;
    NJson::ReadJsonTree(&formulaJsonStream, &formulaJson);

    TFormulaManagerMock manager;
    const auto calculator = manager.CreateFormula(formulaJson);
    EXPECT_NE(calculator.Get(), nullptr);
    EXPECT_NE(dynamic_cast<const NMarketReport::NFormula::TMultiplyCalculator*>(calculator.Get()), nullptr);
}

TEST(FormulaMultiplied, TestCalculateValue) {
    const TString formulaJsonStr = R"(
        {
            "class": "multiplied",
            "children": [
                {
                    "model": "0.3",
                    "class": "model"
                },
                {
                    "model": "0.5",
                    "class": "model"
                }
            ]
        }
    )";
    TStringStream formulaJsonStream(formulaJsonStr);
    NJson::TJsonValue formulaJson;
    NJson::ReadJsonTree(&formulaJsonStream, &formulaJson);

    TFormulaManagerMock manager;
    const auto calculator = manager.CreateFormula(formulaJson);
    EXPECT_DOUBLE_EQ(calculator->Calculate(nullptr).GetValue(), 0.3f * 0.5f);
}
