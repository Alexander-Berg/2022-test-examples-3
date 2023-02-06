#include "lesson_test_block.h"
#include <extsearch/younglings/education/page_data_blocks/helpers/item_converter.h>

TVector<NEducation::TCommand> NEducation::TLessonTestPageDataBlock::GetQuery(
    const NYounglings::TRequestParameters&)
{
    TGetVariantCommand command;
    command.SetVariantId(CommonPageData.Scheme["lesson"]["variant"].GetIntNumber());
    TCommand wrappingCommand;
    wrappingCommand.SetType(ECommandType::GetVariant);
    wrappingCommand.MutableGetVariantCommand()->CopyFrom(command);
    return {wrappingCommand};
}

NSc::TValue NEducation::TLessonTestPageDataBlock::GetBlock(const NYounglings::TRequestParameters& requestParams) {
    QueryResults.push_back(NSc::TValue::Null()); // mesh data

    NSc::TValue result;
    const NSc::TValue variantData = TVariantPageDataBlock::GetBlock(requestParams);
    result["lessonMain"]["test"] = variantData;
    result["formulasData"] = variantData["formulasData"];
    return result;
}

TString NEducation::TLessonTestPageDataBlock::CalcCacheKey(const NYounglings::TRequestParameters& requestParams) {
    return "lesson_test_info_" + ToString(requestParams.GetUIntParameter("lesson_id", NYounglings::ERequestParameterSource::Cgi));
}
