#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/core/include/types_error.h>
#include <mail/spaniel/service/include/hound.h>
#include <mail/http_getter/client/mock/mock.h>
#include <mail/spaniel/tests/unit/include/matchers.h>
#include <yplatform/reactor.h>

#include <boost/algorithm/string/join.hpp>

using namespace ::testing;
using namespace std::string_literals;

namespace spaniel::tests {

std::string makeEnvelope(const Id& mid) {
    return R"(
        {"mid":")" + std::to_string(mid) + R"(",
        "fid":"1","threadId":"170855310863364472","revision":493,"date":1614328831,"receiveDate":1573795913,
        "from":[{"local":"MAILER-DAEMON","domain":"","displayName":"MAILER-DAEMON"}],
        "replyTo":[{"local":"","domain":"","displayName":"MAILER-DAEMON"}],
        "subject":"No subject",
        "subjectInfo":{"type":"","prefix":"","subject":"No subject","postfix":"","isSplitted":true},
        "cc":[],
        "bcc":[],
        "hdrStatus":"","to":[{"local":"","domain":"","displayName":"No address"}],
        "hdrLastStatus":"",
        "uidl":"","imapId":"164","ImapModSeq":"",
        "stid":"320.mail:4003849558.E69075:137755370632421921860156596230",
        "firstline":"","inReplyTo":"","references":"",
        "rfcId":"<20200226114230.eVE07t45@iva8-9d43adb1a0cc.qloud-c.yandex.net>",
        "size":1330,"threadCount":0,"extraData":"","newCount":0,"attachmentsCount":1,"attachmentsFullSize":26,
        "attachments":[{"m_hid":"1","m_contentType":"message/rfc822","m_fileName":"Hello.eml","m_size":26}],
        "labels":["16","17","18","FAKE_ATTACHED_LBL","FAKE_RECENT_LBL","FAKE_SEEN_LBL"],
        "specialLabels":[],"types":[46,54],"tab":"relevant"}
    )";
}

yhttp::response houndError() {
    return yhttp::response {
        .status=200,
        .body=R"(
            {"error": {
                "code":5001,
                "message":"invalid argument",
                "reason":"Sharpei service responded with error code 400: sharpei_client error: bad http code"
            }})"
    };
}

yhttp::response internalServerError() {
    return yhttp::response {
        .status=500,
        .body=""
    };
}

yhttp::response nginxError() {
    return yhttp::response {
        .status=499,
        .body=""
    };
}

yhttp::response filterSearchResponse(const std::vector<Id>& mids) {
    std::vector<std::string> envelopes;
    for (const auto& mid: mids) {
        envelopes.emplace_back(makeEnvelope(mid));
    }

    return yhttp::response {
        .status=200,
        .body="{ \"envelopes\" : [" + boost::join(envelopes, ",") + "] }"
    };
}

struct HoundTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    http_getter::TypedEndpoint endpoint;
    http_getter::ResponseSequencePtr responses;
    http_getter::TypedClientPtr getter;

    const Uid uid {161616};
    const std::vector<Id> mids = { Id(100500), Id(100600) };

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        responses = std::make_shared<StrictMock<http_getter::ResponseSequence>>();
        getter = http_getter::createTypedDummyWithRequest(responses);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

TEST_F(HoundTest, should_return_two_envelopes_for_two_mids) {
    EXPECT_CALL(*responses, get(WithMids(mids)))
            .WillOnce(Return(filterSearchResponse(mids)));

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = hound::filterSearch(uid, mids, *getter, endpoint, yield);
        for (const auto& mid: mids) {
            EXPECT_TRUE(result);
            EXPECT_THAT(result.value(), HasSubstr(std::to_string(mid)));
        }
    });
}

TEST_F(HoundTest, should_not_retry_error_when_response_contains_4xx_code) {
    EXPECT_CALL(*responses, get(WithMids(mids)))
            .WillOnce(Return(nginxError()));

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = hound::filterSearch(uid, mids, *getter, endpoint, yield);
        EXPECT_FALSE(result);
        EXPECT_EQ(result.error(), make_error(RemoteServiceError::proxy));
    });
}

}
