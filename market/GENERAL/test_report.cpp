#include <market/kombat/preset/presets.h>
#include <market/kombat/handler/test_report.h>
#include <util/generic/string.h>

namespace {
    using namespace NMarket::NKombat;
    struct TAdaptiveRepeatSettings {
        ui32 MaxRepeat = 0;
        TMaybe<ui32> AbsoluteTimeEpsilon;
        TMaybe<double> RelativeTimeEpsilon;
        ui32 AnonMemEpsilon = 0;
        ui32 SnippetAnonMemEpsilon;
        float Quantile = 0;
        ui32 AttacksInEpsilon = 0;
    };

    TAdaptiveRepeatSpec ToProto(const TAdaptiveRepeatSettings& settings) {
        TAdaptiveRepeatSpec spec;
        spec.SetMaxRepeat(settings.MaxRepeat);
        if (settings.AbsoluteTimeEpsilon) {
            spec.SetAbsoluteTimeEpsilon(*settings.AbsoluteTimeEpsilon);
        }
        if (settings.RelativeTimeEpsilon) {
            spec.SetRelativeTimeEpsilon(*settings.RelativeTimeEpsilon);
        }
        spec.SetAnonMemEpsilon(settings.AnonMemEpsilon);
        spec.SetSnippetAnonMemEpsilon(settings.SnippetAnonMemEpsilon);
        spec.SetQuantile(settings.Quantile);
        spec.SetAttacksInEpsilon(settings.AttacksInEpsilon);

        return spec;
    }

    THashMap<TBattleSpec::EType, TAdaptiveRepeatSettings> RepeatSettings = {
        {TBattleSpec::MAIN, {5, 5, {}, 200 * 1024, 200 * 1024, 99, 1}},
        {TBattleSpec::API, {5, 5, {}, 200 * 1024, 200 * 1024, 99, 1}},
        {TBattleSpec::PARALLEL, {5, 3, {}, 200 * 1024, 200 * 1024, 99, 1}},
        {TBattleSpec::INT, {5, {}, 5.0, 200 * 1024, 200 * 1024, 99, 1}},
    };

    constexpr i64 REPORT_RELEASE_CHAT = -1001085967802;

    void SetAttackProfile(const TAttackSettings& attackSetings, TFireProfile& fireProfile) {
        if (const auto rpsProfile = std::get_if<TRpsLoadProfile>(&attackSetings.Profile)) {
            fireProfile.SetRps(rpsProfile->Rps);
            fireProfile.SetSeconds(rpsProfile->Duration);
            return;
        }

        const auto& oneThreadProfile = std::get<TOneThreadLoadProfile>(attackSetings.Profile);
        fireProfile.SetStreamCount(1);
        fireProfile.SetRequestCount(oneThreadProfile.RequestCount);
    }

    void SetupRepeatSettings(TBattleSpec::EType fireType, bool oneThread, TAttackSpec& attack, const IBattle::TSpec& battleSpec) {
        if (oneThread) {
            attack.SetRepeat(battleSpec.GetOnlyScenarios() ? 2 : 3);
        } else {
            attack.MutableAdaptiveRepeat()->CopyFrom(ToProto(RepeatSettings[fireType]));
        }
    }

    void SetupAmmo(const TMaybe<TString>& ammoFilter, TAmmoSpec& ammoSpec) {
        ammoSpec.AddTag(TAmmoSpec::LAST);
        if (ammoFilter) {
            ammoSpec.SetFilter(*ammoFilter);
        }
    }
}

void DescribeEnum(NMarket::NShiny::TCgiEnum<TBattleSpec::EType>::TList& values) {
    values = {
        {"main", "", TBattleSpec::MAIN},
        {"api", "", TBattleSpec::API},
        {"parallel", "", TBattleSpec::PARALLEL},
        {"int", "", TBattleSpec::INT},
    };
}

TString AsReportName(const TBattleSpec::EType& fireType) {
    static THashMap<TBattleSpec::EType, TString> types = {
        {TBattleSpec::MAIN, "main"},
        {TBattleSpec::API, "api"},
        {TBattleSpec::PARALLEL, "parallel"},
        {TBattleSpec::INT, "int"},
    };
    if (auto it = types.find(fireType); it != types.end()) {
        return it->second;
    }
    return "UNKNOWN";
}

TMaybe<TString> ResolveBaseVersion(const TMaybe<TString>& baseVersion, IReportStorage& reports) {
    if (baseVersion) {
        return *baseVersion;
    }
    try {
        IReportStorage::TListContext ctx;
        ctx.Limit = 100;
        ctx.Prod = true;
        const auto prodReports = reports.ListReports(ctx);
        Y_ENSURE(!prodReports.empty(), "No prod reports are found");
        return prodReports.front()->GetMetadata().Version;
    } catch (const yexception& exception) {
        Log().Error() << "Cannot resolve report version in test_report handler due to: " << exception.what();
    }
    return Nothing();
}

TString GetBattleDescription(const TBattleSpec::EType& fireType, const TMaybe<TString>& baseVersion, const TString& version) {
    TStringBuilder result;
    result << "Репорт " << AsReportName(fireType) << ": ";
    result << (baseVersion ? *baseVersion : "UNKNOWN") << " vs " << version;
    return std::move(result);
}

void TTestReport::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    args.Required("version", "new report version", &TRequest::Version);
    args.Required("ticket", "startrek ticket", &TRequest::Ticket);
    args.Optional("fire_type", "fire type", &TRequest::FireType, TBattleSpec::MAIN);
    args.Optional("base_version", "base report version", &TRequest::BaseVersion);
    args.Optional("use_stable_base_report", "use stable base report binary", &TRequest::UseStableBaseReport, false);
    args.Optional("use_stable_meta_report", "use stable meta report binary", &TRequest::UseStableMetaReport, false);
    args.Optional("max_rps", "perform max rps test", &TRequest::MaxRps, false);
    args.Optional("auto_repeat_limit", "number of additional auto repeats", &TRequest::AutoRepeatLimit);
    args.Optional("all_revisions", "attack all revisions between given versions", &TRequest::AllRevisions, false);
    args.Optional("owner", "battle owner", &TRequest::Owner, "tsum"); /// @todo rm default after tsum change
    args.Optional("ammo-filter", "filter applied to ammo", &TRequest::AmmoFilter);
    args.Optional("telegram-group", "telegram group to notify", &TRequest::TelegramGroup);
    args.Optional("only-scenarios", "use only main scenarios in ammo in equal proportions", &TRequest::OnlyScenarios);
}

TStringBuf TTestReport::Describe() {
    return "test report performance";
}

TTestReport::TResponse TTestReport::Run(const TRequest& request) const {
    IBattle::TSpec spec;
    static constexpr bool oneShard = true;
    const bool oneThread = request.FireType == TBattleSpec_EType_MAIN || request.FireType == TBattleSpec_EType_API;

    spec.SetOwner(request.Owner);
    spec.SetFireType(request.FireType);
    spec.SetPriority(100);
    if (request.FireType == TBattleSpec_EType_MAIN && !request.MaxRps && !request.AllRevisions) {
        spec.SetOnlyScenarios(request.OnlyScenarios);
    }
    if (request.UseStableBaseReport || request.UseStableMetaReport) {
        spec.SetMetaReport(true);
    }

    auto setStableVersion = [this, &request](TAttackSpec& attack) {
        if (request.UseStableBaseReport) {
            if (const auto version = ResolveBaseVersion(Nothing(), BaseReports)) {
                attack.MutableTarget()->MutableReport()->SetVersion(*version);
            }
        } else if (request.UseStableMetaReport) {
            if (const auto version = ResolveBaseVersion(Nothing(), MetaReports)) {
                attack.MutableTarget()->MutableMetaReport()->SetVersion(*version);
            }
        }
    };

    auto mutableReport = [&request](TAttackSpec& attack) {
        if (request.UseStableBaseReport) {
            return attack.MutableTarget()->MutableMetaReport();
        }
        return attack.MutableTarget()->MutableReport();
    };

    auto attackSettings = GetDefaultAttackSettings(request.FireType, oneShard, oneThread, spec.GetOnlyScenarios(), request.AmmoFilter.GetOrElse(""));

    auto& baseAttack = *spec.AddAttack();
    const auto baseVersion = ResolveBaseVersion(request.BaseVersion, request.UseStableBaseReport ? MetaReports : BaseReports);
    if (request.AllRevisions) {
        TRevisionRange& range = *mutableReport(baseAttack)->MutableRevisionRange();
        range.MutableFrom()->SetVersion(*baseVersion);
        range.MutableTo()->SetVersion(request.Version);
    } else if (!baseVersion) {
        mutableReport(baseAttack)->AddTag(TReportSpec::LAST);
        mutableReport(baseAttack)->AddTag(TReportSpec::PROD);
    } else {
        mutableReport(baseAttack)->SetVersion(*baseVersion);
    }
    baseAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::FULL);
    baseAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::PROD);
    baseAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::PUBLISHED);
    baseAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::DAYLY);
    SetupAmmo(request.AmmoFilter, *baseAttack.MutableFire()->MutableAmmo());
    SetAttackProfile(attackSettings, *baseAttack.MutableFire()->MutableProfile());
    SetupRepeatSettings(request.FireType, oneThread, baseAttack, spec);
    setStableVersion(baseAttack);

    if (!request.AllRevisions) {
        auto& newAttack = *spec.AddAttack();
        mutableReport(newAttack)->SetVersion(request.Version);
        newAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::FULL);
        newAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::PROD);
        newAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::PUBLISHED);
        newAttack.MutableTarget()->MutableIndex()->AddTag(TIndexSpec::DAYLY);
        SetupAmmo(request.AmmoFilter, *newAttack.MutableFire()->MutableAmmo());
        SetAttackProfile(attackSettings, *newAttack.MutableFire()->MutableProfile());
        SetupRepeatSettings(request.FireType, oneThread, newAttack, spec);
        setStableVersion(newAttack);
    }

    TResultCollectorSpec* collectorSpec = spec.MutableResult()->MutableCollector();
    SetQuantilesPreset(*collectorSpec);
    if (request.MaxRps) {
        collectorSpec->SetMaxRps(true);
    }

    if (request.AllRevisions) {
        spec.SetIgnoreFailedBuilds(true);
    }

    auto& transform = *spec.MutableResult()->MutableTransformer();
    transform.MutableDegradation()->SetQuantile(attackSettings.Quantile);
    SetDegradationPreset(request.FireType, oneShard, spec.GetOnlyScenarios(), *transform.MutableDegradation());
    if (request.AutoRepeatLimit) {
        transform.MutableDegradation()->SetAutoRepeatLimit(*request.AutoRepeatLimit);
    }
    transform.SetSource(true);

    auto& publish = *spec.MutableResult()->MutablePublisher();
    publish.MutableStartrek()->SetTicket(request.Ticket);
    publish.SetReShootMode(EReShootMode::RELEASE);
    auto& chat = *publish.MutableTelegram()->AddChat();
    if (request.TelegramGroup) {
        chat.SetGroupName(*request.TelegramGroup);
    } else {
        chat.SetId(REPORT_RELEASE_CHAT);
    }

    publish.MutableTelegram()->SetNotifySuccess(false);

    spec.SetOneShardMode(oneShard);
    spec.SetForceSaasKVSnippets(true);

    auto id = Executor.Execute(spec);
    FireCount.Push(1);

    return {id};
}
