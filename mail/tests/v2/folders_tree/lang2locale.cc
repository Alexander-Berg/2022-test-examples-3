#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/folders_tree/method.h>

namespace {

namespace folders = hound::server::handlers::v2::folders_tree;
using namespace ::testing;

class Lang2LocaleTest : public ::testing::TestWithParam<std::pair<std::string, std::string>> { };
INSTANTIATE_TEST_SUITE_P(test_lang2locale_with_all_supported_languages, Lang2LocaleTest, ::testing::Values(
            std::make_pair("az", "az_AZ.UTF-8"),
            std::make_pair("be", "be_BY.UTF-8"),
            std::make_pair("en", "en_US.UTF-8"),
            std::make_pair("hy", "hy_AM.UTF-8"),
            std::make_pair("ka", "ka_GE.UTF-8"),
            std::make_pair("kk", "kk_KZ.UTF-8"),
            std::make_pair("ro", "ro_RO.UTF-8"),
            std::make_pair("ru", "ru_RU.UTF-8"),
            std::make_pair("tr", "tr_TR.UTF-8"),
            std::make_pair("tt", "tt_RU.UTF-8"),
            std::make_pair("uk", "uk_UA.UTF-8")
));

TEST_P(Lang2LocaleTest, should_return_locale_for_lang) {
    const auto [lang, localeName] = GetParam();
    boost::locale::generator gen;
    const std::locale expected = gen(localeName);
    const std::locale actual = folders::lang2locale(lang).value();
    EXPECT_EQ(expected.name(), actual.name());
}

TEST(lang2locale, should_return_error_code_for_unsupported_lang) {
    EXPECT_EQ(folders::lang2locale("co").error(),
            folders::error_code{folders::error::invalidArgument});
}

} // namespace

