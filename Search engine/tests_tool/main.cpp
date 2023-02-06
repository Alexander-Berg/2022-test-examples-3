#include <search/formula_chooser/mappings_io.h>
#include <search/formula_chooser/ruleset_applier.h>
#include <search/formula_chooser/context_filler/context_filler.h>
#include <search/formula_chooser/archive_checker/archive_checker.h>

#include <search/reqparam/treat_cgi_request.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>
#include <library/cpp/protobuf/util/pb_io.h>
#include <util/folder/path.h>
#include <util/stream/file.h>
#include <util/generic/serialized_enum.h>
#include <util/generic/xrange.h>

using EF=NFormulaChooser::NProto::EInputFlag;
static const TVector<TVector<EF>> DimensionsDivisor {
    // elements in one list can't be enabled together (if it's not true create new dimension)
    // some flags doesn't used by no any active ruleset
    //   so - it is much more useful to have them disabled (to speed up the test)

    {
        // EF::F_IS_RELEV_LOCALE_WORLD, EF::F_IS_RELEV_LOCALE_INDONESIA,
        EF::F_IS_RELEV_LOCALE_TURKEY, EF::F_IS_RELEV_LOCALE_EU
    },
    {
        // EF::F_IS_RELEV_LOCALE_POLAND, EF::F_IS_RELEV_LOCALE_FINLAND,
        EF::F_IS_RELEV_LOCALE_ESTONIYA, EF::F_IS_RELEV_LOCALE_LITHUANIA,
        EF::F_IS_RELEV_LOCALE_LATVIA
    },
    {EF::F_IS_ALICE, EF::F_IS_ARABIC_ALICE},
    {EF::F_IS_COMPUTER_SCIENCE},
    {EF::F_IS_SEO_QUERY},
    {EF::F_HAS_CONSTRAINTS},
    // {EF::F_IS_EXPERT_PRO_GOOGLE},
    // {EF::F_IS_LATINISTIC},

    // {EF::F_IS_EXUSSR_LIST_REGION, EF::F_IS_INDONESIA_REGION, EF::F_IS_ISRAEL_REGION},
    // {EF::F_IS_TURKEY_TLD, EF::F_IS_COM_TLD},
    // {EF::F_IS_FRESH_DETECTED},
    // {EF::F_IS_PORNO},
    // {EF::F_IS_TOUCH_UI},
    // {EF::F_IS_PAY_DETECTED},
};

class TFLagsBrutForcer {
    static constexpr size_t MaxAllowedCases = 1000 * 1000;

    TVector<ui32> DimensionPointers;
    TVector<EF> CurrentState;
    bool Finished = false;

    void SetAsPointers() {
        CurrentState.clear();
        for(size_t d : xrange(DimensionsDivisor.size())) {
            ui32 p = DimensionPointers[d];
            if (p < DimensionsDivisor[d].size()) {
                CurrentState.push_back(DimensionsDivisor[d][p]);
            }
        }
    }

    void DimensionsIteration() {
        size_t iteratedLevel = DimensionsDivisor.size();
        for(size_t d : xrange(DimensionsDivisor.size())) {
            ui32& p = DimensionPointers[d];
            if (p < DimensionsDivisor[d].size()) {
                p += 1;
                iteratedLevel = d;
                break;
            }
        }

        Finished = (iteratedLevel == DimensionsDivisor.size());

        for(size_t d : xrange(iteratedLevel)) {
            DimensionPointers[d] = 0;
        }
    }
public:
    TFLagsBrutForcer() {
        size_t casesNum = 1;
        for(auto& d : DimensionsDivisor) {
            casesNum *= (d.size() + 1);
        }
        Y_ENSURE(casesNum <= MaxAllowedCases);
        DimensionPointers.resize(DimensionsDivisor.size());
        SetAsPointers();
    }

    bool IsFinished() const {
        return Finished;
    }

    const TVector<EF>& Current() const {
        return CurrentState;
    }

    void Next() {
        DimensionsIteration();
        if (!Finished) {
            SetAsPointers();
        }
    }
};

int main_print_all_results(int argc, const char* argv[]) {
    TMaybe<TString> ruleSet;
    bool useLastMapping = false;

    {
        NLastGetopt::TOpts opts = NLastGetopt::TOpts::Default();
        opts
            .AddLongOption('r', "rule-set", "rule set to use")
            .RequiredArgument("NAME")
            .Optional()
            .StoreResultT<TString>(&ruleSet);

        opts
            .AddLongOption("last-mapping", "use mapping with max-id")
            .NoArgument()
            .Optional()
            .SetFlag(&useLastMapping);

        NLastGetopt::TOptsParseResult {&opts, argc, argv};
    }

    NFormulaChooser::TComputationContext context;
    context.MappingsAndRuleSetsPtr = NFormulaChooser::GetDefaultMappingsPtr();
    context.RuleSetToUseRewrite = ruleSet;
    context.GlobalFallback.ConstructInPlace();

    ui32 maxId = std::accumulate(
        context.MappingsAndRuleSetsPtr->GetMapping().begin(),
        context.MappingsAndRuleSetsPtr->GetMapping().end(),
        0,
        [](ui32 cur, auto& x) -> ui32 {
            return Max(cur, x.GetId());
        }
    );
    if (useLastMapping) {
        context.MappingsToUseRewrite = maxId;
    }

    for(TFLagsBrutForcer bf; !bf.IsFinished(); bf.Next()) {
        const TVector<EF>& flagsInTrue = bf.Current();
        TStringStream lineResult;

        try {
            for(auto f : GetEnumAllValues<NFormulaChooser::NProto::EInputFlag>()) {
                context.FlagsContext[f] = false;
            }
            for(auto f : flagsInTrue) {
                Y_ENSURE(context.FlagsContext[f] == false, "reset of " << f);
                context.FlagsContext[f] = true;
                lineResult << f << "=1;";
            }
            if (flagsInTrue.empty()) {
                lineResult << "AllFlags=0";
            }

            NFormulaChooser::TComputationResult cr = DoComputeResult(context);
            Y_ENSURE(!cr.UsedFallBack, "fallback: " << cr.ExecutionError);

            lineResult << "\n|---->";
            Y_ENSURE(cr.ResultVars.GetL2Formula().VarSize() == 1);
            Y_ENSURE(cr.ResultVars.GetL3Formula().VarSize() == 1);
            Y_ENSURE(cr.ResultVars.GetWebTierL1Formula().VarSize() == 1);
            lineResult << "\n L2FormulaVar=" << cr.ResultVars.GetL2Formula().GetVar(0);
            lineResult << "\n L3FormulaVar=" << cr.ResultVars.GetL3Formula().GetVar(0);
            lineResult << "\n WebTierL1FormulaVar=" << cr.ResultVars.GetWebTierL1Formula().GetVar(0);

            if (!cr.ResultVars.GetL3BoostGraph().GetVar().empty()) {
                lineResult << "\n L3BoostGraphVar=" << cr.ResultVars.GetL3BoostGraph().GetVar(0);
            }
            if (!cr.ResultVars.GetRearrsConfGraph().GetVar().empty()) {
                lineResult << "\n RearrsConfGraphVar=" << cr.ResultVars.GetRearrsConfGraph().GetVar(0);
            }
            if (!cr.ResultVars.GetPersFormula().GetVar().empty()) {
                lineResult << "\n PersFormulaVar=" << cr.ResultVars.GetPersFormula().GetVar(0);
            }

            TMap<TString, NFormulaChooser::NProto::TVarValue> sortedCopy(
                cr.ResultVars.GetDynamic().begin(),
                cr.ResultVars.GetDynamic().end()
            );
            for(const auto& [k,v] : sortedCopy) {
                Y_ENSURE(v.VarSize() == 1);
                lineResult << "\n " << k << "=" << v.GetVar(0);
            }

        } catch(yexception& e) {
            Cerr << " error at point: " << lineResult.Str() << Endl;
            Cerr << "exception: " << e.what() << Endl;
            throw;
        }

        Cout << '"' << lineResult.Str() << '"'  << Endl;
    }

    return 0;
}


int main_context_fill(int argc, const char* argv[]) {
    TString input;

    {
        NLastGetopt::TOpts opts = NLastGetopt::TOpts::Default();
        opts
            .AddLongOption('i', "input", "mmeta cgi-s file")
            .RequiredArgument("FILE")
            .Optional()
            .StoreResult(&input);

        NLastGetopt::TOptsParseResult {&opts, argc, argv};
    }

    IInputStream* in = &Cin;
    THolder<IInputStream> fin;
    if (input) {
        fin.Reset(new TFileInput(input));
        in = fin.Get();
    }
    IOutputStream& out = Cout;

    TString line;
    THashMap<NFormulaChooser::NProto::EInputFlag, size_t, NFormulaChooser::TIntHashFwd> stats;
    for(size_t lineId = 0; in->ReadLine(line); lineId += 1) {
        TCgiParameters cgiParam(line);
        TRequestParams rp;
        NCgiRequest::TreatCgiParams(rp, cgiParam);
        auto flags = NFormulaChooser::FillContextFlagsFromRP(rp);
        TVector<TString> parts;
        for(auto [k, v] : flags) {
            if (v) {
                stats[k] += 1;
                parts.push_back(ToString(k));
            }
        }
        Sort(parts);
        out << lineId << ": " << JoinSeq(";", parts) << "\n";
    }

    for(auto k : GetEnumAllValues<NFormulaChooser::NProto::EInputFlag>()) {
        if (!NFormulaChooser::NProto::EInputFlag_IsValid(k)) {
            continue;
        }
        out << "Stats: " << k << ": "<< stats[k] << Endl;
        if (stats[k] == 0) {
            out << "WARNING: " << k << Endl;
        }
    }

    return 0;
}

int main(int argc, const char* argv[]) {

    TModChooser mc;
    mc.AddMode("print-all-results", main_print_all_results, "make computation for brut-forced flags values");
    mc.AddMode("context-fill-emulation", main_context_fill, "calc flags by given mmeta-search cgi requests");

    return mc.Run(argc, argv);
}
