#pragma once

#include <extsearch/younglings/education/page_data_blocks/helpers/math_level_test_helper.h>
#include <extsearch/younglings/education/page_data_blocks/helpers/variant_completion_helper.h>
#include <extsearch/younglings/education/page_data_blocks/helpers/problem_converter.h>
#include <extsearch/younglings/education/page_data_blocks/helpers/report_converter.h>
#include <extsearch/younglings/education/page_data_blocks/block_common.h>

#include <yweb/younglings/education/command_processor/command_chooser.h>

namespace NEducation {
    class TMathLevelTestFinishVariantHandler: public TTutorRWTransactionPageDataBlock {
    public:
        TMathLevelTestFinishVariantHandler(
            const NEducation::TTutorBlockParams& tutorBlockParams
        )
            : TTutorRWTransactionPageDataBlock(tutorBlockParams)
        {}

        NSc::TValue FetchBlock(
            const NYounglings::TRequestParameters& requestParams,
            NYounglings::TParallelYDBConnector& connector) override;

    private:
        NSc::TValue GetProblems(
            const NJson::TJsonValue& problems,
            NYounglings::TParallelYDBConnector& connector) const;

        TItemsWithMeta GetConvertedProblems(
            const NYounglings::TRequestParameters&,
            const NSc::TValue& problems) const;

        TString GetProblemsString(const NSc::TValue& problems) const;

        NSc::TValue GetResponseForStageOne(const NJson::TJsonValue& problems) const;
        NSc::TValue GetResponseForStageTwo(
            const ui64 solvedCount,
            const ui64 subjectId) const;

        NSc::TValue SortTasks(
            const NJson::TJsonValue& problemsFromBody,
            NSc::TValue& tasks) const;

        TVariantCompletionData FillVariantCompletionData(
            const NYounglings::TRequestParameters& requestParams,
            const ui64 stage) const;

        void ExecuteSubmitQueries(
            TVariantCompletionData& variantCompletionData,
            NYounglings::TParallelYDBConnector& connector,
            const NSc::TValue& problems) const;
    };
}
