#include "math_level_test_finish_quiz_handler.h"

TVector<NEducation::TCommand>  NEducation::TMathLevelTestFinishQuizHandler::GetQuery(
    const NYounglings::TRequestParameters& requestParams)
{
    const auto& answers = GetAnswersString(requestParams.BodyParams["data"]);
    const ui64 serverTime = NYounglings::TServerTime::Now(requestParams).Seconds();
    const ui64 totalTime = requestParams.GetUIntParameterWithDefault(
        "total_time", NYounglings::ERequestParameterSource::Body, 0);

    TCommand command;
    command.SetType(ECommandType::AddSpecialQuizSubmission);
    command.MutableAddSpecialQuizSubmissionCommand()->SetQuizId(ESpecialQuizes::MathLevelTest);
    command.MutableAddSpecialQuizSubmissionCommand()->SetUserId(requestParams.UserId);
    command.MutableAddSpecialQuizSubmissionCommand()->SetTimestamp(serverTime);
    command.MutableAddSpecialQuizSubmissionCommand()->SetAnswers(answers);
    command.MutableAddSpecialQuizSubmissionCommand()->SetTotalTime(totalTime);

    return {command};
}

TString NEducation::TMathLevelTestFinishQuizHandler::GetAnswersString(
    const NJson::TJsonValue& userAnswers)
{
    const TString answersString = ToString(userAnswers);
    if (answersString.length() > MAX_QUIZ_ANSWER_LENGTH) {
        ythrow yexception() << "Quiz answer is too long";
    }

    return answersString;
}
