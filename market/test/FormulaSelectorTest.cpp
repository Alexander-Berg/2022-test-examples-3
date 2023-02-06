#include <market/library/recom/src/FormulaSelector.h>
#include <market/library/recom/matrixnet/FormulaPool.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace
{
    using namespace Market;

    class TDummyFormula : public IMatrixNetFormula
    {
        virtual TStringBuf GetFeatureName(size_t idx) const
        {
            switch(idx)
            {
            case 0:
                return TStringBuf("174:2234");

            case 1:
                return TStringBuf("175:434_avg_diff");

            case 2:
                return TStringBuf("some_another_feature");

            default:
                ythrow yexception() << "Feature name index overflow: " << idx;
            }
        }

        virtual size_t GetFeatureCount() const
        {
            return 3;
        }

        virtual float Calculate(float* factors) const
        {
            return factors[0] + factors[1] + factors[2];
        }
    };

    const char* CryptaJson = R"delimiter(
    {
        "id" : "9713308831394629148",
        "hash" : "73b1dac9",
        "data" : [
           {
              "id" : "1",
              "name" : "big brother",
              "segment" : [
                 {
                    "id" : "174",
                    "name" : "krypta-user-gender",
                    "value" : "2234",
                    "weight" : "10",
                    "time" : "1429232768"
                 },
                 {
                    "id" : "175",
                    "name" : "krypta-user-gender",
                    "value" : "434",
                    "weight" : "20",
                    "time" : "1429232768"
                 }
              ]
           }
        ]
    }
    )delimiter";
}

namespace Market
{
    namespace NRecomMn
    {
        TBuiltInMatrixNetFormulaPool::TBuiltInMatrixNetFormulaPool() {}
        const IMatrixNetFormula& TBuiltInMatrixNetFormulaPool::Get(const TString& id, NLog::TErrorLog*) const
        {
            EXPECT_EQ(id, "5");
            static TDummyFormula dummy;
            return dummy;
        }

        bool TBuiltInMatrixNetFormulaPool::Has(const TString&) const {
            return true;
        }

        extern const TBuiltInMatrixNetFormulaPool FormulaPool = {};
    }
}

TEST(TFormulaSelectorTest, SelectsFormula)
{
    TMatrixNetFormulaSelector selector("5");
    TFeatureMap features;
    features["175:434_avg"] = 30;
    features["max"] = 50;
    features["some_another_feature"] = 2;

    TCryptaProfile profile;
    profile.InitFromJson(CryptaJson, TCryptaProfile::EDefaultValuesFillingPolicy::FILL_FROM_DEFAULT_PROFILE);
    EXPECT_EQ(selector.Calculate(TFeatureMapWrapper(features), profile), 10 + (30 - 20) + 2);
}
