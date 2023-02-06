#include <market/kombat/preset/presets.h>
#include <market/kombat/handler/test_report_batch.h>
#include <util/generic/string.h>
#include <util/string/builder.h>

namespace {
    using namespace NMarket::NKombat;

    TString MakeFireName(TBattleSpec::EType reportType, const TMaybe<TString>& ammoFilter) {
        TStringBuilder result;
        result << TBattleSpec_EType_Name(reportType);
        if (ammoFilter) {
            result << "@" << *ammoFilter;
        }

        return result;
    }
}

void TTestReportBatch::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    args.Required("version", "new report version", &TRequest::Version);
    args.Required("ticket", "startrek ticket", &TRequest::Ticket);
    args.Optional("base_version", "base report version", &TRequest::BaseVersion);
    args.Optional("owner", "test owner", &TRequest::Owner);
    args.Optional("use_stable_base_report", "use stable base report binary", &TRequest::UseStableBaseReport);
    args.Optional("use_stable_meta_report", "use stable meta report binary", &TRequest::UseStableMetaReport);
}

TStringBuf TTestReportBatch::Describe() {
    return "test report performance in batch mode";
}

TTestReportBatch::TResponse TTestReportBatch::Run(const TRequest& request) const {
    TResponse result;
    result.Battles = {
        RunDegrad(request, TBattleSpec::MAIN),
        RunDegrad(request, TBattleSpec::MAIN, "prime"),
        RunDegrad(request, TBattleSpec::API),
        RunDegrad(request, TBattleSpec::PARALLEL),
        RunDegrad(request, TBattleSpec::MAIN, Nothing(), true),
        RunDegrad(request, TBattleSpec::MAIN, Nothing(), false, true),
    };

    return result;
}

TTestReportBatch::TResponse::TBattleInfo TTestReportBatch::RunDegrad(
    const TRequest& request,
    TBattleSpec::EType reportType,
    const TMaybe<TString>& ammoFilter,
    bool maxRps,
    bool onlyScenarios
) const {
    constexpr ui32 autoRepeate = 2;
    const auto battle = BattleRunner.Run({
        .Version = request.Version,
        .Ticket = request.Ticket,
        .FireType = reportType,
        .BaseVersion = request.BaseVersion,
        .UseStableBaseReport = request.UseStableBaseReport.GetOrElse(false),
        .UseStableMetaReport = request.UseStableMetaReport.GetOrElse(false),
        .MaxRps = maxRps,
        .AutoRepeatLimit = autoRepeate,
        .AmmoFilter = ammoFilter,
        .Owner = request.Owner,
        .OnlyScenarios = onlyScenarios
    });

    return {
        .Id = battle.Id,
        .Name = MakeFireName(reportType, ammoFilter),
        .MaxRps = maxRps,
        .OnlyScenarios = onlyScenarios
    };
}
