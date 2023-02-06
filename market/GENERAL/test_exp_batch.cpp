#include <market/kombat/handler/test_exp_batch.h>
#include <market/kombat/preset/presets.h>

using namespace NMarket::NKombat;

namespace {
    const TVector<TString> DEFAULT_TELEGRAM_GROUPS = {"market_kombat"};

    void SetupReport(TReportSpec& report) {
        report.AddTag(TReportSpec::LAST);
        report.AddTag(TReportSpec::PROD);
    }

    void SetupTarget(TTargetSpec& target) {
        SetupReport(*target.MutableReport());
        SetupReport(*target.MutableMetaReport());

        target.MutableIndex()->AddTag(TIndexSpec::FULL);
        target.MutableIndex()->AddTag(TIndexSpec::PROD);
        target.MutableIndex()->AddTag(TIndexSpec::PUBLISHED);
        target.MutableIndex()->AddTag(TIndexSpec::DAYLY);
    }

    void SetupAmmo(const TMaybe<TString>& ammoFilter, const TVector<TString>& rearrFactors, TAmmoSpec& ammoSpec) {
        ammoSpec.AddTag(TAmmoSpec::LAST);
        if (ammoFilter) {
            ammoSpec.SetFilter(*ammoFilter);
        }
        for (const auto& rearr : rearrFactors) {
            ammoSpec.AddRearrFlag(rearr);
        }
    }

    void SetupAttackProfile(const TAttackSettings& attackSetings, TFireProfile& fireProfile) {
        if (const auto rpsProfile = std::get_if<TRpsLoadProfile>(&attackSetings.Profile)) {
            fireProfile.SetRps(rpsProfile->Rps);
            fireProfile.SetSeconds(rpsProfile->Duration);
            return;
        }

        const auto& oneThreadProfile = std::get<TOneThreadLoadProfile>(attackSetings.Profile);
        fireProfile.SetStreamCount(1);
        fireProfile.SetRequestCount(oneThreadProfile.RequestCount);
    }

    void AddTelegramGroup(const TString& groupName, TTelegramSpec& telegramSpec) {
        auto& chat = *telegramSpec.AddChat();
        chat.SetGroupName(groupName);
    }

    void SetupTelegram(const TMaybe<TString>& telegramGroup, TTelegramSpec& telegramSpec) {
        telegramSpec.SetNotifyOnlyFinalDegradation(true);
        if (telegramGroup) {
            AddTelegramGroup(*telegramGroup, telegramSpec);
            return;
        }
        for (const auto& group : DEFAULT_TELEGRAM_GROUPS) {
            AddTelegramGroup(group, telegramSpec);
        }
    }
}

void TTestExpBatch::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    args.Required("ticket", "startrek ticket", &TRequest::Ticket);
    args.Required("owner", "battle owner", &TRequest::Owner);
    args.Required("experiment-ticket", "experiment ticket", &TRequest::ExperimentTicket);
    args.Optional("telegram-group-to-notify", "telegram group to send notification in it", &TRequest::TelegramGroupToNotify);
    args.Repeated("rearr-factor", "rearr-factors in format my_factor=my_value", &TRequest::RearrFactors, true);
}

TStringBuf TTestExpBatch::Describe() {
    return "test experiment rearr factors";
}

TTestExpBatch::TResponse TTestExpBatch::Run(const TRequest& request) const {
    return {{
        RunDegrad(request, TBattleSpec_EType_MAIN, false),
        RunDegrad(request, TBattleSpec_EType_MAIN, false, "prime")
    }};
}

TBattleId TTestExpBatch::RunDegrad(const TRequest& request, TBattleSpec::EType reportType, bool maxRps, const TMaybe<TString>& ammoFilter) const {
    static constexpr ui32 attackRepeats = 3;
    static constexpr ui32 autoRepeatLimit = 2;

    IBattle::TSpec spec;
    const bool oneThread = EqualToOneOf(reportType, TBattleSpec_EType_MAIN, TBattleSpec_EType_API);
    spec.SetOwner(request.Owner);
    spec.SetFireType(reportType);
    spec.SetPriority(5);
    spec.SetOneShardMode(true);

    auto attackSettings = GetDefaultAttackSettings(reportType, true, oneThread, false, ammoFilter.GetOrElse(""));

    auto& baseAttack = *spec.AddAttack();
    SetupTarget(*baseAttack.MutableTarget());
    SetupAmmo(ammoFilter, {}, *baseAttack.MutableFire()->MutableAmmo());
    SetupAttackProfile(attackSettings, *baseAttack.MutableFire()->MutableProfile());
    baseAttack.SetRepeat(attackRepeats);

    auto& testAttack = *spec.AddAttack();
    SetupTarget(*testAttack.MutableTarget());
    SetupAmmo(ammoFilter, request.RearrFactors, *testAttack.MutableFire()->MutableAmmo());
    SetupAttackProfile(attackSettings, *testAttack.MutableFire()->MutableProfile());
    testAttack.SetRepeat(attackRepeats);

    auto& collectorSpec = *spec.MutableResult()->MutableCollector();
    SetQuantilesPreset(collectorSpec);
    if (maxRps) {
        collectorSpec.SetMaxRps(true);
    }

    auto& transform = *spec.MutableResult()->MutableTransformer();
    transform.MutableDegradation()->SetQuantile(attackSettings.Quantile);
    SetDegradationPreset(reportType, true, spec.GetOnlyScenarios(), *transform.MutableDegradation());
    transform.MutableDegradation()->SetAutoRepeatLimit(autoRepeatLimit);
    transform.SetSource(true);

    auto& publish = *spec.MutableResult()->MutablePublisher();
    publish.MutableStartrek()->SetTicket(request.Ticket);
    publish.MutableStartrek()->SetExperimentTicket(request.ExperimentTicket);
    publish.SetReShootMode(EReShootMode::RELEASE);
    SetupTelegram(request.TelegramGroupToNotify, *publish.MutableTelegram());

    auto battleId = Executor.Execute(spec);
    DataModel.AddExperimentBattle(request.ExperimentTicket, battleId);
    return battleId;
}
