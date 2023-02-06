#include <mail/http_getter/client/mock/mock.h>


using namespace testing;

namespace http_getter::tests {

using GetRequest = http_getter::RequestBuilder<http_getter::Method::Get, false>;

struct HttpIntegrationTest: public ::testing::Test {
    void SetUp() override {
        tries = 3;
        endpoint = createDummy()->toGET(Endpoint::Data {
            .url = "url",
            .method = "/method",
            .tries = tries
        });
    }

    void setFallbackEndpoint() {
        tries = 2;
        endpoint = createDummy()->toGET(Endpoint::Data {
            .url = "url",
            .fallback = "fallback",
            .method = "/method",
            .tries = tries,
        });
    }

    GetRequest endpoint = http_getter::get("");
    unsigned tries = 0;
    std::string fullUrl = "url/method";
    std::string fullFallback = "fallback/method";
};

YREFLECTION_DEFINE_ENUM_INLINE(HttpServiceName,
    some_http_service
)

TEST_F(HttpIntegrationTest, shouldRetryNotMoreThanMaxTries) {
    unsigned count = 0;
    createDummy()
        ->req(endpoint)
        ->backgroundCall(HttpServiceName::some_http_service, [&](auto) {
            count++;
            return Result::retry;
        });
    EXPECT_EQ(count, tries);
}

TEST_F(HttpIntegrationTest, shouldNotRetryInCaseOfSpecialValue) {
    {
        int count = 0;
        createDummy()
            ->req(endpoint)
            ->backgroundCall(HttpServiceName::some_http_service, [&](auto) {
                count++;
                return Result::fail;
            });
        EXPECT_EQ(count, 1);
    }

    {
        int count = 0;
        createDummy()
                ->req(endpoint)
                ->backgroundCall(HttpServiceName::some_http_service, [&](auto) {
            return count++ == 0 ? Result::retry : Result::fail;
        });
        EXPECT_EQ(count, 2);
    }
}

TEST_F(HttpIntegrationTest, shouldNotRetryInCaseOfSuccess) {
    {
        int count = 0;
        createDummy()
            ->req(endpoint)
            ->backgroundCall(HttpServiceName::some_http_service, [&](auto) {
                count++;
                return Result::success;
            });
        EXPECT_EQ(count, 1);
    }

    {
        int count = 0;
        createDummy()
                ->req(endpoint)
                ->backgroundCall(HttpServiceName::some_http_service, [&](auto) {
            return count++ == 0 ? Result::retry : Result::success;
        });
        EXPECT_EQ(count, 2);
    }
}

TEST_F(HttpIntegrationTest, shouldRetryInCaseOfHandlerException) {
    unsigned count = 0;
    createDummy()
        ->req(endpoint)
        ->backgroundCall(HttpServiceName::some_http_service, [&](auto) -> Result {
            count++;
            throw std::runtime_error("");
        });
    EXPECT_EQ(count, tries);
}

TEST_F(HttpIntegrationTest, shouldPassAnExceptionIfTypeDoesNotDerivedFromStdException) {
    EXPECT_THROW(createDummy()->req(endpoint)->backgroundCall(HttpServiceName::some_http_service, [](auto) -> Result {
        throw 5;
    }), int);

    EXPECT_THROW(createDummy()->req(endpoint)->backgroundCall(HttpServiceName::some_http_service, [](auto) -> Result {
        throw boost::coroutines::detail::forced_unwind();
    }), boost::coroutines::detail::forced_unwind);
}

TEST_F(HttpIntegrationTest, shouldChangeUrlInCaseOfExistingFallback) {
    setFallbackEndpoint();
    int count = 0;

    createDummy([&](http_getter::Request req, http_getter::CallbackType cb) {
        if (count++ == 0) {
            EXPECT_EQ(req.request.url, fullUrl);
        } else {
            EXPECT_EQ(req.request.url, fullFallback);
        }
        cb(boost::system::error_code(), yhttp::response());
    })->req(endpoint)->backgroundCall(HttpServiceName::some_http_service, [](auto) -> Result {
        return Result::retry;
    });
    EXPECT_EQ(count, 2);
}

}
