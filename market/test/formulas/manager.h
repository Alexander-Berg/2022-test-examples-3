#pragma once

#include <market/report/library/formula_calc/formula_calc.h>

class TFormulaManagerMock : public NMarketReport::NFormula::TManager {
public:
    TFormulaManagerMock();

    const NMarket::NMLModel::IModel& CreateModel(const TString& formulaName, EModelType type) override final;
};
