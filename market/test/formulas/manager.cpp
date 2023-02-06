#include "manager.h"

#include <util/generic/map.h>
#include <util/generic/serialized_enum.h>

class TModelMock : public NMarket::NMLModel::IModel {
public:
    TModelMock(float value)
        : Value(value)
    {
    }

    float Calc(const float* factors) const override final {
        Y_UNUSED(factors);
        return Value;
    }

    float Calc(const float* numFactors, const TString* catFactors) const override final {
        Y_UNUSED(numFactors);
        Y_UNUSED(catFactors);
        return Value;
    }

private:
    const float Value;
};

class TModelStoreMock : public NMarket::NMLModel::IModelStore {
public:
    const NMarket::NMLModel::IModel& GetModel(const TString& name) override final {
        if (!Models.contains(name)) {
            Models.insert({ name, TModelMock(FromString<float>(name)) });
        }
        return *Models.FindPtr(name);
    }

private:
    TMap<TString, TModelMock> Models;
};

TFormulaManagerMock::TFormulaManagerMock()
    : NMarketReport::NFormula::TManager(NMarketReport::NFormula::EKind::Base)
{
    for (EModelType type : GetEnumAllValues<EModelType>().Materialize()) {
        GetModelStore()[type] = MakeHolder<TModelStoreMock>();
    }
}

const NMarket::NMLModel::IModel& TFormulaManagerMock::CreateModel(const TString& formulaName, EModelType type) {
    return GetModelStore().GetModel(type, formulaName);
}
