#include "math_level_test_get_quiz_block.h"

static const TString KEY = "mathLevelQuiz";

TVector<NEducation::TCommand> NEducation::TMathLevelTestGetQuizBlock::GetQuery(
    const NYounglings::TRequestParameters& requestParams)
{
    Y_UNUSED(requestParams);

    TCommand command;
    command.SetType(ECommandType::GetSpecialQuizQuestions);
    command.MutableGetSpecialQuizQuestionsCommand()->SetQuizId(ESpecialQuizes::MathLevelTest);
    return {command};
}

NSc::TValue NEducation::TMathLevelTestGetQuizBlock::GetBlock(
    const NYounglings::TRequestParameters& requestParams)
{
    Y_UNUSED(requestParams);
    Y_ENSURE(!QueryResults.empty() && QueryResults[0].Defined());

    NSc::TValue result;
    for (const auto& questionFromDb: QueryResults[0].GetRef()[0].GetArray()) {
        result[KEY]["questions"].Push(ConvertQuestion(questionFromDb));
    }

    result[KEY]["title"] = MATH_LEVEL_TEST_TITLE;
    result[KEY]["description"] = MATH_LEVEL_TEST_QUIZ_DESCRIPTION;

    return result;
}

NSc::TValue NEducation::TMathLevelTestGetQuizBlock::ConvertQuestion(
    const NSc::TValue& questionFromDb) const
{
    NSc::TValue convertedQuestion = NSc::TValue::FromJson(questionFromDb["question"].GetString());
    convertedQuestion["id"] = TStringBuilder() 
        << "question_" << ToString(questionFromDb["position"]);
    return convertedQuestion;
}

TString NEducation::TMathLevelTestGetQuizBlock::CalcCacheKey(
    const NYounglings::TRequestParameters& requestParams)
{
    Y_UNUSED(requestParams);

    return KEY;
}
