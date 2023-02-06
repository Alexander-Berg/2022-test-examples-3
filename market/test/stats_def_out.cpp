#include <market/library/glparams/offer_params/version3.h>
#include <market/report/library/factors/stats_def/stats_def.h>

#include <util/stream/output.h>

template <>
void Out<GuruLightSC3::OfferId>(IOutputStream& out, const GuruLightSC3::OfferId&) {
    out << "implement me";
}

template <>
void Out<Market::TCMagicId>(IOutputStream& out, const Market::TCMagicId&) {
    out << "implement me";
}

template <>
void Out<NMarketReport::NFactors::TRequestStats>(IOutputStream& out, const NMarketReport::NFactors::TRequestStats& stats) {
    out << "{words: " << stats.words_count << ", symbols: " << stats.symbols_count << ", alphas: " << stats.alphas
        << ", cyrs: " << stats.cyrs << ", lats: " << stats.lats << ", digs: " << stats.digs << ", nums: " << stats.nums
        << ", others: " << stats.others << "}";
}

template <>
void Out<NMarketReport::NFactors::TNameStats>(IOutputStream& out, const NMarketReport::NFactors::TNameStats&) {
    out << "implement me";
}

template <>
void Out<NMarketReport::NFactors::TRequestNameStats>(IOutputStream& out, const NMarketReport::NFactors::TRequestNameStats&) {
    out << "implement me";
}

template <>
void Out<NMarketReport::NFactors::TRequestTitleStats>(IOutputStream& out, const NMarketReport::NFactors::TRequestTitleStats& stats) {
    out << "{"
        << stats.title_contain_word_from_request_with_quotes << ", "
        << stats.title_contains_request_as_prefix << ", "
        << stats.title_words_count << ", "
        << stats.title_symbols_count << ", "
        << stats.first_position_in_title << ", "
        << stats.last_position_in_title << ", "
        << stats.title_contain_preposition_before_request_word << ", "
        << stats.title_contain_punctuation_around_request_word << ", "
        << stats.words_order << "}";
}
