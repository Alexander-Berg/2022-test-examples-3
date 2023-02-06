#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/akita/unistat/cpp/metrics.h>

using namespace ::testing;

namespace akita::metrics {

TEST(MetricTest, shouldPassVersionInctypeLabel) {
    EXPECT_EQ(Metric::fullName("my_cool_version", "metric_name"), "ctype=my_cool_version;metric_name");
}

TEST(MetricTest, shouldThrowAnExceptionInCaseOfStrangeLine) {
    Metric m;

    EXPECT_THROW(m.update({}), std::runtime_error);
    EXPECT_THROW(m.update({{"where_name", ""}}), std::runtime_error);
    EXPECT_THROW(m.update({{"where_name", ""}, {"code", "ok"}}), std::runtime_error);
}

std::map<std::string, std::string> createLine(const std::string& where, const std::string& code, const std::string& reason) {
    return {
        {"where_name", where},
        {"code", code},
        {"reason", reason}
    };
}

TEST(MetricTest, shouldUpdateWithLine) {
    Metric m;

    m.update(createLine("ninja_auth", "2019", "Blagoveshchensk"));

    EXPECT_EQ(m.accum.at("ctype=ninja_auth@blagoveshchensk@;error_code_2019"), 1ull);
}

TEST(MetricTest, shouldReturnAllMetrics) {
    Metric m;

    m.update(createLine("ninja_auth", "2019", "Blagoveshchensk"));
    m.update(createLine("ninja_auth", "2019", "Khabarovsk"));
    m.update(createLine("auth", "2020", "Vladivoskok"));

    EXPECT_THAT(m.get(), UnorderedElementsAre(std::make_tuple("ctype=ninja_auth@blagoveshchensk@;error_code_2019_summ", 1ull),
                                              std::make_tuple("ctype=ninja_auth@khabarovsk@;error_code_2019_summ", 1ull),
                                              std::make_tuple("ctype=auth@vladivoskok@;error_code_2020_summ", 1ull)
                                              ));
}

}
