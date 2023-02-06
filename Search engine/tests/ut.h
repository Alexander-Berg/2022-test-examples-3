#pragma once

#include <util/stream/str.h>
#include <util/string/cast.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/threading/serial_postprocess_queue/serial_postprocess_queue.h>

namespace NBg {
    bool RunTest(const TStringBuf, const TStringBuf, std::function<bool(const NJson::TJsonValue&, const NJson::TJsonValue&)>);

    class TRuleTest {
        using TRuleValidator = std::function<bool(const NJson::TJsonValue&, const NJson::TJsonValue&)>;

    public:
        TRuleTest(NJson::TJsonValue& requests, const NJson::TJsonValue& responses)
            : RequestsContext(requests.GetArray()),
              ResponsesContext(responses.GetArray())
        {}

        bool Run();

        void SetValidator(TRuleValidator validator) {
            Validator = std::move(validator);
        }

        class TRrrValidator : public TSerialPostProcessQueue::IProcessObject {
        public:
            TRrrValidator(std::function<NJson::TJsonValue()> f)
                : F(std::move(f))
            {}

            void ParallelProcess(void*) override {
                Result = F();
            }

            void SerialProcess() override {
                const auto ruleResultFromBegemot = Result.GetArraySafe().front()["rules"][RuleName];

                bool validationResult = Validator(ruleResultFromBegemot, RulesResult.at(ValidationIdx));
                ValidationResult &= validationResult;

                if (!validationResult) {
                    Cerr << "Validation failed on request " << ValidationIdx++ << Endl;
                    Cerr << "Begemot response:" << Endl << Result << Endl;
                } else {
                    Cerr << "Validation succeed on request " << ValidationIdx++ << Endl;
                }
            }

        private:
            NJson::TJsonValue Result;
            std::function<NJson::TJsonValue()> F;

        };

    private:
        TMaybe<NJson::TJsonValue> InsertRule(const NJson::TJsonValue::TArray&, size_t);

    private:
        NJson::TJsonValue::TArray RequestsContext;
        const NJson::TJsonValue::TArray ResponsesContext;
        TStringStream TestContext;
        inline static TString RuleName;
        inline static TRuleValidator Validator;
        inline static NJson::TJsonValue RuleResult;
        inline static int ValidationIdx = 0;
        inline static bool ValidationResult = true;
        inline static TVector<NJson::TJsonValue> RulesResult;
    };
}
