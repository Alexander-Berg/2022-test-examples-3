#include "factors_usage_tester.h"

#include <kernel/relevfml/models_archive/models_archive.h>

#include <util/folder/path.h>
#include <util/generic/algorithm.h>
#include <util/generic/singleton.h>
#include <util/stream/output.h>
#include <util/string/cast.h>
#include <util/string/strip.h>
#include <util/string/vector.h>

#include <util/memory/blob.h>
#include <util/system/filemap.h>

TFactorsUsageTester::TFactorsUsageTester(const TString& description) : FactorsSet(description), FactorsAccountant(FactorsSet)
{
}

bool ExpandFileName(TString& filename, const TString& sourceFile) {
    TFsPath filePath(sourceFile);
    if (filename[0] != '/') {
        filename = filePath.Dirname() + '/' + filename;
    }
    if (filename.EndsWith(".cpp")) {
        filename += ".factors";
        return true;
    } else if (filename.EndsWith(".info")) {
        return true;
    } else if (filename.EndsWith(".pln")) {
        return true;
    }
    return false;
}

void TFactorsUsageTester::ParseDefaultFormulasList(const TString& formulasList,
        TVector<TString>& result, const TString& sourcePath)
{
    TVector<TString> files;
    files.push_back(formulasList);
    for (size_t i = 0; i < files.size(); ++i) {
        bool isCMakeFile = false;
        if (files[i].EndsWith(".inc")) {
            isCMakeFile = true;
        }
        TFileInput in(files[i]);
        TString line;
        while (in.ReadLine(line)) {
            line = StripInPlace(line);
            if (isCMakeFile) {
                if (ExpandFileName(line, files[i])) {
                    result.push_back(line);
                }
            } else {
                if (!line.empty() && line[0] != '#') {
                    if (line.EndsWith(".factors") || line.EndsWith(".fml") ||
                            line.EndsWith(".fml2") || line.EndsWith(".fml3") ||
                            line.EndsWith(".info") || line.EndsWith(".pln") ||
                            line.EndsWith(".mnmc")) {
                        result.push_back(sourcePath + line);
                    } else if (line.EndsWith(".inc")) {
                        if (line[0] != '/') {
                            line = sourcePath + line;
                        }
                        files.push_back(line);
                    } else {
                        ythrow yexception() << "Unrecognized file format: " << line;
                    }
                }
            }
        }
    }
}

void TFactorsUsageTester::LoadAdditionalFormulas(const char **additionalSources, int numAdditionalSouces) {
    for (; numAdditionalSouces > 0; --numAdditionalSouces) {
        AddFormula(*additionalSources);
        ++additionalSources;
    }
}

void TFactorsUsageTester::AddFormula(const char* source) {
    FactorsAccountant.AddFormulaIndices(source);
}

void TFactorsUsageTester::LoadDefaultFormulas(const TString& formulasList, const TString& sourcesPath, bool justPrintFormulas) {
    TVector<TString> defaultFiles;
    ParseDefaultFormulasList(formulasList, defaultFiles, sourcesPath);

    for (TVector<TString>::const_iterator i = defaultFiles.begin(); i != defaultFiles.end(); ++i) {
        if (!justPrintFormulas)
            FactorsAccountant.AddFormulaIndices(*i);
        else
            Cout << *i << '\n';
    }
}

// Load information from .archive formulas (models.archive file)
void TFactorsUsageTester::AddFormulasFromArchive(const TString& archiveFile, bool justPrintFormulas) {
    try {
        TFileMap modelsArchive(archiveFile);
        modelsArchive.Map(0, modelsArchive.Length());

        TMap<TString, NMatrixnet::TMnSsePtr> models;
        NModelsArchive::LoadModels(modelsArchive, models, archiveFile);
        for (const auto& model: models) {
            TString formulaName = archiveFile + ":" + model.first;
            if (!justPrintFormulas) {
                FactorsAccountant.AddInfoModelFromArchive(*model.second.Get(), formulaName);
            } else {
                Cout << formulaName << '\n';
            }
        }
    } catch(yexception &e) {
        ythrow e << "Can't load models from archive " << archiveFile.Quote();
    }
}

TString TFactorsUsageTester::TestUnusedFactors() {
    TStringStream errors;
    TVector<NFactorSlices::TFullFactorIndex> unusedFactors;
    FactorsAccountant.GetUnusedFactors(unusedFactors);
    bool isFailed = false;

    for (const auto& factor : unusedFactors) {
        if (FactorsSet.IsDeprecatedFactor(factor) && !FactorsSet.IsRemovedFactor(factor)) {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                        " TG_DEPRECATED factors not encountered in any formula:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice) << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) << '\n';
        }
    }

    return errors.Str();
}

TString TFactorsUsageTester::TestUnusedButNotMarkedFactors() {
    TStringStream errors;
    TVector<NFactorSlices::TFullFactorIndex> unusedFactors;
    FactorsAccountant.GetUnusedFactors(unusedFactors);
    bool isFailed = false;

    for (const auto& factor : unusedFactors) {
        if (!FactorsSet.IsUnusedFactor(factor)) {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                    " Factors not encountered in any formula but not marked as TG_UNUSED/TG_DEPRECATED:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice)  << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) << '\n';
        }
    }

    return errors.Str();
}

TString TFactorsUsageTester::TestRemovedFactors() {
    TStringStream errors;
    TVector<NFactorSlices::TFullFactorIndex> factorsInUse;
    FactorsAccountant.GetFactorsInUse(factorsInUse);
    bool isFailed = false;
    for (const auto& factor : factorsInUse) {
        if (FactorsSet.IsRemovedFactor(factor)) {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                    " TG_REMOVED factors encountered in some formula:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice) << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) <<
                    "\nUsed in:\n\t" << FactorsAccountant.GetFilenames(factor) << "\n\n";
        }
    }
    return errors.Str();
}

TString TFactorsUsageTester::TestUnusedRearrangeFactors() {
    TStringStream errors;
    TVector<NFactorSlices::TFullFactorIndex> unusedFactors;
    FactorsAccountant.GetUnusedFactors(unusedFactors);
    bool isFailed = false;

    for (const auto& factor : unusedFactors) {
        if (FactorsSet.IsRearrangeFactor(factor) &&
                (FactorsSet.IsUnusedFactor(factor) || FactorsSet.IsDeprecatedFactor(factor) ||
                 FactorsSet.IsRemovedFactor(factor)))
        {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                    " Factors marked as TG_REARR_USE but it also marked as"
                    " TG_UNUSED/TG_DEPRECATED/TG_REMOVED and not encountered in any formula:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice) << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) << "\n";
        }
    }

    return errors.Str();
}

TString TFactorsUsageTester::TestRearrangeFactors() {
    TStringStream errors;
    bool isFailed = false;
    const TSet<NFactorSlices::TFullFactorIndex>& allFactors = FactorsSet.GetFactors();
    for (const auto& factor : allFactors) {
        if (FactorsSet.IsRearrangeFactor(factor) &&
            (FactorsSet.IsUnusedFactor(factor) || FactorsSet.IsDeprecatedFactor(factor) ||
             FactorsSet.IsRemovedFactor(factor))) {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                    " Factors marked as TG_REARR_USE but it also marked as TG_UNUSED/TG_DEPRECATED/TG_REMOVED:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice) << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) << "\n";
        }
    }

    return errors.Str();
}

TString TFactorsUsageTester::TestUnimplementedFactors() {
    TStringStream errors;
    TVector<NFactorSlices::TFullFactorIndex> factorsInUse;
    FactorsAccountant.GetFactorsInUse(factorsInUse);
    bool isFailed = false;
    for (const auto& factor : factorsInUse) {
        if (FactorsSet.IsUnimplementedFactor(factor)) {
            if (!isFailed) {
                errors << FactorsSet.GetDescription() <<
                    " TG_UNIMPLEMENTED factors encountered in some formula:\n";
                isFailed = true;
            }
            errors << ToString(factor.Slice)  << ':' << factor.Index << '\t' << FactorsSet.GetFactorName(factor) <<
                "\nUsed in:\n\t" << FactorsAccountant.GetFilenames(factor) << "\n\n";
        }
    }
    return errors.Str();
}

TString TFactorsUsageTester::GetInfoForUnusedFactors(bool groupByID) {
    TString result;
    TVector<NFactorSlices::TFullFactorIndex> factorsInUse;
    FactorsAccountant.GetFactorsInUse(factorsInUse);

    if (groupByID) {
        THashMap<TString, TSet<NFactorSlices::TFullFactorIndex>> fmlToFactors;
        for (const auto& factor : factorsInUse) {
            if (FactorsSet.IsDeprecatedFactor(factor)) {
                TMap<NFactorSlices::TFullFactorIndex, TVector<TString>> factorFormulasInfo;
                FactorsAccountant.GetFilenamesList(factor, factorFormulasInfo);
                for (const auto& factorFormulas : factorFormulasInfo) {
                    const NFactorSlices::TFullFactorIndex& factor = factorFormulas.first;
                    for (const TString& formula : factorFormulas.second) {
                        fmlToFactors[formula].insert(factor);
                    }
                }
            }
        }
        for (const auto& fmlInfo : fmlToFactors) {
            const TString& fmlName = fmlInfo.first;
            result += fmlName + "\nUse deprecated factors:\n";
            for (const auto& factor : fmlInfo.second) {
                result += "\t" + ToString(factor.Slice) + ":" + ToString(factor.Index) + "\t" + FactorsSet.GetFactorName(factor) + "\n";
            }
            result += "\n";
        }
    } else {
        for (const auto& factor : factorsInUse) {
            if (FactorsSet.IsDeprecatedFactor(factor)) {
                result += FactorsSet.GetDescription() + ' ' + ToString(factor.Slice) + ':' + ToString<size_t>(factor.Index) +
                        '\t' + FactorsSet.GetFactorName(factor) +
                        "\nUsed in:\n\t" + FactorsAccountant.GetFilenames(factor) + "\n\n";
                }
        }
    }

    return result;
}

TString TFactorsUsageTester::ShowFactorUsage(const TString& sliceName, size_t factor) {
    TString result;

    NFactorSlices::TFullFactorIndex curFactor(FromString<NFactorSlices::EFactorSlice>(sliceName), factor);

    result += FactorsSet.GetDescription() + ' ' + sliceName + ':' + ToString<size_t>(factor) +
                '\t' + FactorsSet.GetFactorName(curFactor) +
                "\nUsed in:\n\t" + FactorsAccountant.GetFilenames(curFactor) + "\n\n";

    return result;
}
