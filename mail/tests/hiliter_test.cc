#include <string>
#include <tuple>

#include <gtest/gtest.h>

#include <mail_getter/vdirect/dummy_vdirect.h>

#include <internal/hilight/hilight.h>

namespace {

using namespace testing;
using namespace hiliter;

class HiliterTest : public TestWithParam<std::tuple<std::string, std::string>> {
};

TEST_P(HiliterTest, highligth_email_by_span_tag) {
    const auto [src, expectedResult] = GetParam();

    EmbedInfos el;
    const Params params("1234567890", VdirectPtr(new DummyVdirect()));

    const auto result = hiliter::hilite(src, el, params);

    EXPECT_EQ(expectedResult, result);
}

INSTANTIATE_TEST_SUITE_P(message_body_texts_with_emails_should_be_changed_by, HiliterTest, Values(
    std::make_tuple(R"(user.name@yandex.ru)",                           R"(<span class="wmi-mailto">user.name@yandex.ru</span>)"),
    std::make_tuple(R"(&lt;&nbsp;name@mail.com&nbsp;&gt;)",             R"(&lt;&nbsp;<span class="wmi-mailto">name@mail.com</span>&nbsp;&gt;)"),
    std::make_tuple(R"(<p>Dear user.name@yandex.ru&#xff1a;</p>)",       R"(<p>Dear <span class="wmi-mailto">user.name@yandex.ru</span>&#xff1a;</p>)"),
    std::make_tuple(R"(Пишите на Вася.Пупкин@электронная-почта.рф.)",   R"(Пишите на <span class="wmi-mailto">Вася.Пупкин@электронная-почта.рф</span>.)")
));

INSTANTIATE_TEST_SUITE_P(message_body_texts_without_emails_should_not_be_changed_by, HiliterTest, Values(
    std::make_tuple(R"(no any email@here so left text unchanged)",      R"(no any email@here so left text unchanged)"),
    std::make_tuple(R"([user]@yandex.ru)",                              R"([user]@yandex.ru)"),
    std::make_tuple(R"(user @yandex.ru)",                               R"(user @yandex.ru)"),
    std::make_tuple(R"()",                                              R"()")
));

INSTANTIATE_TEST_SUITE_P(message_body_texts_with_links_should_be_changed_by, HiliterTest, Values(
    std::make_tuple(R"(https://yandex.ru/some-path/&#xfeff;)",          R"(<span class="wmi-link" show="https://yandex.ru/some-path/">https://yandex.ru/some-path/</span>&#xfeff;)"),
    std::make_tuple(R"([http://ya.ru])",                                R"([<span class="wmi-link" show="http://ya.ru">http://ya.ru</span>])")
));

INSTANTIATE_TEST_SUITE_P(message_body_texts_without_links_should_not_be_changed_by, HiliterTest, Values(
    std::make_tuple(R"(no any links//here so left text unchanged)",     R"(no any links//here so left text unchanged)"),
    std::make_tuple(R"()",                                              R"()")
));

} // namespace
