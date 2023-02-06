#include "tarc_reader.h"
#include "tarc_iterator.h"

#include <util/random/random.h>
#include <library/cpp/deprecated/split/split_iterator.h>
#include <util/string/vector.h>
#include <util/string/split.h>

namespace NTestShard {

template<typename TCharType>
bool IsQuote(TCharType ch) {
    return ch == '\'';
}

TVector<TString> TTokenizer::operator()(TWtringBuf wstr){
    TVector<TString> result;
    size_t beg = 0;
    size_t end = 0;
    auto push = [&] {
        if (end != beg) {
            result.push_back(WideToUTF8(TWtringBuf(wstr.data() + beg, end - beg)));
        }
    };
    for (size_t i = 0; i < wstr.size(); ++i) {
        auto ch = wstr[i];
        if (IsCommonDigit(ch) || IsAlpha(ch) || IsDigit(ch) || IsQuote(ch)) {
            end = i + 1;
        } else {
            push();
            end = i + 1;
            beg = i + 1;
        }
    }
    push();
    return result;
}

TArcQuoteReader::TArcQuoteReader(ui32 minWords, ui32 maxWords)
    : MinWords_(minWords)
    , MaxWords_(maxWords)
{}

void TArcQuoteReader::SetDocId(ui32 docId) {
    DocId_ = docId;
}

void TArcQuoteReader::WriteString(const TUtf16String& w, size_t) {
    TUtf16String wlineStr = w;
    wlineStr.to_lower();
    TWtringBuf raw(w.data(), w.size());
    if (raw.EndsWith('\n')) {
        raw.Chop(1);
    }
    TWtringBuf wline(wlineStr.data(), wlineStr.size());
    TVector<TString> words = Tokenizer_(wline);
    for (const TString& word : words) {
        LocalVocab_.insert(word);
    }

    if (words.size() >= MinWords_ && words.size() <= MaxWords_) {
        TString sent = WideToUTF8(raw);
        if (Sents_.contains(sent)) {
            Sents_[sent] = Max<ui32>();
        } else {
            Y_ENSURE(DocId_ != Max<ui32>());
            Sents_[sent] = DocId_;
        }
    }
}

void TArcQuoteReader::Flush() {
    ++DocsProcessed_;
    for (const TString& word : LocalVocab_) {
        ++GlobalVocab_[word];
    }
    LocalVocab_.clear();
}

THolder<IIndexIterator> TArcQuoteReader::Finish() {
    THashMap<ui32, TString> quotes;
    THashMap<ui32, double> hardness;
    for (const auto& it : Sents_) {
        const TString& quote = it.first;
        const ui32 docId = it.second;
        if (docId == Max<ui32>()) {
            continue;
        }
        TVector<TString> words;
        StringSplitter(quote).Split(' ').AddTo(&words);
        ui32 num = 0;
        double accum = 0.0;
        for (const TString& word : words) {
            double freq = double(DocsProcessed_) / double(GlobalVocab_[word]);
            ++num;
            accum += freq;
        }
        double curHardness = accum / num;
        if (!hardness.contains(docId) || hardness[docId] > curHardness) {
            quotes[docId] = quote;
            hardness[docId] = curHardness;
        }
    }
    return MakeHolder<TArcIterator>(std::move(quotes), true);
}

void TArcWordReader::SetDocId(ui32 docId) {
    DocId_ = docId;
}

void TArcWordReader::WriteString(const TUtf16String& w, size_t) {
    if (SkipSameDocIds_) {
        return;
    }
    TUtf16String wlineStr = w;
    wlineStr.to_lower();
    TWtringBuf raw(w.data(), w.size());
    if (raw.EndsWith('\n')) {
        raw.Chop(1);
    }
    TWtringBuf wline(wlineStr.data(), wlineStr.size());
    TVector<TString> words = Tokenizer_(wline);

    if (!words.empty()) {
        size_t i = RandomNumber(words.size());
        Words_[DocId_] = words[i];
        SkipSameDocIds_ = true;
    }
}

void TArcWordReader::Flush() {
    ++DocsProcessed_;
    SkipSameDocIds_ = false;
}

THolder<IIndexIterator> TArcWordReader::Finish() {
    return MakeHolder<TArcIterator>(std::move(Words_), false);
}

}
