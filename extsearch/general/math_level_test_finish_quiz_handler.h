#pragma once

#include <extsearch/younglings/education/page_data_blocks/block_common.h>

namespace NEducation {
    class TMathLevelTestFinishQuizHandler: public TTutorRWTransactionPageDataBlock {
    public:
        TMathLevelTestFinishQuizHandler(
            const NEducation::TTutorBlockParams& tutorBlockParams
        )
            : TTutorRWTransactionPageDataBlock(tutorBlockParams)
        {}

        virtual TVector<TCommand> GetQuery(
            const NYounglings::TRequestParameters& requestParams) override;

    private:
        TString GetAnswersString(const NJson::TJsonValue& userAnswers);
    };
}
