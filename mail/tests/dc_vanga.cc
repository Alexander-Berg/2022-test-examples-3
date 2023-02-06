#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <internal/dc_vanga.h>

#include <exception>
#include <functional>
#include <optional>
#include <stdexcept>

namespace {

using namespace std::literals;

using namespace testing;
using namespace sharpei::db;
using namespace sharpei::dc_vanga;

TEST(DCVangaTests_getHostName, noError) {
    ASSERT_NO_THROW(getHostName());
}

struct Case {
    RuleConfig rule;
    std::string host;
    std::string expectedDC;
};

class DCVangaTests : public TestWithParam<Case> {};

TEST_P(DCVangaTests, simple) {
    const auto& param = GetParam();
    const auto vanga = DCVanga({param.rule});
    ASSERT_EQ(vanga.getHostDC(param.host), param.expectedDC);
}

const auto SharpeiBMRule = RuleConfig{.regex = R"([\w-]+\d+(?P<dcid>[a-z]).mail.yandex.net)",
                                      .mapping = {{.dc = DC::iva, .dcId = "e"},
                                                  {.dc = DC::man, .dcId = "i"},
                                                  {.dc = DC::myt, .dcId = "f"},
                                                  {.dc = DC::sas, .dcId = "h"},
                                                  {.dc = DC::vla, .dcId = "k"}}};

INSTANTIATE_TEST_SUITE_P(
    sharpeiBMHosts, DCVangaTests,
    Values(Case{.rule = SharpeiBMRule, .host = "sharpei01e.mail.yandex.net", .expectedDC = "iva"},
           Case{.rule = SharpeiBMRule, .host = "sharpei01i.mail.yandex.net", .expectedDC = "man"},
           Case{.rule = SharpeiBMRule, .host = "sharpei01f.mail.yandex.net", .expectedDC = "myt"},
           Case{.rule = SharpeiBMRule, .host = "sharpei01h.mail.yandex.net", .expectedDC = "sas"},
           Case{.rule = SharpeiBMRule, .host = "sharpei01k.mail.yandex.net", .expectedDC = "vla"},
           Case{.rule = SharpeiBMRule, .host = "sharpei-test01h.mail.yandex.net", .expectedDC = "sas"},
           Case{.rule = SharpeiBMRule, .host = "corp-sharpei02h.mail.yandex.net", .expectedDC = "sas"}));

const auto SharpeiQLOUDRule = RuleConfig{.regex = R"((?P<dcid>[a-z]{3})\d+-[^.]*.qloud-\w.yandex.net)",
                                         .mapping = {{.dc = DC::iva, .dcId = "iva"},
                                                     {.dc = DC::man, .dcId = "man"},
                                                     {.dc = DC::myt, .dcId = "myt"},
                                                     {.dc = DC::sas, .dcId = "sas"},
                                                     {.dc = DC::vla, .dcId = "vla"}}};

INSTANTIATE_TEST_SUITE_P(
    sharpeiQLOUDHosts, DCVangaTests,
    Values(Case{.rule = SharpeiQLOUDRule, .host = "iva11-36ab097d0932.qloud-c.yandex.net", .expectedDC = "iva"},
           Case{.rule = SharpeiQLOUDRule, .host = "man12-36ab097d0932.qloud-c.yandex.net", .expectedDC = "man"},
           Case{.rule = SharpeiQLOUDRule, .host = "myt13-36ab097d0932.qloud-c.yandex.net", .expectedDC = "myt"},
           Case{.rule = SharpeiQLOUDRule, .host = "sas14-36ab097d0932.qloud-c.yandex.net", .expectedDC = "sas"},
           Case{.rule = SharpeiQLOUDRule, .host = "vla15-36ab097d0932.qloud-c.yandex.net", .expectedDC = "vla"}));

TEST(DCVangaTests, exceptionIfHostMatchesRegexButCaptureGroupNamedDCIDAbsent) {
    const auto rule = RuleConfig{.regex = "(?P<random>.*)"};
    const auto vanga = DCVanga({rule});
    try {
        vanga.getHostDC("hostname");
        FAIL() << "expected exception";
    } catch (std::logic_error& e) {
        ASSERT_EQ(e.what(), "no named capture group with name 'dcid'"s);
    }
}

TEST(DCVangaTests, exceptionIfHostMatchesMoreThanOneRegex) {
    const auto rules = {RuleConfig{.regex = "(?P<dcid>.*)", .mapping = {Mapping{.dc = DC::sas, .dcId = "hostname"}}},
                        RuleConfig{.regex = ".(?P<dcid>.*).", .mapping = {Mapping{.dc = DC::iva, .dcId = "ostnam"}}}};
    const auto vanga = DCVanga(rules);
    try {
        vanga.getHostDC("hostname");
        FAIL() << "expected exception";
    } catch (std::logic_error& e) {
        ASSERT_EQ(e.what(), "hostname matches more than one regex"s);
    }
}

TEST(DCVangaTests, exceptionIfLocalHostNameDoesNotMatchAnyRegex) {
    const auto rule = RuleConfig{.regex = "invalid"};
    const auto vanga = DCVanga({rule});
    try {
        vanga.getLocalhostDC();
        FAIL() << "expected exception";
    } catch (std::logic_error& e) {
        ASSERT_EQ(e.what(), "unable to determine dc for the hostname"s);
    }
}

TEST(DCVangaTests, exceptionIfHostMatchedDCIDNotFound) {
    const auto rules = {RuleConfig{.regex = "(?P<dcid>.*)", .mapping = {Mapping{.dc = DC::sas, .dcId = "blah"}}}};
    const auto vanga = DCVanga(rules);
    try {
        vanga.getHostDC("hostname");
        FAIL() << "expected exception";
    } catch (std::logic_error& e) {
        ASSERT_EQ(e.what(), "no mapping found for the matched dcId"s);
    }
}

}  // namespace
