
#include <mail/notsolitesrv/src/msearch/types/reflection/response.h>

#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/yajl.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <iostream>

namespace NNotSoLiteSrv::NMSearch {

using boost::fusion::operators::operator==;

static std::ostream& operator<< (std::ostream& s, const TSubscriptionStatusResponse::TSubscription& subscription) {
    return s << yamail::data::serialization::JsonWriter<TSubscriptionStatusResponse::TSubscription>(subscription).result();
}

} // NNotSoLiteSrv::NMSearch


namespace {

using namespace testing;

struct TTestMSearchReflection : Test {
    using ESubscriptionStatus = NNotSoLiteSrv::NMSearch::ESubscriptionStatus;
    using TResponse = NNotSoLiteSrv::NMSearch::TSubscriptionStatusResponse;
};

TEST_F(TTestMSearchReflection, empty_list_in_response_test) {
    auto responseText = R"({"subscriptions" : []})";

    TResponse response;
    yamail::data::deserialization::fromJson(responseText, response);

    ASSERT_TRUE(response.Subscriptions.empty());
}

TEST_F(TTestMSearchReflection, multiple_records_in_response_test) {
    auto responseText = R"({
        "subscriptions" : [
            {
                "uid":      111111,
                "email":    "subscription@ya.ru",
                "status":   "active"
            },
            {
                "uid":      222222,
                "email":    "other-subscription@some-domain.ru",
                "status":   "active"
            },
            {
                "uid":      333333,
                "email":    "other-subscription@some-domain.ru",
                "status":   "hidden"
            }
        ]
    })";

    TResponse response;
    yamail::data::deserialization::fromJson(responseText, response);

    EXPECT_THAT(response.Subscriptions, UnorderedElementsAreArray({
        TResponse::TSubscription
        {
            .Uid = 111111,
            .Email = "subscription@ya.ru",
            .Status = ESubscriptionStatus::active,
        },
        {
            .Uid = 222222,
            .Email = "other-subscription@some-domain.ru",
            .Status = ESubscriptionStatus::active,
        },
        {
            .Uid = 333333,
            .Email = "other-subscription@some-domain.ru",
            .Status = ESubscriptionStatus::hidden,
        }
    }));
}

} // namespace
