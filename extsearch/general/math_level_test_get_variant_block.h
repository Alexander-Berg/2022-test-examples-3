#pragma once

#include "block_common.h"

#include <extsearch/younglings/education/page_data_blocks/helpers/problem_converter.h>
#include <extsearch/younglings/education/page_data_blocks/helpers/math_level_test_helper.h>

#include <yweb/younglings/education/command_processor/command_chooser.h>

#include <library/cpp/digest/md5/md5.h>

namespace NEducation {
class TMathLevelTestGetVariantBlock : public TTutorReadTransactionPageDataBlock {
public:
    TMathLevelTestGetVariantBlock(
        const TTutorBlockParams& tutorBlockParams
    )
        : TTutorReadTransactionPageDataBlock(tutorBlockParams)
    {}

    ~TMathLevelTestGetVariantBlock() override {}

    NSc::TValue FetchBlock(
        const NYounglings::TRequestParameters& requestParams,
        NYounglings::TParallelYDBConnector& connector) override;

private:
    NSc::TValue GetProblemsFromDBAnswer(
        THashMap<ui64, TMathLevelTestProblemsForPosition>& problemsPositionMap,
        const NYounglings::TRequestParameters& requestParams,
        const NSc::TValue& queryResult,
        size_t problems_index,
        const ui64 stage) const;

    TString GetPartialNameForProblem(
        const ui64 problemNumber,
        const ui64 index) const;
    TString GetPositionTitleForIndex(
        const ui64 problemNumber,
        const ui64 index) const;

    ui64 GetRandomSeed(const NYounglings::TRequestParameters& requestParams) const;

    NSc::TValue TryGetProblemFromMap(
        THashMap<ui64, NSc::TValue>& problemsMap,
        const ui64 problemId) const;

    NSc::TValue GetSimilarProblems(
        const ui64 problemPosition,
        const TVector<ui64>& problemsForPosition,
        THashMap<ui64, NSc::TValue>& problemsMap) const;

    NSc::TValue FillProblemPositionData(
        NSc::TValue& problem,
        const ui64 problemPosition,
        const ui64 groupIndex) const;

    bool CheckQuizIsFinished(
        const TString& userId,
        NYounglings::TParallelYDBConnector& connector,
        const TYQLCommandProcessor& commandProcessor) const;

private:
    ui64 ProblemsCount;
};
}
