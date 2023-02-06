#include "test_output.h"

#include <extsearch/video/robot/vt_extractor/lib/quality_record.pb.h>

#include <library/cpp/diff/diff.h>

#include <google/protobuf/text_format.h>

#include <util/charset/unidata.h>
#include <util/charset/wide.h>
#include <util/datetime/cputimer.h>
#include <util/generic/algorithm.h>
#include <util/generic/set.h>
#include <util/generic/string.h>
#include <util/stream/str.h>
#include <util/stream/trace.h>
#include <util/string/subst.h>
#include <library/cpp/string_utils/url/url.h>

#include <algorithm>
#include <iterator>

using namespace NVideo;

namespace {

/**
    @brief  OneVideoEmptyUrlTestCase

    @return true    if test case has only 1 quality record (so there is only one video),
                    and there is no urls for the content.

    @note   Right now only etalons from toloka should be like that. This is not good practice, so
            try to not overuse it.
 */
bool OneVideoEmptyUrlTestCase(const NVtExtractor::TTestCase& testCase) {
    if (testCase.RecordsSize() != 1) {
        return false;
    }

    const NVtExtractor::TQualityRecord& record = testCase.GetRecords(0);
    if (record.UrlsSize()) {
        return false;
    }

    return true;
}


TString SerializeTestCase(const NVtExtractor::TTestCase& testCase) {
    TString res;
    if (!google::protobuf::TextFormat::PrintToString(testCase, &res)) {
        ythrow yexception() << "Failed to print test case to string";
    }
    return res;
}


/**
    @brief  NormalizeUtf8Text   normalizes text that is guaranteed to be a valied utf8 sequence.
 */
TString NormalizeUtf8Text(const TString& text) {
    const TUtf16String wtext = UTF8ToWide(text.data(), text.length());
    TUtf16String normalized;

    /// Leave only alnums and separate then with single space.
    bool startedBlanks = false;
    for (auto it = wtext.begin(); it != wtext.end(); ++it) {
        const auto wch = *it;
        if (IsAlnum(wch)) {
            startedBlanks = false;
            normalized.append(ToLower(wch));
        } else {
            if (!startedBlanks) {
                startedBlanks = true;
                normalized.append(' ');
            }
        }
    }

    /// Trim spaces at the begin and end.
    size_t newBegin = 0;
    while (newBegin != normalized.length() && (IsSpace(normalized[newBegin]) || IsBlank(normalized[newBegin]))) { //!< does IsBlank implies IsSpace?
        ++newBegin;
    }
    size_t newEnd = normalized.length();
    while (newEnd > 0 && (IsSpace(normalized[newEnd - 1]) || IsBlank(normalized[newEnd - 1]))) {
        --newEnd;
    }
    const TUtf16String wCutText = newBegin < newEnd ? normalized.substr(newBegin, newEnd - newBegin) : TUtf16String();

    /// Convert back to utf8.
    size_t written = 0;
    TVector<char> resVector(wCutText.length() * 4 + 1, 0);
    WideToUTF8(wCutText.data(), wCutText.length(), resVector.data(), written);
    const TString res(resVector.data(), written);
    return res;
}


/**
    @brief  NormalizeNonUtf8Text    normalizes text that is in unknown encoding.
 */
TString NormalizeNonUtf8Text(const TString& text) {
    TStringStream sstr;

    size_t idx = 0;
    while (idx < text.size() && IsWhitespace(text[idx])) {
        ++idx;
    }

    /// Merge whitespaces into one space, remove '_' at the end of words.
    char prevChar = idx < text.size() ? text[idx] : 0;
    ++idx;
    while (idx < text.size()) {
        if (IsWhitespace(text[idx])) {
            if (prevChar != '_') {
                sstr << prevChar;
            }
            prevChar = ' ';
            while (idx < text.size() && IsWhitespace(text[idx])) {
                ++idx;
            }
        } else {
            sstr << prevChar;
            prevChar = text[idx];
            ++idx;
        }
    }
    if (prevChar) {
        sstr << prevChar;
    }

    TString res = sstr.Str();

    /// Cut whitespaces at the end.
    idx = res.length();
    while (idx && IsWhitespace(res[idx - 1])) {
        --idx;
    };
    res = res.substr(0, idx);

    if (res.length() && res.back() == '.') {
        res.pop_back();
    }

    SubstGlobal(res, ". .", ".");
    SubstGlobal(res, "–", "-");

    const TString& constRes = res;
    if (AllOf(constRes.begin(), constRes.end(), isspace)) {
        res.clear();
    }

    SubstGlobal(res, "\"", "");
    SubstGlobal(res, "'", "");
    SubstGlobal(res, "«", "");
    SubstGlobal(res, "»", "");
    SubstGlobal(res, "[", "");
    SubstGlobal(res, "]", "");

    return res;
}


/**
    @brief  NormalizeText   normalizes text to be able to compare indexarch and vtextractor outputs.
 */
TString NormalizeText(const TString& text) {
    TString res;
    if (IsUtf(text)) {
        res = NormalizeUtf8Text(text);
    } else  {
        res = NormalizeNonUtf8Text(text);
    }
    return res;
}


/**
    @brief  DiffQr calculates diff on quality records.
 */
TDiff DiffQr(const NVtExtractor::TQualityRecord& result, const NVtExtractor::TQualityRecord& etalon) {
    size_t resultIdx = 0;
    size_t etalonIdx = 0;

    /// Calculate diff.
    TDiff res;
    while (resultIdx < result.TextsSize() && etalonIdx < etalon.TextsSize()) {
        const TString& resultText = result.GetTexts(resultIdx);
        const TString& etalonText = etalon.GetTexts(etalonIdx);

        const int cmp = resultText.compare(etalonText);

        if (cmp < 0) {
            ++res.ResultOnly;
            ++resultIdx;
        } else if (cmp > 0) {
            ++res.EtalonOnly;
            ++etalonIdx;
        } else {
            ++res.Common;
            ++resultIdx;
            ++etalonIdx;
        }
    }

    res.ResultOnly += result.TextsSize() - resultIdx;
    res.EtalonOnly += etalon.TextsSize() - etalonIdx;

    return res;
}


enum ECaseType {
    E_ETALON,
    E_RESULT
};

/**
    @brief  ReadTestCase reads all quality records from string and sorts them by url.
 */
NVtExtractor::TTestCase ReadTestCase(const TString& data, ECaseType type) {
    NVtExtractor::TTestCase res;

    google::protobuf::TextFormat::Parser parser;
    if (!parser.ParseFromString(data, &res)) {
        ythrow yexception() << "Failed to read protobuf: " << data;
    }

    /// Check data.
    bool emptyUrls = false;
    for (const NVtExtractor::TQualityRecord& record : res.GetRecords()) {
        emptyUrls |= record.UrlsSize() == 0;
    }
    if (emptyUrls && (type != E_ETALON || !OneVideoEmptyUrlTestCase(res))) {
        ythrow yexception() << "Found quality record with no urls. This is allowed as exception in only one case, but this isn't it.";
    }

    /// Normalize records.
    for (NVtExtractor::TQualityRecord& record : *res.MutableRecords()) {
        for (TString& text : *record.MutableTexts()) {
            text = NormalizeText(text);
        }
    }

    /// Sort urls and texts in each record.
    for (NVtExtractor::TQualityRecord& record : *res.MutableRecords()) {
        google::protobuf::RepeatedPtrField<TString>& texts = *record.MutableTexts();
        Sort(texts.begin(), texts.end());
        const auto it = Unique(texts.begin(), texts.end());
        const size_t newSize = std::distance(texts.begin(), it);
        texts.Truncate(newSize);

        Sort(
            record.MutableUrls()->begin(),
            record.MutableUrls()->end()
        );
    }

    /// Sort records by urls.
    Sort(
        res.MutableRecords()->begin(),
        res.MutableRecords()->end(),
        [](const NVtExtractor::TQualityRecord& left, const NVtExtractor::TQualityRecord& right) {
            /// Urls in TQualityRecords should be sorted on creation of the structure.
            return std::lexicographical_compare(
                left.GetUrls().begin(),
                left.GetUrls().end(),
                right.GetUrls().begin(),
                right.GetUrls().end()
            );
        }
    );

    return res;
}


NVtExtractor::TQualityRecord FromVideo(const NVtExtractor::TExtractedVideo& video) {
    NVtExtractor::TQualityRecord res;

    /// Urls.
    for (size_t idx = 0; idx != video.ContentsSize(); ++idx) {
        const NVtExtractor::TVideoContent& content = video.GetContents(idx);
        for (size_t uidx = 0; uidx != content.UrlsSize(); ++uidx) {
            const TString& url = content.GetUrls(uidx);
            *res.AddUrls() = url;
        }
    }
    Sort(res.MutableUrls()->begin(), res.MutableUrls()->end());
    Y_ENSURE(res.UrlsSize(), "No urls in extracted video. This is unacceptable.");

    /// Texts.
    for (size_t idx = 0; idx != video.TextsSize(); ++idx) {
        const TString& text = video.GetTexts(idx);
        const TString& normalizedText = NormalizeText(text);
        *res.AddTexts() = normalizedText;
    }

    return res;
}


bool IsVideoSuitable(const NVtExtractor::TExtractedVideo& video) {
    /// Video is not suitable if it has no urls.
    bool has_url = false;
    for (size_t idx = 0; idx != video.ContentsSize(); ++idx) {
        const NVtExtractor::TVideoContent& content = video.GetContents(idx);
        const bool content_has_url = content.UrlsSize() != 0;
        has_url |= content_has_url;
    }
    if (!has_url) {
        return false;
    }

    return true;
}


} // <anonymous> namespace


TString NVideo::QualityTestOutput(const NVtExtractor::TVideosWithTexts& videosWithTexts) {
    TStringStream sstr;

    NVtExtractor::TTestCase testCase;
    for (size_t idx = 0; idx != videosWithTexts.VideosSize(); ++idx) {
        const NVtExtractor::TExtractedVideo& video = videosWithTexts.GetVideos(idx);
        if (!IsVideoSuitable(video)) {
            continue;
        }
        const NVtExtractor::TQualityRecord qualityRec = FromVideo(video);
        *testCase.AddRecords() = qualityRec;
    }

    const TString res = SerializeTestCase(testCase);
    return res;
}


TString QualityTestOutput(const TMap<TString, TVector<TString>>& urlsWithTexts) {
    NVtExtractor::TTestCase testCase;

    for (const auto& urlWithTexts : urlsWithTexts) {
        NVtExtractor::TQualityRecord record;

        *record.AddUrls() = urlWithTexts.first;
        for (const TString& text : urlWithTexts.second) {
            *record.AddTexts() = text;
        }

        *testCase.AddRecords() = std::move(record);
    }


    const TString res = SerializeTestCase(testCase);
    return res;
}


void TDiff::MergeWith(const TDiff& other) noexcept {
    ResultOnly += other.ResultOnly;
    EtalonOnly += other.EtalonOnly;
    Common += other.Common;
}


TDiff NVideo::Diff(const TString& result, const TString& etalon) {
    TVector<NDiff::TChunk<char>> chunks;
    NDiff::InlineDiff(chunks, result, etalon);

    TDiff res;
    for (const NDiff::TChunk<char>& chunk : chunks) {
        res.ResultOnly += chunk.Left.size();
        res.EtalonOnly += chunk.Right.size();
        res.Common += chunk.Common.size();
    }
    Y_ENSURE(res.ResultOnly + res.Common == result.size(), "diff failed on result");
    Y_ENSURE(res.EtalonOnly + res.Common == etalon.size(), "diff failed on etalon");

    return res;
}


TFullDiff NVideo::CompareQualityOutput(const TString& result, const TString& etalon) {
    NVtExtractor::TTestCase resultCase = ReadTestCase(result, E_RESULT);
    NVtExtractor::TTestCase etalonCase = ReadTestCase(etalon, E_ETALON);

    TFullDiff res;
    size_t resultIdx = 0;
    size_t etalonIdx = 0;

    const bool oneVideoNoUrl = OneVideoEmptyUrlTestCase(etalonCase);
    if (oneVideoNoUrl) {
        /// @note: this is special case for toloka etalons. They don't have urls in quality records.
        if (resultCase.RecordsSize() == 1) {
            const NVtExtractor::TQualityRecord& etalonRec = etalonCase.GetRecords(0);
            const NVtExtractor::TQualityRecord& resultRec = resultCase.GetRecords(0);
            const TDiff diff = DiffQr(resultRec, etalonRec);
            res.TextDiff.MergeWith(diff);
            ++res.UrlDiff.Common;

            resultIdx = 1;
            Y_ENSURE(resultIdx == resultCase.RecordsSize(), "bad logic");
        }

        etalonIdx = 1;
        Y_ENSURE(etalonIdx == etalonCase.RecordsSize(), "bad logic");
    }

    while (resultIdx < resultCase.RecordsSize() && etalonIdx < etalonCase.RecordsSize()) {
        const NVtExtractor::TQualityRecord& etalonRec = etalonCase.GetRecords(etalonIdx);
        const NVtExtractor::TQualityRecord& resultRec = resultCase.GetRecords(resultIdx);

        Y_ENSURE(resultRec.UrlsSize(), "result doesn't have urls");

        const int cmp = TString::compare(etalonRec.GetUrls(0), resultRec.GetUrls(0));

        if (cmp < 0) {
            /// skip etalon
            Y_DBGTRACE(INFO, "[INFO] Skipping etalon: " << etalonRec.GetUrls(0));
            res.TextDiff.EtalonOnly += etalonRec.TextsSize();
            ++res.UrlDiff.EtalonOnly;
            ++etalonIdx;
        } else if (cmp > 0) {
            /// skip result.
            Y_DBGTRACE(INFO, "[INFO] Skipping result: " << resultRec.GetUrls(0));
            res.TextDiff.ResultOnly += resultRec.TextsSize();
            ++res.UrlDiff.ResultOnly;
            ++resultIdx;
        } else {
            /// compare etalon and result.
            const TDiff diff = DiffQr(resultRec, etalonRec);
            res.TextDiff.MergeWith(diff);
            ++res.UrlDiff.Common;

            ++resultIdx;
            ++etalonIdx;
        }
    }

    for (; resultIdx < resultCase.RecordsSize(); ++resultIdx) {
        const NVtExtractor::TQualityRecord& resultRec = resultCase.GetRecords(resultIdx);
        Y_DBGTRACE(INFO, "url in result only: " << resultRec.GetUrls(0));
        res.TextDiff.ResultOnly += resultRec.TextsSize();
        ++res.UrlDiff.ResultOnly;
    }

    for (; etalonIdx < etalonCase.RecordsSize(); ++etalonIdx) {
        const NVtExtractor::TQualityRecord& etalonRec = etalonCase.GetRecords(etalonIdx);
        Y_DBGTRACE(INFO, "url in etalon only: " << etalonRec.GetUrls(0));
        res.TextDiff.EtalonOnly += etalonRec.TextsSize();
        ++res.UrlDiff.EtalonOnly;
    }

    return res;
}


TString NVideo::QualityTestOutput(const NMediaDB::TMediaProperties& media) {
    NVtExtractor::TTestCase testCase;

    THashMap<TString, NVtExtractor::TQualityRecord> records;
    for (const NMediaDB::TMediaProperties::TItem& item : media.GetItems()) {
        const TString& videoUrl = item.GetCanonicalUrl().GetValue();

        NVtExtractor::TQualityRecord& record = records[videoUrl];
        *record.AddUrls() = videoUrl;
        for (const NMediaDB::TMediaProperties::TContent& content : item.GetContents()) {
            *record.AddUrls() = content.GetUrl().GetValue();
        }

        for (const NMediaDB::TMediaProperties::TText& text : item.GetTexts()) {
            *record.AddTexts() = text.GetValue();
        }
    }

    for (auto& pair : records) {
        NVtExtractor::TQualityRecord& record = pair.second;

        /// Remove empy urls and duplicate urls if any.
        google::protobuf::RepeatedPtrField<TString>& urls = *record.MutableUrls();
        const auto urlsEmptyEnd = std::remove(urls.begin(), urls.end(), "");
        Sort(urls.begin(), urlsEmptyEnd);
        const auto urlsEndIt = Unique(urls.begin(), urlsEmptyEnd);
        const size_t newUrlsSize = std::distance(urls.begin(), urlsEndIt);
        urls.Truncate(newUrlsSize);

        /// Remove duplicate texts if any.
        google::protobuf::RepeatedPtrField<TString>& texts = *record.MutableTexts();
        Sort(texts.begin(), texts.end());
        const auto textsEndIt = Unique(texts.begin(), texts.end());
        const size_t newTextsSize = std::distance(texts.begin(), textsEndIt);
        texts.Truncate(newTextsSize);

        testCase.AddRecords()->CopyFrom(record);
    }

    const TString res = SerializeTestCase(testCase);
    return res;
}
