#pragma once

#include "block_common.h"
#include "variant_block.h"

#include <extsearch/younglings/education/page_data_blocks/helpers/lesson_helper.h>

namespace NEducation {

class TLessonTestPageDataBlock : public TVariantPageDataBlock {
public:
    explicit TLessonTestPageDataBlock(
        const TTutorBlockParams& tutorBlockParams
    )
        : TVariantPageDataBlock(tutorBlockParams)
    {}

    TVector<TCommand> GetQuery(
        const NYounglings::TRequestParameters& requestParams) override;

    NSc::TValue GetBlock(
        const NYounglings::TRequestParameters& requestParams) override;

    TString CalcCacheKey(
        const NYounglings::TRequestParameters& requestParams) override;
};

} // namespace NEducation
