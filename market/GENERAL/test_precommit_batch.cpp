#include <market/kombat/handler/test_precommit_batch.h>
#include <market/kombat/preset/presets.h>

using namespace NMarket::NKombat;

namespace {
    const TVector<TString> DEFAULT_TELEGRAM_GROUPS = {"market_kombat"};

    void SetupIndex(TIndexSpec& index) {
        index.AddTag(TIndexSpec::FULL);
        index.AddTag(TIndexSpec::PROD);
        index.AddTag(TIndexSpec::PUBLISHED);
        index.AddTag(TIndexSpec::DAYLY);
    }

    void SetupBaseAttackTarget(const TString& baseRevision, TTargetSpec& target) {
        ui32 revision = FromString<ui32>(baseRevision.substr(baseRevision[0] == 'r' ? 1 : 0));
        target.MutableReport()->SetRevision(revision);
        target.MutableMetaReport()->SetRevision(revision);
        SetupIndex(*target.MutableIndex());
    }

    void SetupTestAttackTarget(ui32 pullRequestId, TTargetSpec& target) {
        target.MutableReport()->MutablePatch()->SetReview(pullRequestId);
        target.MutableMetaReport()->MutablePatch()->SetReview(pullRequestId);
        SetupIndex(*target.MutableIndex());
    }

    void SetupAmmo(const TMaybe<TString>& ammoFilter, TAmmoSpec& ammoSpec) {
        ammoSpec.AddTag(TAmmoSpec::LAST);
        if (ammoFilter) {
            ammoSpec.SetFilter(*ammoFilter);
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

    void SetupTelegram(TTelegramSpec& telegramSpec) {
        telegramSpec.SetNotifyOnlyFinalDegradation(true);
        for (const auto& group : DEFAULT_TELEGRAM_GROUPS) {
            AddTelegramGroup(group, telegramSpec);
        }
    }
}

void TTestPrecommitBatch::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    args.Required("owner", "battle owner", &TRequest::Owner);
    args.Required("ticket", "startrek ticket", &TRequest::Ticket);
    args.Required("br", "base revision", &TRequest::BaseRevision);
    args.Required("pr", "pull request id", &TRequest::PullRequestId);
    args.Optional("priority", "pull request id", &TRequest::Priority, 100);
}

TStringBuf TTestPrecommitBatch::Describe() {
    return "test report in precommit mode";
}

TTestPrecommitBatch::TResponse TTestPrecommitBatch::Run(const TRequest& request) const {
    return {{
        RunDegrad(request, TBattleSpec_EType_MAIN, "prime")
    }};
}

TBattleId TTestPrecommitBatch::RunDegrad(
    const TRequest& request,
    TBattleSpec::EType reportType,
    const TMaybe<TString>& ammoFilter
) const {
    static constexpr bool oneShardMode = true;
    static constexpr ui32 attackRepeats = 3;
    static constexpr ui32 autoRepeatLimit = 0;

    IBattle::TSpec spec;
    const bool oneThread = EqualToOneOf(reportType, TBattleSpec_EType_MAIN, TBattleSpec_EType_API);
    spec.SetOwner(request.Owner);
    spec.SetFireType(reportType);
    spec.SetPriority(request.Priority);
    spec.SetOneShardMode(oneShardMode);

    auto attackSettings = GetDefaultAttackSettings(reportType, oneShardMode, oneThread, false, ammoFilter.GetOrElse(""));

    auto& baseAttack = *spec.AddAttack();
    SetupBaseAttackTarget(request.BaseRevision, *baseAttack.MutableTarget());
    SetupAmmo(ammoFilter, *baseAttack.MutableFire()->MutableAmmo());
    SetupAttackProfile(attackSettings, *baseAttack.MutableFire()->MutableProfile());
    baseAttack.SetRepeat(attackRepeats);

    auto& testAttack = *spec.AddAttack();
    SetupTestAttackTarget(request.PullRequestId, *testAttack.MutableTarget());
    SetupAmmo(ammoFilter, *testAttack.MutableFire()->MutableAmmo());
    SetupAttackProfile(attackSettings, *testAttack.MutableFire()->MutableProfile());
    testAttack.SetRepeat(attackRepeats);

    auto& collectorSpec = *spec.MutableResult()->MutableCollector();
    SetQuantilesPreset(collectorSpec);

    auto& transform = *spec.MutableResult()->MutableTransformer();
    transform.MutableDegradation()->SetQuantile(attackSettings.Quantile);
    SetDegradationPreset(reportType, oneShardMode, spec.GetOnlyScenarios(), *transform.MutableDegradation());
    transform.MutableDegradation()->SetAutoRepeatLimit(autoRepeatLimit);
    transform.SetSource(true);

    auto& publish = *spec.MutableResult()->MutablePublisher();
    publish.MutableStartrek()->SetTicket(request.Ticket);
    publish.SetReShootMode(EReShootMode::CLONING);
    SetupTelegram(*publish.MutableTelegram());

    auto battleId = Executor.Execute(spec);
    return battleId;
}
