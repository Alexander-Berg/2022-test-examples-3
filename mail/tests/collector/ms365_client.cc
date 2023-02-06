#include <collector_ng/http/ms365_client.h>
#include <ymod_httpclient/util/url_parser.h>
#include <yplatform/encoding/url_encode.h>
#include <yplatform/util/split.h>
#include <yplatform/util/unique_id.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <algorithm>
#include <regex>

using namespace yrpopper;
using namespace yrpopper::collector;

namespace {

using ::testing::_;
using ::testing::Return;
using ::testing::HasSubstr;
using ::testing::AllOf;
using ::testing::Matches;
using ::testing::ElementsAre;
using ::testing::Eq;
using ::testing::SizeIs;
using ::testing::Field;

using params_map = std::unordered_map<std::string, std::string>;

const std::time_t ANY_TS = 0;

rpop_context_ptr createContext()
{
    auto t = boost::make_shared<task>();
    auto ctx = boost::make_shared<rpop_context>(
        t,
        yplatform::util::make_unique_id(42ull),
        false,
        false,
        std::make_shared<promise_void_t>());
    return ctx;
}

std::string encodeParam(const std::string& key, const std::string& value)
{
    return yhttp::url_encode({ { key, value } }, '\0');
}

time_t ts(const std::string& timeStr)
{
    return string_to_datetime(timeStr);
}

const std::string beginPage = R"(
{
    "@odata.nextLink": "https://graph.microsoft.com/v1.0/users/login/mailFolders/inbox/messages?$top=n&$skip=m",
    "value": [
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T08:00:00Z", "receivedDateTime": "2022-06-01T04:00:00Z"
        },
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T08:00:00Z", "receivedDateTime": "2022-06-01T03:00:00Z"
        },
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T08:00:00Z", "receivedDateTime": "2022-06-01T02:00:00Z"
        },
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T08:00:00Z", "receivedDateTime": "2022-06-01T01:00:00Z"
        },
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T09:00:00Z", "receivedDateTime": "2022-06-01T02:00:00Z"
        }
    ]
}
)";

const std::string endPage = R"(
{
    "value": [
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T09:00:00Z", "receivedDateTime": "2022-06-01T01:00:00Z"
        },
        {
            "id": "", "parentFolderId": "", "internetMessageId": "", "isRead": false, "subject": "",
            "lastModifiedDateTime": "2022-06-02T10:00:00Z", "receivedDateTime": "2022-06-01T01:00:00Z"
        }
    ]
}
)";

yhttp::response createResponse(const std::string& page)
{
    yhttp::response res;
    res.status = 200;
    res.body = page;
    return res;
}

}

class MS365ClientTestable : public MS365Client
{
public:
    MS365ClientTestable() : MS365Client(createContext(), "", "https://graph.microsoft.com/v1.0", 10)
    {
    }

    MOCK_METHOD(
        yhttp::response,
        request,
        (const std::string& url, std::string headers),
        (override));
};

MATCHER_P2(HasParam, key, value, "")
{
    return Matches(HasSubstr(encodeParam(key, value)))(arg);
}

MATCHER_P(LastModifiedDateTimeEq, modifyDateTime, "")
{
    *result_listener << datetime_to_string(arg.lastModifiedDateTime);
    return Matches(Field(&MS365Message::lastModifiedDateTime, Eq(ts(modifyDateTime))))(arg);
}

MATCHER_P(ReceivedDateTimeEq, receivedDateTime, "")
{
    *result_listener << datetime_to_string(arg.receivedDateTime);
    return Matches(Field(&MS365Message::receivedDateTime, Eq(ts(receivedDateTime))))(arg);
}

TEST(MS365Client, fetchMessages_filter_and_order_by_lastModifiedDateTime_and_receivedDateTime)
{
    MS365ClientTestable client;

    auto modify = "2022-06-01T01:00:00Z"s;
    auto received = "2022-06-01T02:00:00Z"s;

    EXPECT_CALL(
        client,
        request(
            AllOf(
                HasParam(
                    "$filter",
                    "(lastModifiedDateTime ge 2022-06-01T01:00:00Z and lastModifiedDateTime lt "
                    "2022-06-01T01:00:01Z and receivedDateTime ge 2022-06-01T02:00:01Z) or "
                    "lastModifiedDateTime ge 2022-06-01T01:00:01Z"),
                HasParam("$orderby", "lastModifiedDateTime,receivedDateTime")),
            _))
        .WillOnce(Return(createResponse(endPage)));

    client.fetchMessages("", ts(modify), ts(received), 100);
}

TEST(MS365Client, fetchMessages_fetched_less_than_count)
{
    MS365ClientTestable client;

    EXPECT_CALL(client, request(_, _)).WillOnce(Return(createResponse(endPage)));

    auto res = client.fetchMessages("", ANY_TS, ANY_TS, 100);
    EXPECT_TRUE(res.size() == 2);
}

TEST(MS365Client, fetchMessages_sort_and_filter_by_receivedDateTime_if_same_lastModifiedDateTime)
{
    MS365ClientTestable client;
    std::size_t count = 3;

    EXPECT_CALL(client, request(_, _)).WillOnce(Return(createResponse(beginPage)));

    auto res = client.fetchMessages("", ANY_TS, ANY_TS, count);

    EXPECT_THAT(res, SizeIs(count));
    EXPECT_THAT(
        res,
        ElementsAre(
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z")));
    EXPECT_THAT(
        res,
        ElementsAre(
            ReceivedDateTimeEq("2022-06-01T01:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T02:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T03:00:00Z")));
}

TEST(MS365Client, fetchMessages_sort_by_receivedDateTime_if_same_lastModifiedDateTime_on_next_page)
{
    MS365ClientTestable client;
    std::size_t count = 5;

    EXPECT_CALL(client, request(_, _))
        .WillOnce(Return(createResponse(beginPage)))
        .WillOnce(Return(createResponse(endPage)));

    auto res = client.fetchMessages("", ANY_TS, ANY_TS, count);

    EXPECT_THAT(res, SizeIs(count));
    EXPECT_THAT(
        res,
        ElementsAre(
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T09:00:00Z")));
    EXPECT_THAT(
        res,
        ElementsAre(
            ReceivedDateTimeEq("2022-06-01T01:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T02:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T03:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T04:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T01:00:00Z")));
}

TEST(MS365Client, fetchMessages_stop_if_lastModifiedDateTime_of_next_message_is_not_same)
{
    MS365ClientTestable client;
    std::size_t count = 4;

    EXPECT_CALL(client, request(_, _)).WillOnce(Return(createResponse(beginPage)));

    auto res = client.fetchMessages("", ANY_TS, ANY_TS, count);

    EXPECT_THAT(res, SizeIs(count));
    EXPECT_THAT(
        res,
        ElementsAre(
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z"),
            LastModifiedDateTimeEq("2022-06-02T08:00:00Z")));
    EXPECT_THAT(
        res,
        ElementsAre(
            ReceivedDateTimeEq("2022-06-01T01:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T02:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T03:00:00Z"),
            ReceivedDateTimeEq("2022-06-01T04:00:00Z")));
}
