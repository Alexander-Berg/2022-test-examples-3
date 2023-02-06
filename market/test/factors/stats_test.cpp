#include <market/report/library/stop_words_manager/stop_words_manager.h>

#include <market/report/library/relevance/factors/factors_stats.h>
#include <market/report/library/factors/utils/utils.h>

#include <cmath>

#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/string.h>

namespace NMarketReport {
    namespace NFactors {
        TRequestStats getRequestStats(const TString& request, TStopWordsManager::TConstPtr stopWords) {
            return getRequestStats(TRequest(request), stopWords);
        }

        const double EPS = 0.0001;

        bool operator==(const TRequestStats& a, const TRequestStats& b) {
            return a.words_count == b.words_count &&
                   a.symbols_count == b.symbols_count &&
                   a.nums == b.nums &&
                   a.alphas == b.alphas &&
                   a.cyrs == b.cyrs &&
                   a.lats == b.lats &&
                   a.digs == b.digs &&
                   a.others == b.others;
        }

        bool operator==(const TNameStats& a, const TNameStats& b) {
            return (fabs(a.max_length - b.max_length) < EPS) &&
                   (fabs(a.min_length - b.min_length) < EPS) &&
                   (fabs(a.avg_length - b.avg_length) < EPS) &&
                   (fabs(a.max_length_in_words - b.max_length_in_words) < EPS) &&
                   (fabs(a.min_length_in_words - b.min_length_in_words) < EPS) &&
                   (fabs(a.avg_length_in_words - b.avg_length_in_words) < EPS) &&
                   (fabs(a.common_words_count - b.common_words_count) < EPS);
        }

        bool operator==(const TRequestNameStats& a, const TRequestNameStats& b) {
            return (fabs(a.max_common_words_count - b.max_common_words_count) < EPS) &&
                   (fabs(a.min_common_words_count - b.min_common_words_count) < EPS) &&
                   (fabs(a.avg_common_words_count - b.avg_common_words_count) < EPS) &&
                   (fabs(a.max_common_words_count_in_symb - b.max_common_words_count_in_symb) < EPS) &&
                   (fabs(a.min_common_words_count_in_symb - b.min_common_words_count_in_symb) < EPS) &&
                   (fabs(a.avg_common_words_count_in_symb - b.avg_common_words_count_in_symb) < EPS) &&
                   (fabs(a.high_relev_name_count - b.high_relev_name_count) < EPS) &&
                   (fabs(a.full_request_in_name_count - b.full_request_in_name_count) < EPS);
        }

        bool operator==(const TRequestTitleStats& a, const TRequestTitleStats& b) {
            return a.title_contain_word_from_request_with_quotes ==  b.title_contain_word_from_request_with_quotes &&
                   a.title_contains_request_as_prefix ==  b.title_contains_request_as_prefix &&
                   a.title_words_count ==  b.title_words_count &&
                   a.title_symbols_count ==  b.title_symbols_count &&
                   a.first_position_in_title ==  b.first_position_in_title &&
                   a.last_position_in_title ==  b.last_position_in_title &&
                   a.title_contain_preposition_before_request_word ==  b.title_contain_preposition_before_request_word &&
                   a.title_contain_punctuation_around_request_word ==  b.title_contain_punctuation_around_request_word &&
                   a.words_order ==  b.words_order;
        }

        TEST(Factors, getRequestStats) {
            TStopWordsManager::TPtr stopWords = MakeAtomicShared<TStopWordsManager>();
            {
                TRequestStats original = getRequestStats("", stopWords);
                TRequestStats expected;
                EXPECT_EQ(original, expected);
            }
            {
                TRequestStats original = getRequestStats("qwer", stopWords);
                TRequestStats expected;

                expected.words_count = 1;
                expected.symbols_count = 4;
                expected.alphas = 4;
                expected.lats = 4;

                EXPECT_EQ(original, expected);
            }
            {
                TRequestStats original = getRequestStats("qwe qwe", stopWords);
                TRequestStats expected;

                expected.words_count = 2;
                expected.symbols_count = 6;
                expected.alphas = 6;
                expected.lats = 6;

                EXPECT_EQ(original, expected);
            }
            {
                TRequestStats original = getRequestStats("йцу qwe 123", stopWords);
                TRequestStats expected;

                expected.words_count = 3;
                expected.symbols_count = 9;
                expected.alphas = 6;
                expected.cyrs = 3;
                expected.lats = 3;
                expected.nums = 1;
                expected.digs = 3;

                EXPECT_EQ(original, expected);
            }
            {
                TRequestStats original = getRequestStats(". .", stopWords);
                TRequestStats expected;

                expected.words_count = 2;
                expected.symbols_count = 2;
                expected.others = 2;

                EXPECT_EQ(original, expected);
            }
            {
                TRequestStats original = getRequestStats("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ абвгдеёжзийклмнопрстуфхцчшщъыьэюя", stopWords);
                TRequestStats expected;

                expected.words_count = 2;
                expected.symbols_count = 33+33;
                expected.alphas = 66;
                expected.cyrs = 66;

                EXPECT_EQ(original, expected);
            }

            {
                TRequestStats original = getRequestStats("ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz", stopWords);
                TRequestStats expected;

                expected.words_count = 2;
                expected.symbols_count = 26+26;
                expected.alphas = 52;
                expected.lats = 52;

                EXPECT_EQ(original, expected);
            }

            {
                TRequestStats original = getRequestStats("Ѐ123-Џ456 ѐ789 0 #$%&,.!~`?[]{}()<>;:|^*_", stopWords);
                TRequestStats expected;

                expected.words_count = 4;
                expected.symbols_count = 38;
                expected.digs = 10;
                expected.nums = 4;
                expected.alphas = 3; // Символы Ѐ Џ ѐ
                expected.others = 25; // прочие символы включая -

                EXPECT_EQ(original, expected);
            }

            {
                TRequestStats original = getRequestStats("телевизор samsung BL422-13P 32\" ", stopWords);
                TRequestStats expected;

                expected.words_count = 4;
                expected.symbols_count = 28;
                expected.digs = 7;
                expected.nums = 3;
                expected.alphas = 19;
                expected.cyrs = 9;
                expected.lats = 10;
                expected.others=2;

                EXPECT_EQ(original, expected);
            }
        }

        TEST(Factors, getNameStats) {
            {
                TNameStats original = getNameStats(std::vector<TUtf16String>());
                TNameStats expected;
                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New(u"qwer"));
                TNameStats expected;

                expected.max_length = 4;
                expected.min_length = 4;
                expected.avg_length = 4;
                expected.max_length_in_words = 1;
                expected.min_length_in_words = 1;
                expected.avg_length_in_words = 1;
                expected.common_words_count = 1;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"qwer")
                        (TUtf16String::FromUtf8(("asdf zxxcv")))
                );
                TNameStats expected;

                expected.max_length = 10;
                expected.min_length = 4;
                expected.avg_length = 7;
                expected.max_length_in_words = 2;
                expected.min_length_in_words = 1;
                expected.avg_length_in_words = 1.5;
                expected.common_words_count = 0;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"qwer c")
                        (u"qwer a")
                        (u"qwer b"));
                TNameStats expected;

                expected.max_length = 6;
                expected.min_length = 6;
                expected.avg_length = 6;
                expected.max_length_in_words = 2;
                expected.min_length_in_words = 2;
                expected.avg_length_in_words = 2;
                expected.common_words_count = 1;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"a")
                        (u"b")
                        (u"c")
                        (u"d"));
                TNameStats expected;

                expected.max_length = 1;
                expected.min_length = 1;
                expected.avg_length = 1;
                expected.max_length_in_words = 1;
                expected.min_length_in_words = 1;
                expected.avg_length_in_words = 1;
                expected.common_words_count = 0;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"qwe")
                        (u"")
                        (u"qwe")
                );
                TNameStats expected;

                expected.max_length = 3;
                expected.min_length = 0;
                expected.avg_length = 2;
                expected.max_length_in_words = 1;
                expected.min_length_in_words = 0;
                expected.avg_length_in_words = 2.0 / 3;
                expected.common_words_count = 0;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"qwe")
                        (u"QwE"));
                TNameStats expected;

                expected.max_length = 3;
                expected.min_length = 3;
                expected.avg_length = 3;
                expected.max_length_in_words = 1;
                expected.min_length_in_words = 1;
                expected.avg_length_in_words = 1;
                expected.common_words_count = 1;

                EXPECT_EQ(original, expected);
            }
            {
                TNameStats original = getNameStats(
                        TInplaceVector<TUtf16String>::New
                        (u"йцу")
                        (u"ЙцУ")
                 );
                TNameStats expected;

                expected.max_length = 3;
                expected.min_length = 3;
                expected.avg_length = 3;
                expected.max_length_in_words = 1;
                expected.min_length_in_words = 1;
                expected.avg_length_in_words = 1;
                expected.common_words_count = 1;

                EXPECT_EQ(original, expected);
            }
        }

        TEST(Factors, getRequestTitlesStats) {
            {
                const std::vector<TUtf16String> request_words;
                const std::vector<TUtf16String> titles;
                TRequestNameStats original = getRequestNameStats(request_words, titles);
                TRequestNameStats expected;
                EXPECT_EQ(original, expected);
            }
            {
                const std::vector<TUtf16String> request_words = TInplaceVector<TUtf16String>::New(u"qwer");
                const std::vector<TUtf16String> titles = TInplaceVector<TUtf16String>::New
                                (u"asdf")
                                (u"zxcv");
                TRequestNameStats original = getRequestNameStats(request_words, titles);
                TRequestNameStats expected;
                EXPECT_EQ(original, expected);
            }
            {
                const std::vector<TUtf16String> request_words = TInplaceVector<TUtf16String>::New
                                            (u"qwer")
                                            (u"zxcv");
                const std::vector<TUtf16String> titles = TInplaceVector<TUtf16String>::New
                                             (u"qwer asdf")
                                             (u"qwer zxcv йцук");
                TRequestNameStats original = getRequestNameStats(request_words, titles);
                TRequestNameStats expected;

                expected.max_common_words_count = 2;
                expected.min_common_words_count = 1;
                expected.avg_common_words_count = 1.5;
                expected.max_common_words_count_in_symb = 8;
                expected.min_common_words_count_in_symb = 4;
                expected.avg_common_words_count_in_symb = 6;
                expected.high_relev_name_count = 1;
                expected.full_request_in_name_count = 1;

                EXPECT_EQ(original, expected);
            }
            {
                const std::vector<TUtf16String> request_words = TInplaceVector<TUtf16String>::New(u"qwer");
                const std::vector<TUtf16String> titles = TInplaceVector<TUtf16String>::New
                                                         (u"qwer")
                                                         (u"qwer qwer");
                TRequestNameStats original = getRequestNameStats(request_words, titles);
                TRequestNameStats expected;

                expected.max_common_words_count = 1;
                expected.min_common_words_count = 1;
                expected.avg_common_words_count = 1;
                expected.max_common_words_count_in_symb = 4;
                expected.min_common_words_count_in_symb = 4;
                expected.avg_common_words_count_in_symb = 4;
                expected.high_relev_name_count = 0;
                expected.full_request_in_name_count = 2;

                EXPECT_EQ(original, expected);
            }
        }

        TEST(Factors, getRequestTitleStats) {
            /*
            /// @todo uncomment after establishing libc++ usage
            {
                const TUtf16String request;
                const TUtf16String title;
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.words_order = true;
                expected.title_contains_request_as_prefix = true;
                EXPECT_EQ(original, expected);
            }*/

            {
                const TUtf16String request(u"qwer");
                const TUtf16String title(u"zxcv asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 2;
                expected.title_symbols_count = 8;
                expected.words_order = true;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer");
                const TUtf16String title(u"qwer asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_contains_request_as_prefix = true;
                expected.title_words_count = 2;
                expected.title_symbols_count = 8;
                expected.words_order = true;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer");
                const TUtf16String title(u"zxcv qwer asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 3;
                expected.title_symbols_count = 12;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 1;
                expected.words_order = true;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer asdf");
                const TUtf16String title(u"zxcv qwer asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 3;
                expected.title_symbols_count = 12;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 2;
                expected.words_order = true;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer asdf");
                const TUtf16String title(u"zxcv qwer qwer asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 4;
                expected.title_symbols_count = 16;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 3;
                expected.words_order = true;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer asdf");
                const TUtf16String title(u"zxcv asdf qwer");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 3;
                expected.title_symbols_count = 12;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 2;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer asdf");
                const TUtf16String title(u"zxcv qwer asdf qwer");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 4;
                expected.title_symbols_count = 16;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 3;
                EXPECT_EQ(original, expected);
            }
            {
                const TUtf16String request(u"qwer asdf");
                const TUtf16String title(u"zxcv qwer asdf qwer asdf");
                TRequestTitleStats original = getRequestTitleStats(request, title);
                TRequestTitleStats expected;
                expected.title_words_count = 5;
                expected.title_symbols_count = 20;
                expected.first_position_in_title = 1;
                expected.last_position_in_title = 4;
                EXPECT_EQ(original, expected);
            }
        }
    }
} // namespace NMarketReport::NFactors
