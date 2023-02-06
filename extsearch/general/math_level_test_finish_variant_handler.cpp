#include "math_level_test_finish_variant_handler.h"

#include <extsearch/younglings/education/page_data_blocks/helpers/report_helper.h>

NSc::TValue NEducation::TMathLevelTestFinishVariantHandler::FetchBlock(
    const NYounglings::TRequestParameters& requestParams,
    NYounglings::TParallelYDBConnector& connector)
{
    const ui64 stage = requestParams.GetUIntParameterWithDefault(
        "stage", NYounglings::ERequestParameterSource::Body, 1);

    const ui64 subjectId = requestParams.GetUIntParameterWithDefault(
        "subject_id", NYounglings::ERequestParameterSource::Body, 1);

    const auto& userAnswers = requestParams.BodyParams["result"];

    auto variantCompletionData = FillVariantCompletionData(requestParams, stage);

    const NSc::TValue queryResult = GetProblems(userAnswers, connector);
    const NSc::TValue problems = queryResult[0];
    const NSc::TValue tags = queryResult[1];

    variantCompletionData
        .SetUserAnswers(userAnswers)
        .BuildProblemsAnswersFromDB(problems)
        .BuildTagsFromDB(tags)
        .BuildCorrectAnswersSet();

    variantCompletionData.FillSubmissionsInfo();

    for (const auto& [key, value] : variantCompletionData.ProblemsInfoMap) {
        variantCompletionData.ProblemsInfo.Push(value);
    }

    NSc::TValue result;
    result["report"] = TVariantCompletionHelper::GetReport(
        variantCompletionData,
        GetConvertedProblems(requestParams, problems).Items,
        "math_level_test",
        NSc::TValue::Null(), // variantInfo
        Nothing() // hideAnswersUntilTimestamp
    )["math_level_test"]["report"]["report"];

    if (stage == 1) {
        result.MergeUpdate(GetResponseForStageOne(userAnswers));
    }
    if (stage == 2) {
        const ui64 solvedCount = result["report"]["solvedRight"];
        result.MergeUpdate(GetResponseForStageTwo(solvedCount, subjectId));
    }

    result["report"]["totalTime"] = variantCompletionData.TotalTime;
    result["report"]["items"] = SortTasks(userAnswers, result["report"]["items"]);

    ExecuteSubmitQueries(variantCompletionData, connector, problems);

    return result;
}

NSc::TValue NEducation::TMathLevelTestFinishVariantHandler::GetProblems(
    const NJson::TJsonValue& problems,
    NYounglings::TParallelYDBConnector& connector) const
{
    TCommand command;
    command.SetType(ECommandType::GetProblemsByIds);

    for (const auto& problem: problems.GetArray()) {
        command.MutableGetProblemsByIdsCommand()->
            AddProblemsIds(GetInnerProblemId(problem["id"].GetString()));
    }

    command.MutableGetProblemsByIdsCommand()->SetGetEgeNumbers(true);
    command.MutableGetProblemsByIdsCommand()->SetGetTags(true);

    const auto& commandProcessor = TYQLCommandProcessor(connector.GetDatabasePrefix());
    const auto& queryResult = connector.ExecuteTypedQuery(
        commandProcessor.ProcessCommand(command));

    Y_ENSURE(queryResult[0].GetArray().size() == problems.GetArray().size());

    return queryResult;
}

TString NEducation::TMathLevelTestFinishVariantHandler::GetProblemsString(
    const NSc::TValue& problems) const
{
    NSc::TValue problemsIds;

    for (const auto& problem: problems.GetArray()) {
        problemsIds.Push(problem["problem_id"]);
    }

    return problemsIds.ToJson();
}

NSc::TValue NEducation::TMathLevelTestFinishVariantHandler::GetResponseForStageOne(
    const NJson::TJsonValue& problems) const
{
    NSc::TValue result;

    NSc::TValue button1;
    button1["text"] = "Базовый уровень";
    button1["subjectId"] = 1;
    result["choiceButtons"].Push(button1);

    NSc::TValue button2;
    button2["text"] = "Профильный уровень";
    button2["subjectId"] = 2;
    result["choiceButtons"].Push(button2);

    THashMap<ui64, TVector<bool>> positionToResultsMap;
    for (const auto& problem: problems.GetArray()) {
        positionToResultsMap[problem["position"].GetUInteger()].push_back(problem["result"].GetBoolean());
    }

    TVector<TMathLevelTestStage1Result> userResults;
    for (const auto& [position, userResult]: positionToResultsMap) {
        TMathLevelTestStage1Result positionResult;
        positionResult.Position = position;
        positionResult.UserResults = userResult;

        userResults.push_back(positionResult);
    }

    result["text"] = GetRecommendationsForStage2(userResults);

    return result;
}

NSc::TValue NEducation::TMathLevelTestFinishVariantHandler::GetResponseForStageTwo(
    const ui64 solvedCount,
    const ui64 subjectId) const
{
    NSc::TValue result;

    result["text"] = TStringBuilder()
        << "Результаты по итогам диагностической работы.\n\n\n"
        << GetTextForTestFinish(solvedCount, subjectId);
    result["showReports"] = true;

    return result;
}

NEducation::TItemsWithMeta NEducation::TMathLevelTestFinishVariantHandler::GetConvertedProblems(
    const NYounglings::TRequestParameters& requestParams,
    const NSc::TValue& problems) const
{
    const auto& displayParams = TProblemsDisplayParams::GetVariantProblemsDisplayParams();
    size_t index = 0;
    return TProblemConverter::GetProblemsFromDatabaseQueryResults(
        requestParams, displayParams, problems, index, true, TutorBlockParams.EnableAdminLink);
}

NSc::TValue NEducation::TMathLevelTestFinishVariantHandler::SortTasks(
    const NJson::TJsonValue& orderedProblems,
    NSc::TValue& problems) const
{
    THashMap<TString, NSc::TValue> problemIdToProblemMap;
    for (const auto& problem : problems.GetArray()) {
        problemIdToProblemMap[problem["taskId"]] = problem;
    }

    NSc::TValue result;

    for (const auto& orderedProblem: orderedProblems.GetArray()) {
        NSc::TValue problem = problemIdToProblemMap[orderedProblem["id"].GetString()];
        problem["taskNumber"] = orderedProblem["positionTitle"].GetString();
        result.Push(problem);
    }

    return result;
}

NEducation::TVariantCompletionData NEducation::TMathLevelTestFinishVariantHandler::FillVariantCompletionData(
    const NYounglings::TRequestParameters& requestParams,
    const ui64 stage) const
{
    TVariantCompletionData variantCompletionData;

    variantCompletionData.UserId = requestParams.UserId;
    variantCompletionData.VariantId = 0;
    variantCompletionData.SubjectId = requestParams.GetUIntParameterWithDefault(
        "subject_id", NYounglings::ERequestParameterSource::Body, 1);
    variantCompletionData.StartTimestamp = requestParams.GetUIntParameterWithDefault(
        "attempt_number", NYounglings::ERequestParameterSource::Body, 0);
    variantCompletionData.TotalTime = requestParams.GetUIntParameterWithDefault(
        "total_time", NYounglings::ERequestParameterSource::Body, 0);
    variantCompletionData.Timestamp = NYounglings::TServerTime::Now(requestParams).Seconds();
    variantCompletionData.ReportId = CalcReportId(
        variantCompletionData.UserId,
        variantCompletionData.VariantId,
        variantCompletionData.StartTimestamp,
        ToString(stage));

    variantCompletionData.CorrectAnswersCount = variantCompletionData.CorrectSubmittedAnswers.size();

    return variantCompletionData;
}

void NEducation::TMathLevelTestFinishVariantHandler::ExecuteSubmitQueries(
    NEducation::TVariantCompletionData& variantCompletionData,
    NYounglings::TParallelYDBConnector& connector,
    const NSc::TValue& problems) const
{
    const auto& commandProcessor = TYQLCommandProcessor(connector.GetDatabasePrefix());

    TCommand wrappingCommand;
    wrappingCommand.SetType(ECommandType::AddMultipleSubmissions);
    wrappingCommand.MutableAddMultipleSubmissionsCommand()->CopyFrom(variantCompletionData.MultipleSubmissionsCommand);

    connector.ExecuteTypedQuery(
        commandProcessor.ProcessCommand(wrappingCommand));

    variantCompletionData.FinishVariantCommand.SetProblemsInfo(GetProblemsString(problems));
    wrappingCommand.SetType(ECommandType::FinishVariant);
    wrappingCommand.MutableFinishVariantCommand()->CopyFrom(variantCompletionData.FinishVariantCommand);

    connector.ExecuteTypedQuery(
        commandProcessor.ProcessCommand(wrappingCommand));
}
