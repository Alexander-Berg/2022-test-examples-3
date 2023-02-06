#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/sendbernar/unistat/cpp/metrics.h>

using namespace ::testing;

namespace sendbernar::metrics {

TEST(MetricTest, shouldPassVersionInTierLabel) {
    EXPECT_EQ(Metric::fullName("my_cool_version", "metric_name"), "ctype=my_cool_version;metric_name");
}

TEST(MetricTest, shouldThrowAnExceptionInCaseOfStrangeLine) {
    Metric m;

    EXPECT_THROW(m.update({}), std::runtime_error);
    EXPECT_THROW(m.update({{"type", ""}}), std::runtime_error);
    EXPECT_THROW(m.update({{"type", "SmtpGate"}, {"value", "ok"}}), std::runtime_error);
}

template<class Signal>
std::map<std::string, std::string> createLine(const Signal& req) {
    const auto seq = serialize(req);

    return {
        {"type", std::string(logdog::value(std::get<0>(seq)))},
        {"value", std::string(logdog::value(std::get<1>(seq)))},
        {"version", std::string("ver")}
    };
}

TEST(MetricTest, shouldUpdateWithServiceRequest) {
    Metric m;

    m.update(createLine(ServiceRequest{
        "blackbox",
        http_getter::RequestStatus::success,
        {
            ServiceRequest::HttpCall(500, 10.0),
            ServiceRequest::HttpCall(200, 1.0),
        }
    }));

    EXPECT_EQ(m.accum.at("ctype=ver;blackbox_success"), 1ull);
    EXPECT_EQ(m.accum.at("ctype=ver;blackbox_try_0_fail"), 1ull);
    EXPECT_EQ(m.accum.at("ctype=ver;blackbox_try_1_success"), 1ull);
}

TEST(MetricTest, shouldUpdateWithErrorCode) {
    Metric m;

    m.update(createLine(ErrorCode{
        "sendbernar",
        1,
        "message"
    }));
    m.update(createLine(ErrorCode{
        "mail_send",
        1,
        "message"
    }));
    EXPECT_EQ(m.accum.at("ctype=ver;sendbernar_message_1"), 1ull);
    EXPECT_EQ(m.accum.at("ctype=ver;mail_send_message_1"), 1ull);
}

TEST(MetricTest, shouldNotUpdateWithNonSendbernarOrMailSendError) {
    Metric m;

    m.update(createLine(SmtpGate{
        DeliveryResult::spam
    }));
    EXPECT_EQ(m.accum.at("ctype=ver;spam"), 1ull);
}

TEST(MetricTest, shouldReturnAllMetrics) {
    Metric m;

    m.update(createLine(SmtpGate{
        DeliveryResult::spam
    }));
    m.update(createLine(ErrorCode{
        "sendbernar",
        1,
        "message"
    }));
    m.update(createLine(ErrorCode{
        "mail_send",
        1,
        "message"
    }));
    m.update(createLine(ServiceRequest{
        "blackbox",
        http_getter::RequestStatus::success,
        {
            ServiceRequest::HttpCall(500, 10.0),
            ServiceRequest::HttpCall(200, 1.0),
        }
    }));
    EXPECT_THAT(m.get(), UnorderedElementsAre(std::make_tuple("ctype=ver;sendbernar_message_1_summ", 1ull),
                                              std::make_tuple("ctype=ver;mail_send_message_1_summ", 1ull),
                                              std::make_tuple("ctype=ver;blackbox_success_summ", 1ull),
                                              std::make_tuple("ctype=ver;blackbox_try_0_fail_summ", 1ull),
                                              std::make_tuple("ctype=ver;blackbox_try_1_success_summ", 1ull),
                                              std::make_tuple("ctype=ver;spam_summ", 1ull)));
}

}
