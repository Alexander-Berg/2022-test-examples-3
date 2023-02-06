#include <market/report/src/redirects/suggest_redirects.h>
#include <library/cpp/testing/unittest/gtest.h>

TEST(TSuggestRedirect, Desktop) {
    {
        const TString response = "[\"звездные войны\","
                                 "[\"звездные войны\",\"звёздные войны игрушки лего\"],"
                                 "["
                                     "\"[{\\\"text\\\": \\\"Звездные войны\\\", \\\"popularity\\\": 1, \\\"link\\\": \\\"\\/franchise\\/14192222?suggest_text=%D0%97%D0%B2%D0%B5%D0%B7%D0%B4%D0%BD%D1%8B%D0%B5%20%D0%B2%D0%BE%D0%B9%D0%BD%D1%8B&suggest=1&suggest_type=franchise\\\", \\\"type\\\": \\\"franchise\\\"}]\","
                                     "\"[{\\\"link\\\": \\\"\\/search?text=звёздные войны игрушки лего&suggest=2&cvredirect=2\\\", \\\"type\\\": \\\"search\\\", \\\"popularity\\\": 2.10000379093e-18}]\""
                                 "],"
                                 "{}]";
        const auto expectedUrl = "/franchise/14192222?suggest_text=%D0%97%D0%B2%D0%B5%D0%B7%D0%B4%D0%BD%D1%8B%D0%B5%20%D0%B2%D0%BE%D0%B9%D0%BD%D1%8B&suggest=1&suggest_type=franchise";
        const auto url = NMarketReport::ExtractUrlForSuggestRedirect("звездные войны", "звездные войны", response, false, false, true, false);
        EXPECT_TRUE(url.Defined());
        EXPECT_EQ(*url, expectedUrl);
    }
}



