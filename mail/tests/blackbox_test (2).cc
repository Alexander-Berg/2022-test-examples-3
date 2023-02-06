#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/corgi/include/types_error.h>
#include <mail/webmail/corgi/include/resolve/blackbox.h>
#include <mail/http_getter/client/mock/mock.h>
#include <yplatform/reactor.h>


using namespace ::testing;

namespace corgi::tests {

const Uid PDD_UID(9);
const std::string YANDEX = R"({ "uid" : { "hosted" : false, "lite" : false, "value" : "1" } })";
const std::string PDD    = R"({ "uid" : { "hosted" : true,  "lite" : false, "value" : ")" + std::to_string(PDD_UID) + R"(" } })";
const std::string EMPTY  = R"({ "uid" : { } })";


yhttp::response bbResponse(const std::vector<std::string>& users) {
    return yhttp::response {
        .status=200,
        .body="{ \"users\" : [" + boost::algorithm::join(users, ", ") + "] }"
    };
}

yhttp::response retriableError() {
    return yhttp::response {
        .status=200,
        .body=R"({ "exception": { "id": 10, "value": "error" } })"
    };
}

yhttp::response fatalError() {
    return yhttp::response {
        .status=200,
        .body=R"({ "exception": { "id": 1, "value": "error" } })"
    };
}

struct BlackboxTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    ResolverConfig config;
    std::vector<Uid> uids = {Uid(1), Uid(2), Uid(3)};
    http_getter::ResponseSequencePtr responses;
    http_getter::TypedClientPtr getter;

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        responses = std::make_shared<StrictMock<http_getter::ResponseSequence>>();
        getter = http_getter::createTypedDummy(responses);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

TEST_F(BlackboxTest, shouldReturnOnlyPDDUids) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(bbResponse({YANDEX, PDD, EMPTY})))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield);
        EXPECT_THAT(result.value(), UnorderedElementsAre(PDD_UID));
    });
}

TEST_F(BlackboxTest, shouldSplitManyUidsOnChunks) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(bbResponse({YANDEX})))
        .WillOnce(Return(bbResponse({EMPTY})))
        .WillOnce(Return(bbResponse({PDD})))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield, 1);
        EXPECT_THAT(result.value(), UnorderedElementsAre(PDD_UID));
    });
}

TEST_F(BlackboxTest, shouldReturnErrorInCaseOfWrongAccountsNubmer) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(bbResponse({YANDEX, PDD})))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield, 1);
        EXPECT_EQ(result.error(), make_error(RemoteServiceError::blackbox));
    });
}

TEST_F(BlackboxTest, shouldReturnFirstError) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(bbResponse({YANDEX})))
        .WillOnce(Return(bbResponse({YANDEX, PDD})))
    ;
    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield, 1);
        EXPECT_EQ(result.error(), make_error(RemoteServiceError::blackbox));
    });
}

TEST_F(BlackboxTest, shouldReturnErrorInCaseOfEmptyResponse) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=200, .body=""}))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield);
        EXPECT_EQ(result.error(), make_error(UnexpectedError::exception));
    });
}

TEST_F(BlackboxTest, shouldReturnErrorInCaseOfEmptyJsonResponse) {
    EXPECT_CALL(*responses, get())
        .WillOnce(Return(yhttp::response {.status=200, .body="{}"}))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        const auto result = pddUids(uids, *getter, config, yield);
        EXPECT_EQ(result.error(), make_error(RemoteServiceError::blackbox));
    });
}

}
