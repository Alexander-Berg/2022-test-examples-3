#pragma once

#include "factors.h"
#include "factors_accountant.h"

#include <util/stream/input.h>
#include <utility>

// Main class of check_factor_usage, which use TFactorsAccountant and TFactorsSet
// First one to load all used factors from models.archives
// Second one to get information about all exisiting factors

class TFactorsUsageTester {
private:
    TFactorsSet FactorsSet;
    TFactorsAccountant FactorsAccountant;

    // Parse list with default formulas.
    void ParseDefaultFormulasList(const TString& defaultSources,
            TVector<TString>& result, const TString& sourcePath);

public:
    TFactorsUsageTester(const TString& description);

    // Load information from additional formulas
    void LoadAdditionalFormulas(const char **additionalSources, int numAdditionalSouces);
    // Add additional formula
    void AddFormula(const char* source);
    // Load information from default formulas (library/all_formulas)
    void LoadDefaultFormulas(const TString& formulasList, const TString& sourcesPath, bool justPrintFormulas);
    // Load information from .archive formulas (models.archive file)
    void AddFormulasFromArchive(const TString& archiveFile, bool justPrintFormulas);

    // Check that all TG_UNUSED (but not TG_REMOVED) factors are still used in some formula
    TString TestUnusedFactors();
    // Check all the factors not marked as TG_UNUSED/TG_REMOVED are used in some formula.
    TString TestUnusedButNotMarkedFactors();
    // Check that TG_REMOVED factors aren't used in any formula
    TString TestRemovedFactors();
    // Check that TG_REARR_USE factors are not marked as
    // TG_UNUSED/TG_DEPRECATED/TG_REMOVED and are not used in any formula
    TString TestUnusedRearrangeFactors();
    // Check all the factors marked as TG_REARR_USE and TG_UNUSED/TG_DEPRECATED/TG_REMOVED
    TString TestRearrangeFactors();
    // Check that TG_UNIMPLEMENTED factors aren't used in any formula
    TString TestUnimplementedFactors();
    // Show in which formulas TG_UNUSED marked factors are still in used
    TString GetInfoForUnusedFactors(bool groupByID = false);
    // Show all formulas where specified factor is used.
    TString ShowFactorUsage(const TString& sliceName, size_t factor);
    // Show all factors that shouldn't be used in new formulas
    TString ShowIgnoreFactors();
};
