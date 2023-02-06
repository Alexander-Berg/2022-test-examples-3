#include <search/web/rearr_formulas_bundle/content_io_lib/bundle_content.h>
#include <search/web/rearr_formulas_bundle/content_io_lib/download_formula.h>
#include <search/formula_chooser/archive_checker/archive_checker.h>

#include <kernel/matrixnet/mn_file.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/folder/tempdir.h>
#include <util/system/fstat.h>

struct TEnvPreparer {
    THolder<TTempDir> TmpDir;
    THashMap<TString, TString> FmlIdToFilePath;
    NRearrFormulasBundle::NProto::TFullContent ContentInfo;
    NRearrFormulasBundle::NProto::TExportData ExportData;

    void Init() {
        if (TmpDir) {
            return;
        }

        TmpDir.Reset(new TTempDir("rearr_formulas_bundle"));
        Cerr << "created dir at " << TmpDir->Name() << Endl;

        ContentInfo = NRearrFormulasBundle::GetBundleContent();
        ExportData = NRearrFormulasBundle::BuildExportData(ContentInfo);

        TVector<TString> allIds;
        for(auto f : ExportData.GetFmlIdToFormulaInfo()) {
            allIds.push_back(f.first);
        }
        Cerr << "starting downloading from fml" << Endl;
        FmlIdToFilePath = NRearrFormulasBundle::DownloadMatrixnetInfosIntoDir(allIds, TmpDir->Name());
        Cerr << "downloaded " << FmlIdToFilePath.size() << " formulas " << Endl;

        Y_IF_DEBUG(
            TmpDir->DoNotRemove();
        );
    }
};
static THolder<TEnvPreparer> Env;

struct TStartAndStop : public TTestBase {
    void AtStart() {
        Env.Reset(new TEnvPreparer);
        Env->Init();
    }

    void AtEnd() {
        Env = nullptr;
    }
};

Y_UNIT_TEST_SUITE_IMPL(DownloadBundleAndCheck, TStartAndStop) {
    Y_UNIT_TEST(TestSizes) {
        Env->Init();

        THashMap<TString, ui64> fmlIdToSize;
        for(auto [id, path] : Env->FmlIdToFilePath) {
            fmlIdToSize[id] = GetFileLength(path);
        }

        auto checkFormula = [&](auto& f) {
            float mb = 1024 * 1024;
            float bytes = f.GetWeightMb() * mb;
            float factual = fmlIdToSize.at(f.GetFmlId());
            Y_ENSURE(
                std::abs(bytes - factual) / factual < 0.05,
                "weight in meta-data is different from actual size for fml-id " << f.GetFmlId()
                << ": got " << (factual / mb) << "mb while expected "
                << f.GetWeightMb() << "mb"
            );
        };

        for(auto& r : Env->ContentInfo.GetRearrFormulas()) {
            for(auto& e : r.GetExperimentPack()) {
                for(auto& f : e.GetFormula()) {
                    checkFormula(f);
                }
            }
            for(auto& f : r.GetProdFormula()) {
                checkFormula(f);
            }
        }
    }

    Y_UNIT_TEST(TestIterations) {
        THashMap<TString, ui64> fmlIdToIterations;
        for(auto [id, path] : Env->FmlIdToFilePath) {
            NMatrixnet::TMnSseFile file(path.data());
            fmlIdToIterations[id] = file.NumTrees();
        }

        auto checkFormula = [&](auto& f) {
            float iters = f.GetIterationsCount();
            float factual = fmlIdToIterations.at(f.GetFmlId());
            Y_ENSURE(
                std::abs(iters - factual) / factual < 0.05,
                "iterations in meta-data is different from actual iterations-count for fml-id " << f.GetFmlId()
                << ": got " << factual << " iters while expected "
                << iters << " iters"
            );
        };

        for(auto& r : Env->ContentInfo.GetRearrFormulas()) {
            for(auto& e : r.GetExperimentPack()) {
                for(auto& f : e.GetFormula()) {
                    checkFormula(f);
                }
            }
            for(auto& f : r.GetProdFormula()) {
                checkFormula(f);
            }
        }
    }

    Y_UNIT_TEST(HaveAllRequiredL3ProdFormulas) {
        TVector<TString> paths;
        TFsPath(Env->TmpDir->Name()).ListNames(paths);
        bool result = NFormulaChooser::CheckFormulasExistence(
            NFormulaChooser::GetRequiredProdFormulasIdsFromCompiledResource(NRearrFormulasBundle::NProto::EArchiveType::Middle),
            NFormulaChooser::GetAllFormulasIds(paths),
            Cerr
        );
        UNIT_ASSERT(result);
    }

    Y_UNIT_TEST(HaveAllRequiredL3ExpFormulas) {
        TVector<TString> paths;
        TFsPath(Env->TmpDir->Name()).ListNames(paths);
        bool result = NFormulaChooser::CheckFormulasExistence(
            NFormulaChooser::GetRequiredExpFormulasIds(NRearrFormulasBundle::NProto::EArchiveType::Middle),
            NFormulaChooser::GetAllFormulasIds(paths),
            Cerr
        );
        UNIT_ASSERT(result);
    }
}
