#include <mail/notsolitesrv/src/mthr/mthr.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/util/mthr.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/env.h>
#include <yamail/data/deserialization/yajl.h>

#include <gtest/gtest.h>

#include <memory>
#include <string>
#include <vector>

namespace {

using namespace testing;

using NNotSoLiteSrv::NMthr::TMthr;
using NNotSoLiteSrv::NMthr::TMthrPtr;
using NNotSoLiteSrv::NMthr::TMthrRequest;
using NNotSoLiteSrv::NMthr::TTestData;
using NNotSoLiteSrv::NMthr::TTestDataRequest;
using NNotSoLiteSrv::TContextPtr;
using yamail::data::deserialization::fromJson;

class TTestMthr : public Test {
public:
    std::string MakeMergeRulesPath() const {
        return ArcadiaSourceRoot() + "/mail/notsolitesrv/package/deploy/app/config/merge_rules.json";
    }

    std::string MakeTrivialSubjectsPath() const {
        return ArcadiaSourceRoot() +
            "/mail/notsolitesrv/package/deploy/app/config/trivial_subjects.json";
    }

    TMthrRequest MakeMthrRequest(TTestDataRequest testDataRequest) const {
        return {
            .HdrFromDomain = std::move(testDataRequest.Message.From.Domain),
            .Subject = std::move(testDataRequest.Message.Subject),
            .MsgTypes = std::move(testDataRequest.Types),
            .MessageId = std::move(testDataRequest.Message.MessageId),
            .References = std::move(testDataRequest.Message.References),
            .InReplyTo = std::move(testDataRequest.Message.InReplyTo),
            .DomainLabel = std::move(testDataRequest.DomainLabel)
        };
    }

protected:
    TContextPtr Context{GetContext({{"merge_rules", MakeMergeRulesPath()},
        {"trivial_subjects", MakeTrivialSubjectsPath()}})};
    TMthrPtr Mthr{std::make_shared<TMthr>(Context->GetConfig())};
};

class TTestMthrWithTestDataParam
    : public TTestMthr
    , public WithParamInterface<TTestData>
{
};

TEST_P(TTestMthrWithTestDataParam, http_thread_info) {
    auto param{GetParam()};
    auto mthrResult{Mthr->GetThreadInfo(Context, MakeMthrRequest(std::move(param.Request)))};
    ASSERT_TRUE(mthrResult);
    EXPECT_EQ(param.Out, *mthrResult);
}

INSTANTIATE_TEST_SUITE_P(UseTestData, TTestMthrWithTestDataParam,
    ValuesIn(fromJson<std::vector<TTestData>>(NResource::Find("mthr/data.json"))));

}
