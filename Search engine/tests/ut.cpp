#include "ut.h"

#include <util/generic/xrange.h>

#include <util/system/fs.h>
#include <util/stream/output.h>
#include <util/stream/file.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>

#include <search/begemot/core/proto/config.pb.h>
#include <search/begemot/core/rulefactory.h>
#include <search/begemot/server/server.h>

bool NBg::RunTest(const TStringBuf requestsContext, const TStringBuf responsesContext, std::function<bool(const NJson::TJsonValue&, const NJson::TJsonValue&)> validator) {
    auto getJsonFromCtx = [](const TStringBuf name) {
       TStringStream context(NResource::Find(name));
       return NJson::ReadJsonTree(&context);
    };

    NJson::TJsonValue requestsJson = getJsonFromCtx(requestsContext), responsesJson = getJsonFromCtx(responsesContext);

    NBg::TRuleTest test(requestsJson, responsesJson);

    test.SetValidator(validator);
    return test.Run();
}

bool NBg::TRuleTest::Run() {
    if (RequestsContext.size() != ResponsesContext.size()) {
        throw yexception() << "Requests and responses dumps mismatch: " << RequestsContext.size() << " requests and " << ResponsesContext.size() << " responses.";
    }

    const NBg::TRuleFactory rf = NBg::DefaultRuleFactory();
    NBg::NProto::TConfig config;
    RuleName = rf.begin()->Name;
    config.SetDataDir(BinaryPath("search/begemot/rules/" + to_lower(RuleName) + "/ut/data/search/wizard/data/wizard"));

    for (auto idx : xrange(ResponsesContext.size())) {
        auto responseItems = ResponsesContext.at(idx).GetArraySafe().front()["results"].GetArraySafe();
        const auto canonicalResponse = InsertRule(responseItems, idx);
        if (!canonicalResponse) {
            throw yexception() << "Can't find rule " << RuleName << " in response context.";
        }
        RulesResult.push_back(*canonicalResponse);
    }
    StartWorkerStream<NBg::TRuleTest::TRrrValidator>(config, rf, &TestContext);
    TestContext.clear();
    return ValidationResult;
}

TMaybe<NJson::TJsonValue> NBg::TRuleTest::InsertRule(const NJson::TJsonValue::TArray& responseItems, size_t idx) {
    TMaybe<NJson::TJsonValue> testingRule;
    auto& requestItems = RequestsContext[idx].GetArraySafe().front()["results"].GetArraySafe();

    TString prefix;

    for (auto& item : requestItems) {
        if (item["type"].GetString() == "begemot_config") {
            item.InsertValue("protocol", NJson::TJsonValue("proto+json"));
        }
        if (item.Has("result_type")) {
            prefix = item["result_type"].GetStringSafe() + '#';
        }
    }

    for (const auto item : responseItems) {
        TStringBuf type = item["type"].GetStringSafe();
        if (!type.StartsWith(prefix)) {
            continue;
        }
        TStringBuf ruleName = type.Skip(prefix.Size());
        if (ruleName != RuleName) {
            requestItems.push_back(item);
        } else {
            testingRule = item;
        }
    }

    TestContext << NJson::WriteJson(&RequestsContext.at(idx), /*formatOutput=*/false) << Endl;
    return testingRule;
}
