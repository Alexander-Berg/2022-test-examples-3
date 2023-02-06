#pragma once

#include "block_common.h"

namespace NEducation {
class TMathLevelTestGetQuizBlock: public TTutorReadTransactionPageDataBlock {
public:
    TMathLevelTestGetQuizBlock(
        const TTutorBlockParams& tutorBlockParams
    )
        : TTutorReadTransactionPageDataBlock(tutorBlockParams)
    {}

    ~TMathLevelTestGetQuizBlock() override {}

    virtual TVector<TCommand> GetQuery(
        const NYounglings::TRequestParameters& requestParams) override;

    virtual NSc::TValue GetBlock(
        const NYounglings::TRequestParameters& requestParams) override;

    virtual TString CalcCacheKey(
        const NYounglings::TRequestParameters& requestParams) override;

private:
    NSc::TValue ConvertQuestion(const NSc::TValue& questionFromDb) const;
};
}
