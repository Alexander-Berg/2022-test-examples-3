#include <search/meta/rank/pers/profilestorage.h>
#include <search/web/personalization/testlib/env.h>

#include <ysite/yandex/pers/userdata/io/userprofile/io.h>
#include <ysite/yandex/pers/userdata/io/rtuserdata/rtuserdata.h>
#include <ysite/yandex/pers/userdata/types/all/userdata.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/scheme/scheme.h>

#include <util/datetime/cputimer.h>
#include <util/generic/algorithm.h>
#include <util/stream/input.h>
#include <util/stream/format.h>
#include <util/string/split.h>

using namespace NPers;
using namespace NPers::NRTUserData;
using namespace NUserProfiles;

static const TString TAB = "\t";

class TProfiler {
private:
    TSimpleTimer Timer;

    TString ProfilingInfo;
    TStringOutput ProfilingOut;

public:
    TProfiler() : ProfilingOut(ProfilingInfo) {}

public:
    void Start() {
        Timer.Reset();
    }

    void Clear() {
        ProfilingOut.Flush();
        ProfilingInfo.clear();
    }

    void Stop(const TString& what) {
        const TDuration d = Timer.Get();
        ProfilingOut << " " << what << "~" << ToString(d.MicroSeconds());
    }

    const TString& GetInfo() {
        ProfilingOut.Flush();
        return ProfilingInfo;
    }
};

void RerankAndPrintRPStats(TRerankingContext& ctx, ui32 lineIdx, TProfiler& p, bool printTimings) {
    const ui32 nDocsToRearrange = ctx.Grouping.Size();
    if (nDocsToRearrange < 1 || nDocsToRearrange > 2 * NPers::MAX_DOCS_TO_REARRANGE) {
        return;
    }

    ctx.Calc.NewSerp();
    ctx.NewOrder.reserve(nDocsToRearrange);
    for (ui32 i = 0; i < nDocsToRearrange; i++) {
        TMetaGroup& group = ctx.Grouping.GetMetaGroup(i);
        ctx.OldPositions[&group] = i;
        ctx.NewOrder.push_back(TRelevMG(group, 0));
    }

    ui32 cnt = Min((ui32) ctx.NewOrder.size(), nDocsToRearrange);
    TVector<TPersonalizationFeatures> features(cnt);

    p.Start();
    FillFeatures(ctx, features);
    p.Stop("fillfeatures");

    for (ui32 i = 0; i < cnt; i++) {
        TPersonalizationFeatures& feat = features[i];
        Cout << lineIdx << TAB << i << Endl;
        for (ui32 f = 0; f < PF_LAST; f++) {
            Cout << TAB
                << f << ":" << FEATURE_METADATA[f].Name << TAB
                << Prec(feat[f], PREC_POINT_DIGITS, 4) << Endl;
        }
        Cout << Endl;
    }

    if (printTimings) {
        Cout << lineIdx << TAB << "times" << p.GetInfo() << Endl;
    }
}

template<class T>
TAutoPtr<T> FromJson(const NSc::TValue& data);

template<>
TAutoPtr<TUserProfile> FromJson(const NSc::TValue& data) {
    TAutoPtr<TUserProfile> profile = new TUserProfile;
    GetSchemeConverter<TUserProfile>()->ConvertFrom(data, &*profile);
    return profile;
}

template<>
TAutoPtr<TRTUserData> FromJson(const NSc::TValue& data) {
    return NPers::NRTUserData::FromJson<TRTUserData>(data);
}

template<class TUserDataType>
TAutoPtr<TUserDataType> LoadDataFromJson(const NSc::TValue& data, const TString& key, TProfiler& p) {
    TAutoPtr<TUserDataType> ptr;
    if (data.Has(key)) {
        try {
            p.Start();
            ptr = ::FromJson<TUserDataType>(data[key]);
            p.Stop(key);
        } catch (yexception& e) {
            Cerr << "Can't parse user profile: " << e.what() << Endl;
            throw;
        }
    }
    return ptr;
}

int main(int argc, const char* argv[]) {
    TString clusterProfilesTrie;
    TString rc;
    TString uc;

    NLastGetopt::TOpts opts;
    opts
        .AddLongOption("cp", "cluster profiles trie")
        .Optional()
        .DefaultValue("")
        .StoreResult(&clusterProfilesTrie);
    opts
        .AddCharOption('t', "print timings")
        .Optional()
        .NoArgument();
    opts
        .AddLongOption("rc", "rearrange context dump")
        .Required()
        .StoreResult(&rc);
    opts
        .AddLongOption("uc", "user context dump")
        .Required()
        .StoreResult(&uc);
    opts.AddHelpOption();
    NLastGetopt::TOptsParseResult optsRes(&opts, argc, argv);

    TEnv env;
    bool printTimings = optsRes.Has('t');

    TFormulaCalculator formulaCalc;

    TFormula formula("");
    TPersonalizationOptions persOpts(formula);

    THolder<TIFStream> rearrCtx(new TIFStream(rc));
    THolder<TIFStream> userCtx (new TIFStream(uc));
    TAutoPtr<TProfileStorage> clusterProfiles;
    if (!clusterProfilesTrie.empty()) {
        clusterProfiles = new TProfileStorage(clusterProfilesTrie);
    }

    TWsCache cache;

    TProfiler prof;
    ui32 lineIdx = 0;
    TString rcLine, ucLine;
    while (rearrCtx->ReadLine(rcLine) && userCtx->ReadLine(ucLine)) {
        const NSc::TValue sdata = NSc::TValue::FromJson(rcLine);
        const NSc::TValue udata = NSc::TValue::FromJson(ucLine);

        TSearchData searchData(sdata);
        TAutoPtr<ICtx> ctx = env.CreateContext(searchData);

        prof.Clear();

        TAutoPtr<TUserProfile> weeklyProfile = LoadDataFromJson<TUserProfile>(udata, WEEKLY_PROFILE_ALIAS, prof);
        TAutoPtr<TUserProfile> longProfile   = LoadDataFromJson<TUserProfile>(udata, LONG_PROFILE_ALIAS, prof);
        TAutoPtr<TUserProfile> loginProfile  = LoadDataFromJson<TUserProfile>(udata, LOGIN_PROFILE_ALIAS, prof);
        TAutoPtr<TUserProfile> spylogProfile = LoadDataFromJson<TUserProfile>(udata, SPYLOG_PROFILE_ALIAS, prof);
        TAutoPtr<TUserProfile> rtProfile     = LoadDataFromJson<TUserProfile>(udata, REALTIME_PROFILE_ALIAS, prof);
        TAutoPtr<TRTUserData>  rtUserData    = LoadDataFromJson<TRTUserData>(udata, REALTIME_USER_DATA_ALIAS, prof);
        TAutoPtr<TRTUserData>  rtSpylogData  = LoadDataFromJson<TRTUserData>(udata, REALTIME_SPYLOG_DATA_ALIAS, prof);
        if (clusterProfiles && weeklyProfile) {
            weeklyProfile->ClusterProfile = clusterProfiles->GetProfile(ToString(weeklyProfile->ClusteringStats.GetClusterId()));
        }

        const TUserData userData(
            weeklyProfile.Get(),
            longProfile.Get(),
            loginProfile.Get(),
            rtUserData.Get(),
            spylogProfile.Get(),
            rtSpylogData.Get(),
            rtProfile.Get()
        );
        if (userData.IsEmpty()) {
            Cerr << "Warning: empty userdata" << Endl;
        }

        auto featCalc = ctx->CreateFeatureCalculator(cache);

        TRerankingContext rerankCtx(
            ctx->GetRearrangeParams(),
            ctx->GetGrouping(),
            userData,
            persOpts,
            *featCalc,
            formulaCalc,
            nullptr,
            false,
            false
        );

        try {
            RerankAndPrintRPStats(rerankCtx, lineIdx, prof, printTimings);
        } catch (const yexception& e) {
            Cerr << "ERROR at line " << lineIdx << ": " << e.what() << Endl;
            Cerr << "USER CTX:  " << ucLine << Endl;
            Cerr << "REARR CTX: " << rcLine << Endl;
            throw;
        }
        lineIdx++;
    }
    return 0;
}
