#include <market/kombat/handler/test_report_result.h>
#include <market/kombat/engine/tank.h>
#include <market/kombat/engine/utils.h>
#include <google/protobuf/text_format.h>

namespace {
    using namespace NMarket::NKombat;

    TMaybe<TString> GetErrorMessage(const TResultPack& result) {
        TVector<TString> errors;
        if (result.HasError()) {
            errors.push_back(result.GetError().GetMessage());
        }
        for (const auto& attack : result.GetSource().GetAttack()) {
            if (attack.HasError()) {
                TStringBuilder msg;
                msg << "Атака " << attack.GetFamilyNumber() << " упала с ошибкой: " << attack.GetError();
                errors.emplace_back(std::move(msg));
            }
        }
        if (errors.empty()) {
            return Nothing();
        }
        return JoinSeq("; ", errors);
    }
}

void TTestReportResult::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    args.Required("id", "battle id", &TRequest::BattleId);
}

NSc::TValue TTestReportResult::TResponse::AsJson() const {
    NSc::TValue result;
    result["status"] = Status;
    result["progress"] = Progress;
    result["error"] = Error;
    result["release_version"] = BaseVersion;
    result["test_version"] = TestVersion;
    result["fire_type"] = FireType;
    if (AmmoFilter) {
        result["ammo_filter"] = *AmmoFilter;
    }
    result["is_retry"] = IsRetry;
    result["kolmogorov_smirnov_result"] = KolmogorovSmirnovResult;

    if (!DegradationReasons.empty()) {
        auto& reasons = result["degradation_reasons"].GetArrayMutable();
        reasons.AppendAll(DegradationReasons);
    }

    result["success"].SetBool(Success);

    if (NextBattleId) {
        result["next_battle_id"] = *NextBattleId;
    }

    return result;
}

TStringBuf TTestReportResult::Describe() {
    return "Get result of report performance test";
}

TTestReportResult::TResponse TTestReportResult::Run(const TRequest& request) const {
    const auto battle = Battles.Find(request.BattleId);
    if (!battle) {
        throw NShiny::TClientError() << "Battle " << request.BattleId << " is not found";
    }

    TTestReportResult::TResponse response;
    const auto state = battle->GetState();
    response.Status = ToString(state.Status);
    response.Progress = state.Progress;
    const auto& spec = battle->GetMeta().Spec;
    response.FireType = GetFireTypeName(spec.GetFireType());
    if (spec.AttackSize() > 0 && spec.GetAttack(0).GetFire().GetAmmo().HasFilter()) {
        response.AmmoFilter = spec.GetAttack(0).GetFire().GetAmmo().GetFilter();
    }
    if (state.Status != IBattle::EStatus::COMPLETE) {
        return response;
    }

    if (battle->GetData().NextBattleId) {
        response.NextBattleId = *battle->GetData().NextBattleId;
    }

    const auto battleRes = battle->GetResult();
    if (const auto errorMsg = GetErrorMessage(battleRes)) {
        response.Error = *errorMsg;
    }

    if (battleRes.DegradationSize() == 0) {
        return response;
    }

    const auto& degrad = battleRes.GetDegradation(0);
    response.BaseVersion = degrad.GetBase().GetReportVersion();
    response.TestVersion = degrad.GetTest().GetReportVersion();
    response.IsRetry = degrad.GetIsRetry();

    response.DegradationReasons.insert(response.DegradationReasons.end(), degrad.GetReason().begin(), degrad.GetReason().end());

    if (battleRes.StatisticalTestsSize() > 0) {
        const auto& statTests = battleRes.GetStatisticalTests(0);
        if (statTests.HasKolmogorovSmirnov()) {
            response.KolmogorovSmirnovResult = statTests.GetKolmogorovSmirnov().GetAccept() ? "accept" : "reject";
        }
    }

    response.Success = !degrad.GetIsDegradation();

    return response;
}
