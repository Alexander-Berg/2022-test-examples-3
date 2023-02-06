#pragma once

#include "tarc_reader.h"
#include "tarc_iterator.h"

#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/generic/hash.h>

#include <kernel/tarc/markup_zones/arcreader.h>

namespace NTestShard {

class TTokenizer {
public:
    TVector<TString> operator()(TWtringBuf wstr);
};

class TArcQuoteReader : public IDocTextOutput {
public:
    TArcQuoteReader(ui32 minWords, ui32 maxWords);

    void SetDocId(ui32 docId);
    void WriteString(const TUtf16String& w, size_t) override;
    void Flush();
    THolder<IIndexIterator> Finish();

private:
    TTokenizer Tokenizer_;
    THashSet<TString> LocalVocab_;
    THashMap<TString, ui32> GlobalVocab_;
    THashMap<TString, ui32> Sents_;
    ui32 DocId_ = Max<ui32>();
    ui32 DocsProcessed_ = 0;

    ui32 MinWords_ = 0;
    ui32 MaxWords_ = 0;
};

class TArcWordReader : public IDocTextOutput {
public:
    TArcWordReader() = default;
    TArcWordReader(ui32, ui32) {}

    void SetDocId(ui32 docId);
    void WriteString(const TUtf16String& w, size_t) override;
    void Flush();
    THolder<IIndexIterator> Finish();

private:
    TTokenizer Tokenizer_;

    THashMap<ui32, TString> Words_;

    ui32 DocId_ = Max<ui32>();
    ui32 DocsProcessed_ = 0;

    bool SkipSameDocIds_ = false;
};

}
