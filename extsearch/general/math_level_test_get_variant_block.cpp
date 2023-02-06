#include "math_level_test_get_variant_block.h"

static const TString KEY = "mathLevelTest";

NSc::TValue NEducation::TMathLevelTestGetVariantBlock::FetchBlock(
    const NYounglings::TRequestParameters& requestParams,
    NYounglings::TParallelYDBConnector& connector)
{
    const ui64 seed = GetRandomSeed(requestParams);

    const ui64 selectedSubjectId = requestParams.GetUIntParameterWithDefault(
        "subject_id", NYounglings::ERequestParameterSource::Cgi, 0);
    const ui64 stage = (selectedSubjectId == 0) ? 1 : 2;

    const TVector<TMathLevelTestProblemsForPosition> problemsToGet =
        GetMathLevelTestProblems(connector, seed, selectedSubjectId, stage);

    Y_ENSURE(!problemsToGet.empty() && !problemsToGet[0].Problems.empty());

    ProblemsCount = problemsToGet.size();

    TCommand command;
    command.SetType(ECommandType::GetProblemsByIds);

    THashMap<ui64, TMathLevelTestProblemsForPosition> problemsPositionsMap;

    for (const auto& problemsForPosition: problemsToGet) {
        problemsPositionsMap[problemsForPosition.Position] = problemsForPosition;

        for (const auto& problemId: problemsForPosition.Problems) {
            command.MutableGetProblemsByIdsCommand()->AddProblemsIds(problemId);
        }
    }

    command.MutableGetProblemsByIdsCommand()->SetGetEgeNumbers(true);
    command.MutableGetProblemsByIdsCommand()->SetGetAuthors(true);
    command.MutableGetProblemsByIdsCommand()->SetGetDescription(true);
    command.MutableGetProblemsByIdsCommand()->SetGetFormulaImages(true);

    const auto& commandProcessor = TYQLCommandProcessor(connector.GetDatabasePrefix());
    const auto& queryResult = connector.ExecuteTypedQuery(
       commandProcessor.ProcessCommand(command));

    size_t problemsIndex = 0;

    Y_ENSURE(!queryResult.GetArray().empty());

    NSc::TValue result = GetProblemsFromDBAnswer(
        problemsPositionsMap, requestParams, queryResult, problemsIndex, stage);

    result[KEY]["title"]         = MATH_LEVEL_TEST_TITLE;
    result[KEY]["description"]   = MATH_LEVEL_TEST_DESCRIPTION;
    result[KEY]["attemptNumber"] = NYounglings::TServerTime::Now(requestParams).Seconds();
    result[KEY]["isQuizFinished"] = CheckQuizIsFinished(
        requestParams.UserId, connector, commandProcessor);

    return result;
}

bool NEducation::TMathLevelTestGetVariantBlock::CheckQuizIsFinished(
    const TString& userId,
    NYounglings::TParallelYDBConnector& connector,
    const TYQLCommandProcessor& commandProcessor) const
{
    TCommand command;
    command.SetType(ECommandType::GetSpecialQuizAnswersForUser);
    command.MutableGetSpecialQuizAnswersForUserCommand()->SetUserId(userId);
    command.MutableGetSpecialQuizAnswersForUserCommand()->SetQuizId(ESpecialQuizes::MathLevelTest);

    const auto& queryResult = connector.ExecuteTypedQuery(
        commandProcessor.ProcessCommand(command));

    return (queryResult[0].GetArray().size() > 0);
}

NSc::TValue NEducation::TMathLevelTestGetVariantBlock::GetProblemsFromDBAnswer(
    THashMap<ui64, TMathLevelTestProblemsForPosition>& problemsPositionsMap,
    const NYounglings::TRequestParameters& requestParams,
    const NSc::TValue& queryResult,
    size_t problemsIndex,
    const ui64 stage) const
{
    const auto& displayParams = TProblemsDisplayParams::GetControlVariantDisplayParams();

    const TItemsWithMeta problemsWithMeta = TProblemConverter::GetProblemsFromDatabaseQueryResults(
        requestParams, displayParams, queryResult, problemsIndex, true, TutorBlockParams.EnableAdminLink);

    NSc::TValue result;
    THashMap<ui64, NSc::TValue> problemsMap;
    const auto& problems = problemsWithMeta.Items.GetArray();

    for (const auto& problem: problems) {
        const ui64 problemId = problem["integer_id"];
        problemsMap[problemId] = problem;
    }

    for (size_t problemPosition = 1; problemPosition <= ProblemsCount; problemPosition++) {
        if (!problemsPositionsMap.contains(problemPosition)) {
            ythrow yexception() << "No problems for position = " << problemPosition;
        }

        const auto& problemsForPosition = problemsPositionsMap[problemPosition];

        NSc::TValue problem = TryGetProblemFromMap(problemsMap, problemsForPosition.Problems[0]);
        problem = FillProblemPositionData(problem, problemPosition, 0);

        const bool needToGroup = problemsForPosition.AreGroupedTogether && (stage == 1);
        const auto& similarProblems = GetSimilarProblems(
            problemPosition,
            problemsForPosition.Problems,
            problemsMap);

        if (needToGroup) {
            problem["similar_tasks"] = similarProblems;
            result[KEY]["tasks"].Push(problem);
        } else {
            result[KEY]["tasks"].Push(problem);
            for (const auto& element: similarProblems.GetArray()) {
                result[KEY]["tasks"].Push(element);
            }
        }
    }

    result["formulasData"] = problemsWithMeta.Meta;

    return result;
}

NSc::TValue NEducation::TMathLevelTestGetVariantBlock::FillProblemPositionData(
    NSc::TValue& problem,
    const ui64 problemPosition,
    const ui64 groupIndex) const
{
    problem["partial_title"] = GetPartialNameForProblem(problemPosition, groupIndex);
    problem["positionTitle"] = GetPositionTitleForIndex(problemPosition, groupIndex);
    problem["position"] = problemPosition;

    return problem;
}

NSc::TValue NEducation::TMathLevelTestGetVariantBlock::GetSimilarProblems(
    const ui64 problemPosition,
    const TVector<ui64>& problemsForPosition,
    THashMap<ui64, NSc::TValue>& problemsMap) const
{
    NSc::TValue similarProblems;

    for (size_t groupIndex = 1; groupIndex < problemsForPosition.size(); groupIndex++) {
        NSc::TValue problem = TryGetProblemFromMap(problemsMap, problemsForPosition[groupIndex]);
        problem = FillProblemPositionData(problem, problemPosition, groupIndex);

        similarProblems.Push(problem);
    }

    return similarProblems;
}

TString NEducation::TMathLevelTestGetVariantBlock::GetPartialNameForProblem(
    const ui64 problemNumber,
    const ui64 index) const
{
    return TStringBuilder() << "Задание " << problemNumber << '.' << (index + 1);
}

TString NEducation::TMathLevelTestGetVariantBlock::GetPositionTitleForIndex(
    const ui64 problemNumber,
    const ui64 index) const
{
    return TStringBuilder() << problemNumber << '.' << (index + 1);
}

ui64 NEducation::TMathLevelTestGetVariantBlock::GetRandomSeed(
    const NYounglings::TRequestParameters& requestParams) const
{
    return MD5::CalcHalfMix(TStringBuilder()
        << NYounglings::TServerTime::Now(requestParams).Seconds()
        << '/'
        << requestParams.UserId);
}

NSc::TValue NEducation::TMathLevelTestGetVariantBlock::TryGetProblemFromMap(
    THashMap<ui64, NSc::TValue>& problemsMap,
    const ui64 problemId) const
{
    if (!problemsMap.contains(problemId)) {
        ythrow yexception() << "Problem with id " << problemId << " was not found";
    }

    return problemsMap[problemId];
}
