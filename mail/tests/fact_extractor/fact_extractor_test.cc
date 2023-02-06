#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/message_body/tests/http_client_mock.h>
#include <mail/message_body/tests/test_with_yield_context.h>
#include <internal/fact_extractor/fact_extractor.h>
#include <internal/fact_extractor/async_fact_extractor.h>

namespace {
using namespace testing;
using namespace msg_body;
typedef msg_body::Transport Transport;
typedef Transport::RequestPtr Request;
typedef msg_body::FactExtractor::Query Query;

struct TransportMock : public Transport {
    MOCK_METHOD(Request, postStub, (const std::string & params,
                                      const std::string & content, const std::string& requestId), (const));
    Request post(const std::string& params, const std::string& content, const std::string& requestId) const override {
        return Request(postStub(params, content, requestId));
    }
};

struct QueryTest : public Test {
    TransportMock transport;
    Request request;
    Query query( const std::string & content ) const {
        return Query(content, transport);
    }
    QueryTest() : request(0){}
};

struct TestFactExtractor : public TestWithYieldContext {
    MessagePart messagePart{"", "", "", "content", "", "", false, false, false, true, true, false, false, {}, {}, {}, {}, {}, {}};
    MessageContext messageContext;
    ParserConfig parserConfig{};
    TransportImpl::Config extractorConfig{"fact_extractor.ru", 250000, 1000000, true, false};
    std::string requestId = "requestId";
    TransformerAttributes attr;
    const PaLog paLog = PaLog {"FactExtractor"};
    LogPtr dummyLogger = std::make_shared<ContextLogger>(makeLoggerWithRequestId(""));
    const GetClusterClientMockWrapper getClusterClient {};
};

void makeAttr(TransformerAttributes& attr) {
    attr.flags.showContentMeta = true;
    attr.from = "kl@r.net";
    time_t timer(0);
    attr.messageDate = timer;
    attr.timeZoneOffset = 1;
    attr.mid = "1";
    attr.types = {1, 2, 3};
    attr.lang = "lang";
    attr.uid = "uid";
}

TEST_F( TestFactExtractor,  for_correct_attributes_should_build_request) {
    withSpawn([&] (YieldCtx yc) {
        makeAttr(attr);
        messageContext.originalMessageId_ = "originalMessageId";
        std::set<std::string> ticketEntity;
        ticketEntity.insert("ticket");
        ticketEntity.insert("events");
        TypeMsg rule{{ticketEntity}, {}, {}};
        parserConfig.rules.push_back(rule);
        TextAsyncFactExtractor factExtractor {dummyLogger, parserConfig, extractorConfig, requestId, yc, getClusterClient};
        auto request = yhttp::request::POST(
            "fact_extractor.ru/?e=events,ticket&domain=r.net&email=kl@r.net&lang=lang&mid=1&original_message_id=originalMessageId&part=message&time=0&time_zone=1&types=1,2,3&uid=uid",
            "Content-Length: 7\r\n"
            "Content-Type: text/html\r\n"
            "X-Request-Id: requestId\r\n",
            "content"
        );
        EXPECT_EQ(factExtractor.extractFactsBegin(messagePart, messageContext, attr)->request, request);
    });
}

TEST_F( QueryTest, for_non_empty_entities_correct_call_request ) {
    Query::Entities ticketEntity;
    ticketEntity.insert("ticket");
    EXPECT_CALL(transport, postStub(_,"content", _)).WillOnce(Return(request));
    query("content").entities(ticketEntity).request("");
}

TEST_F( QueryTest, empty_query_response_throws_exception ) {
    EXPECT_THROW( query("content").request(""), std::logic_error);
}

TEST_F( QueryTest, query_empty_content_throws_exception ) {
    EXPECT_THROW( query(""), std::logic_error);
}

TEST_F( QueryTest, query_empty_part_attribure_throws_exception ) {
    EXPECT_THROW( query("content").part(""), std::logic_error );
}

TEST_F( QueryTest, query_empty_domain_attribure_throws_exception ) {
    EXPECT_THROW( query("content").domain(""), std::logic_error );
}

TEST_F( QueryTest, query_empty_mid_attribure_throws_exception ) {
    EXPECT_THROW( query("content").mid(""), std::logic_error );
}

TEST_F( QueryTest, query_contact_with_no_email_throws_exception ) {
    EXPECT_THROW( query("content").contact("","000666"), std::logic_error);
}

TEST_F( QueryTest, queryContactWithNoUidThrowsException ) {
    EXPECT_THROW( query("content").contact("email@domain.com",""), std::logic_error);
}

TEST_F( QueryTest, query_single_entity_with_attributes_returns_correct_params ) {
    Query::Entities ticketEntity;
    ticketEntity.insert("ticket");
    EXPECT_CALL(transport, postStub("/?e=ticket&domain=domain.com&mid=777&part=message&types=1,3",_, _))
            .WillOnce(Return(request));
    query("content").entities(ticketEntity).part("message").domain("domain.com").mid("777").types("1,3").request("");
}

TEST_F( QueryTest, query_multiple_entities_with_attributes_returns_correct_params ) {
    Query::Entities multipleEntities;
    multipleEntities.insert("abook-contacts");
    multipleEntities.insert("events");
    EXPECT_CALL(transport, postStub("/?e=abook-contacts,events&domain=domain.com&email=kohen@yandex-team.ru&mid=999&part=attachment&time=1360757133&time_zone=10800&types=1,2,3&uid=159823277",_, _))
            .WillOnce(Return(request));
    query("content").entities(multipleEntities).part("attachment").domain("domain.com").time(1360757133)
            .timeZone(10800)
            .mid("999").types("1,2,3").contact("kohen@yandex-team.ru","159823277").request("");
}

}
