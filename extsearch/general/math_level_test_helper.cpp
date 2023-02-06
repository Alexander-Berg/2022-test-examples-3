#include "math_level_test_helper.h"

#include <yweb/younglings/common/database/lib/prepared_query/prepared_query.h>

#include <util/string/builder.h>

#include <random>

namespace NEducation {

TVector<TMathLevelTestProblemsForPosition> GetMathLevelTestProblems(
    NYounglings::TParallelYDBConnector& connector,
    ui64 randomSeed,
    ui64 subjectId,
    ui64 stage)
{
    TVector<TMathLevelTestProblemsForPosition> result;

    TStringBuilder query;
    query <<
        "SELECT stage, subject_id, position, problem_id "
        "FROM [" << connector.GetDatabasePrefix() << "math_level_test_problems] "
        "WHERE stage = $stage AND subject_id = $subject_id;";
    query <<
        "SELECT stage, subject_id, position, showed_problems_count, are_grouped_together "
        "FROM [" << connector.GetDatabasePrefix() << "math_level_test_positions_info] "
        "WHERE stage = $stage AND subject_id = $subject_id;";

    NYounglings::TTypedQuery q;
    q.Type = "GetMathLevelTestProblemsAndPositionsInfo";

    q.Query.SetQuery(query);
    q.Query.AddParam("$stage", stage);
    q.Query.AddParam("$subject_id", subjectId);

    const auto& queryResult = connector.ExecuteTypedQuery(std::move(q));
    const auto& resultSet0 = queryResult[0].GetArray();
    const auto& resultSet1 = queryResult[1].GetArray();

    if (resultSet0.empty()) {
        ythrow yexception()
            << "Not enough problems for math level test, "
            << "stage = " << stage << ", "
            << "subject_id = " << subjectId;
    }

    TMap<ui64, TVector<ui64>, TLess<ui64>> positionProblems;
    THashMap<ui64, TMathLevelTestProblemsPositionInfo> positionsInfo;

    for (const auto& dbRow : resultSet1) {
        ui64 position = dbRow["position"].GetIntNumber();
        TMathLevelTestProblemsPositionInfo positionInfo;
        positionInfo.AreGroupedTogether = dbRow["are_grouped_together"].GetBool();
        positionInfo.ShowedProblemsCount = dbRow["showed_problems_count"].GetIntNumber();
        positionsInfo[position] = positionInfo;
    }

    for (const auto& dbRow : resultSet0) {
        ui64 position = dbRow["position"].GetIntNumber();
        ui64 problemId = dbRow["problem_id"].GetIntNumber();
        positionProblems[position].push_back(problemId);

        //use default info for missed positions
        if (!positionsInfo.contains(position)) {
            positionsInfo[position] = TMathLevelTestProblemsPositionInfo();
        }
    }

    std::mt19937 randomGenerator(randomSeed);

    for (auto& [position, problems] : positionProblems) {
        if (problems.size() < positionsInfo[position].ShowedProblemsCount) {
            ythrow yexception()
                << "Not enough problems for math level test, "
                << "stage = " << stage << ", "
                << "subject_id = " << subjectId << ", "
                << "position = " << position;
        }

        TMathLevelTestProblemsForPosition problemsRecord;
        problemsRecord.Position = position;
        problemsRecord.AreGroupedTogether = positionsInfo[position].AreGroupedTogether;

        std::random_shuffle(problems.begin(), problems.end(), [&](int i){return randomGenerator() % i;});
        for (size_t idx = 0; idx < positionsInfo[position].ShowedProblemsCount; ++idx) {
            problemsRecord.Problems.push_back(problems[idx]);
        }

        result.push_back(problemsRecord);
    }

    return result;
}

TString GetRecommendationsForStage2(
    const TVector<TMathLevelTestStage1Result>& results)
{
    Y_ASSERT(results.size() == MATH_LEVEL_TEST_STAGE_1_PROBLEMS_COUNT);
    ui64 correctlySolvedCount = 0;

    for (const auto& [position, userResults]: results) {
        for (bool result : userResults) {
            if (result) {
                ++correctlySolvedCount;
                break;
            }
        }
    }

    Y_ENSURE(correctlySolvedCount <= results.size());

    if (results.size() - correctlySolvedCount < 2) {
        return MATH_LEVEL_TEST_RECOMMENDATION_PROFILE;
    }

    if (results.size() - correctlySolvedCount < 5) {
        return MATH_LEVEL_TEST_RECOMMENDATION_UNCERTAIN;
    }

    return MATH_LEVEL_TEST_RECOMMENDATION_BASIC;
}

TString GetTextForTestFinish(const ui64 solvedCount, const ui64 subjectId)
{
    TString recommendationText;

    if (subjectId == 1) {
        recommendationText = GetRecommendationTextForBase(solvedCount);
    }

    if (subjectId == 2) {
        recommendationText = GetRecommendationTextForProfile(solvedCount);
    }

    return recommendationText;
}

TString GetRecommendationTextForBase(const ui64 solvedCount)
{
    if (solvedCount <= 4) {
        return MATH_LEVEL_TEST_RECOMMENDATION_BASE_0_4;
    }
    if (solvedCount >= 5 && solvedCount <= 8) {
        return MATH_LEVEL_TEST_RECOMMENDATION_BASE_5_8;
    }
    if (solvedCount >= 9 && solvedCount <= 13) {
        return MATH_LEVEL_TEST_RECOMMENDATION_BASE_9_13;
    }
    if (solvedCount >= 14 && solvedCount <= 17) {
        return MATH_LEVEL_TEST_RECOMMENDATION_BASE_14_17;
    }
    //if (solvedCount >= 18)
    return MATH_LEVEL_TEST_RECOMMENDATION_BASE_18_PLUS;
}

TString GetRecommendationTextForProfile(const ui64 solvedCount)
{
    if (solvedCount <= 7) {
        return MATH_LEVEL_TEST_RECOMMENDATION_PROFILE_0_7;
    }
    if (solvedCount >= 8 && solvedCount <= 12) {
        return MATH_LEVEL_TEST_RECOMMENDATION_PROFILE_8_12;
    }
    if (solvedCount >= 13 && solvedCount <= 17) {
        return MATH_LEVEL_TEST_RECOMMENDATION_PROFILE_13_17;
    }
    //if (solvedCount >= 18)
    return MATH_LEVEL_TEST_RECOMMENDATION_PROFILE_18_PLUS;
}

} //namespace NEducation
