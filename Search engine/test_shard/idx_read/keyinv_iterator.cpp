#include "keyinv_iterator.h"

#include <library/cpp/charset/recyr.hh>

namespace NTestShard {

namespace {

TString EscapeAttribute(const TString& attr, const TString& key) {
    return attr + ":\"" + key + "\"";
}

}

TKeyInvIterator::TKeyInvIterator(const TString& attribute, TKeySearcher* keySearcher, THitSearcher* hitSearcher)
    : Attribute_(attribute)
    , SizeLeft_(0)
    , HitSearcher_(hitSearcher)
{
    size_t start = 0;
    size_t end = 0;
    TKeySearcher::TKeyRef currentKeyBuf;
    TKeyData currentRange;
    keySearcher->LowerBound("#" + attribute + "=\"\xff\xff\xff\xff", &currentKeyBuf, &currentRange, &Iterator_, &end);
    keySearcher->LowerBound("#" + attribute + "=\"", &currentKeyBuf, &currentRange, &Iterator_, &start);
    SizeLeft_ = end - start;
}

bool TKeyInvIterator::Read(TAttributeWithDocs* doc) {
    if (SizeLeft_ == 0) {
        return false;
    }
    TKeySearcher::TKeyRef currentKeyBuf;
    TKeyData currentRange;
    THitIterator hitIt;
    Iterator_.ReadKey(&currentKeyBuf, &currentRange);
    HitSearcher_->Seek(currentRange.Start(), currentRange.End(), &hitIt);
    THit hit;
    const size_t offset = Attribute_.size() + TStringBuf("#\"=").size();
    const TString utfQuery = RecodeFromYandex(CODES_UTF8, TString{currentKeyBuf.SubStr(offset)});
    const TString key = EscapeAttribute(Attribute_, utfQuery);
    doc->Clear();
    doc->SetAttribute(key);
    while (hitIt.ReadHit(&hit)) {
        doc->AddDoc(hit.DocId());
    }
    --SizeLeft_;
    return true;
}

size_t LongestCommonPrefix(TWtringBuf lhs, TWtringBuf rhs) {
    size_t len = 0;
    for (; len < Min(rhs.size(), lhs.size()); ++len) {
        if (lhs[len] != rhs[len]) {
            break;
        }
    }
    return len;
}

TMultiKeyInvIterator::TMultiKeyInvIterator(const TString& attribute, TKeySearcher* keySearcher, THitSearcher* hitSearcher, ui32 maxAsteriskDocs, ui32 minPrefixLength)
    : TKeyInvIterator(attribute, keySearcher, hitSearcher)
    , MaxAsteriskDocs_(maxAsteriskDocs)
    , MinPrefixLength_(minPrefixLength)
    , QueryStartPos_(EscapeAttribute(attribute, "").size() - 1)
{}

size_t TMultiKeyInvIterator::LongestCommonQueryPrefix(const TUtf16String& l, const TUtf16String& r) {
    auto chopAttr = [this](const TUtf16String& str) {
        return TWtringBuf(str, GetQueryStart(str));
    };
    return LongestCommonPrefix(chopAttr(l), chopAttr(r));
}

size_t TMultiKeyInvIterator::GetQueryStart(const TUtf16String& str) {
    const TWtringBuf www = u"www.";
    size_t start = QueryStartPos_;
    if (str.substr(start, www.size()) == www) {
        start += www.size();
    }
    return start;
}

void TMultiKeyInvIterator::ResizeToPrefixLength(TUtf16String* str, ui32 prefixLength) {
    size_t start = GetQueryStart(*str);
    str->resize(start + prefixLength);
}

bool TMultiKeyInvIterator::Read(TAttributeWithDocs* docs) {
    Y_ASSERT(docs);

    TUtf16String attribute;
    docs->Clear();
    size_t prefixLength = Max<size_t>();
    if (!LastUnread_.Empty()) {
        attribute = LastUnread_.WideAttribute();
        prefixLength = attribute.size();
        *docs = LastUnread_;
        LastUnread_.Clear();
    }
    while (TKeyInvIterator::Read(&LastUnread_)) {
        TUtf16String last = LastUnread_.WideAttribute();
        if (prefixLength != Max<size_t>()) {
            prefixLength = Min(prefixLength, LongestCommonQueryPrefix(attribute, last));
        } else {
            attribute = last;
            prefixLength = attribute.size();
            *docs = LastUnread_;
            LastUnread_.Clear();
            continue;
        }
        if (prefixLength < MinPrefixLength_) {
            break;
        }
        if (LastUnread_.Size() + docs->Size() > MaxAsteriskDocs_) {
            break;
        }
        if (attribute.size() > prefixLength) {
            ResizeToPrefixLength(&attribute, Max<ui32>(prefixLength, 3));
        }
        for (TDoc doc : LastUnread_.Docs()) {
            docs->AddDoc(doc);
        }
        LastUnread_.Clear();
    }
    if (!docs->Empty()) {
        TString utf8Attribute = WideToUTF8(attribute);
        if (utf8Attribute.back() == '\"') {
            utf8Attribute.pop_back();
        }
        utf8Attribute += "*\"";
        docs->SetAttribute(utf8Attribute);
    }
    return !docs->Empty();
}

}
