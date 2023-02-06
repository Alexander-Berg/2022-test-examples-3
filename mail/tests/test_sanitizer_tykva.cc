#include <gtest/gtest.h>
#include <internal/sanitizer_tykva.h>

namespace {

using namespace testing;

struct TykvaTest : public testing::TestWithParam<std::tuple<std::string, std::string>> {
};

TEST_P(TykvaTest, sanitizer_tykva_with_cases) {
    std::string s = std::get<0>(GetParam());
    msg_body::sanitizer::sanitizeByTykva(s);
    EXPECT_EQ(s, std::get<1>(GetParam()));
}

INSTANTIATE_TEST_SUITE_P(
    sanitizer_tykva_with_cases,
    TykvaTest,
    testing::Values(
        std::make_tuple("", ""), // empty
        std::make_tuple("aa", "aa"), // short string without tags
        std::make_tuple("aaaaaaa", "aaaaaaa"), // long string without tags
        std::make_tuple("a<other>b", "ab"), // unknown tag
        std::make_tuple("a< other attr=val >bc", "abc"), // unknown tag with extra whitespace and attr
        std::make_tuple("aaa<br>bbb1", "aaa<br>bbb1"), // tag from mapping
        std::make_tuple("aaa<br/>bbb2", "aaa<br>bbb2"), // tag from mapping self-closed
        std::make_tuple("aaa<BR/>bbb3", "aaa<br>bbb3"), // case sensitivenessless
        std::make_tuple("aaa<br/>", "aaa<br>"), // tag at the end
        std::make_tuple("<br/>bbb", "<br>bbb"), // tag at the beginning
        std::make_tuple("aaa<br />bbb", "aaa<br>bbb"), // tag with whitespace
        std::make_tuple("aaa<br attr=val />bbb4", "aaa<br>bbb4"), // tag with attributes
        std::make_tuple("<br/", ""), // unclosed tag at the beginning
        std::make_tuple("aaa<br/", "aaa"), // unclosed tag
        std::make_tuple("<br/>", "<br>"), // just a tag with no other chars in string
        std::make_tuple("a<br/>b<br/>c<br/>d", "a<br>b<br>c<br>d"), // multiple tags separated with chars
        std::make_tuple("<br/><br/><br/>", "<br><br><br>"), // multiple tags one after each other
        std::make_tuple("a<p>", "a"), // tag which should be replaced only if is closing is not replaced
        std::make_tuple("a</p>", "a<br>"), // closing tag from mapping
        std::make_tuple("b< / p attr=val>", "b<br>"), // closing tag from mapping with extra whitespace and tags
        std::make_tuple("c</other>", "c "), // unknown closing tag
        std::make_tuple("a</other attr=val>b", "a b"), // unknown closing tag with attr and outer chars
        std::make_tuple("<script>steal cookie</script>", ""), // script tag - delete all inner text
        std::make_tuple("<style>ugly</style>", ""), // style tag - delete all inner text
        std::make_tuple("azaz<script>steal cookie", "azaz"), // script tag - unclosed
        std::make_tuple("azaza<script>steal cookie</scrip", "azaza"), // script tag - unclosed badly
        std::make_tuple("a<script>steal cookie</script>zzz", "azzz"), // script tag - delete all inner text, dont delete outer text
        std::make_tuple("a<script>steal cookie</script>z<script>steal again</script>zzz", "azzzz"), // script tag - delete all inner text, twice
        std::make_tuple("a<script>steal cookie<style>still</style> stealing</script>x", "ax"), // delete-inner-text tags mess
        std::make_tuple("a<other attr=val>b<br>c<hr / >d</other>e<p>f</p>g<script>steal cookie</script>h<style>ugly", "ab<br>c<br>d ef<br>gh") // combined
    )
);

}
