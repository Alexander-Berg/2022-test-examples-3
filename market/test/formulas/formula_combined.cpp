#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <library/cpp/json/json_reader.h>

#include "manager.h"

TEST(FormulaCombined, TestLoadFromJson) {
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
            "class": "combined",
            "trace": "MATRIXNET_SUM_VALUE",
            "children": [
                {
                    "coefficient": 0.15,
                    "threshold": 0.415,
                    "model": "0.3",
                    "class": "model",
                    "trace": "MATRIXNET_ASSESSOR_VALUE"
                },
                {
                    "coefficient": 0.85,
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
    EXPECT_NE(dynamic_cast<const NMarketReport::NFormula::TCombineCalculator*>(calculator.Get()), nullptr);
}

TEST(FormulaCombined, TestCalculateValue) {
    const TString formulaJsonStr = R"(
        {
            "class": "combined",
            "children": [
                {
                    "coefficient": 0.1,
                    "model": "0.3",
                    "class": "model"
                },
                {
                    "coefficient": 0.4,
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
    EXPECT_DOUBLE_EQ(calculator->Calculate(nullptr).GetValue(), 0.1f * 0.3f + 0.4f * 0.5f);
}
