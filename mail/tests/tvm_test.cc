#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/http_getter/client/include/tvm.h>
#include <mail/http_getter/client/include/client.h>

#include <iostream>

using namespace testing;
using namespace http_getter::operators;

std::ostream& operator<<(std::ostream& out, const boost::optional<std::vector<std::string>>& opt) {
    if (opt) {
        for (const auto& s: *opt) {
            out << s << " ";
        }
    } else {
        out << "none";
    }

    return out;
}

namespace http_getter::tests {

Endpoint endpoint(const std::string& tvmName) {
    return Endpoint({
        .url = "url",
        .method = "/method",
        .tvm_service = tvmName,
        .tries = 1
    });
}

const std::string serviceTicket = "ticket";
const std::string userTicket = "user";

struct TestableTvmManager: public TvmManager {
    using TvmManager::tickets;

    TestableTvmManager(TvmManager::Options options)
        : TvmManager(std::move(options))
    { }
};

struct TvmTest: public ::testing::Test {
    TvmTest()
        : ::testing::Test()
        , logger(getHttpLogger("", "", ""))
    { }

    virtual ~TvmTest() { }

    void SetUp() override {
        TvmManager::Options opts;
        opts["all_true"]  = TvmManager::Option{.service=true,  .user=true};
        opts["all_false"] = TvmManager::Option{.service=false, .user=false};
        opts["user_only"] = TvmManager::Option{.service=false, .user=true};
        tvm = std::make_shared<TestableTvmManager>(opts);
    }

    std::shared_ptr<TestableTvmManager> tvm;
    Logger logger;
};

struct ListOfServicesWithServiceTicketTest: public TvmTest { };
struct UpdateServiceTicketTest: public TvmTest { };
struct GetAllTicketsTest: public TvmTest { };
struct RequiredHeadersTest: public TvmTest { };
struct HttpAndTvmTest: public TvmTest { };

TEST_F(ListOfServicesWithServiceTicketTest, shouldReturnServicesWithServiceTicketOnly) {
    EXPECT_THAT(tvm->tvm2ServicesWithServiceTicket(), UnorderedElementsAre("all_true"));
}

TEST_F(UpdateServiceTicketTest, shouldUpdateTicketForServiceInList) {
    tvm->updateTicket("all_true", serviceTicket);

    EXPECT_EQ(tvm->tickets()["all_true"], serviceTicket);
}

TEST_F(UpdateServiceTicketTest, shouldNotUpdateTicketWithoutTvm2Option) {
    tvm->updateTicket("user_only", serviceTicket);
    tvm->updateTicket("all_false", serviceTicket);

    EXPECT_EQ(tvm->tickets().count("user_only"), 0ul);
    EXPECT_EQ(tvm->tickets().count("all_false"), 0ul);
}

TEST_F(GetAllTicketsTest, shouldReturnTickets) {
    tvm->updateTicket("all_true", serviceTicket);
    auto ticket = tvm->tickets(userTicket);

    EXPECT_EQ(*ticket->service("all_true"), serviceTicket);
    EXPECT_EQ(*ticket->user("all_true"), userTicket);

    EXPECT_EQ(ticket->service("all_false"), std::nullopt);
    EXPECT_EQ(ticket->user("all_false"), std::nullopt);

    EXPECT_EQ(ticket->service("user_only"), std::nullopt);
    EXPECT_EQ(*ticket->user("user_only"), userTicket);
}

void AsyncRunImpl(http_getter::Request, http_getter::CallbackType) { }

TEST_F(HttpAndTvmTest, shouldAddRequiredHeadersToRequest) {
    tvm->updateTicket("all_true", serviceTicket);

    http::headers h;
    h.add("x-header1", "one");
    h.add("x-header2", "two");

    Client http(tvm->tickets(userTicket), h, &AsyncRunImpl, nullptr);

    Endpoint e = endpoint("all_true");

    http_getter::Request req = http.toGET(e).primary();
    auto headers = std::get<std::string>(req.request.headers);

    EXPECT_THAT(headers, HasSubstr("x-header1: one"));
    EXPECT_THAT(headers, HasSubstr("x-header2: two"));
}

TEST_F(RequiredHeadersTest, shouldAddRequiredHeadersToRequestWithOtherHeaders) {
    tvm->updateTicket("all_true", serviceTicket);

    http::headers h;
    h.add("x-header1", "one");
    h.add("x-header2", "two");

    Client http(tvm->tickets(userTicket), h, &AsyncRunImpl, nullptr);

    Endpoint e = endpoint("all_true");

    http_getter::Request req = http.toGET(e).headers("x-header3"_hdr="three").primary();
    auto headers = std::get<std::string>(req.request.headers);

    EXPECT_THAT(headers, HasSubstr("x-header1: one"));
    EXPECT_THAT(headers, HasSubstr("x-header2: two"));
    EXPECT_THAT(headers, HasSubstr("x-header3: three"));
}

TEST_F(HttpAndTvmTest, shouldPassTicketsToRequest) {
    tvm->updateTicket("all_true", serviceTicket);
    {
        auto builder = toGET(endpoint("all_true"));
        helpers::setTickets(builder, "all_true", tvm->tickets(userTicket));
        const std::string headers = std::get<std::string>(builder.primary().request.headers);

        EXPECT_THAT(headers, HasSubstr("X-Ya-Service-Ticket: ticket"));
        EXPECT_THAT(headers, HasSubstr("X-Ya-User-Ticket: user"));
    }

    {
        auto builder = toGET(endpoint("all_false"));
        helpers::setTickets(builder, "all_false", tvm->tickets(userTicket));
        const std::string headers = std::get<std::string>(builder.primary().request.headers);

        EXPECT_EQ(headers, "");
    }

    {
        auto builder = toGET(endpoint("user_only"));
        helpers::setTickets(builder, "user_only", tvm->tickets(userTicket));
        const std::string headers = std::get<std::string>(builder.primary().request.headers);

        EXPECT_THAT(headers, HasSubstr("X-Ya-User-Ticket: user"));
        EXPECT_THAT(headers, Not(HasSubstr("X-Ya-Service-Ticket")));
    }
}

}
