#include "tarc_iterator.h"

namespace NTestShard {

TArcIterator::TArcIterator(THashMap<ui32, TString>&& quotes, bool shouldEscape)
    : ShouldEscape_(shouldEscape)
    , Quotes_(std::move(quotes))
    , It_(Quotes_.begin())
{}

bool TArcIterator::Read(TAttributeWithDocs* doc) {
    if (It_ == Quotes_.end()) {
        return false;
    }

    doc->AddDoc(It_->first);
    if (ShouldEscape_) {
        doc->SetAttribute("\"" + It_->second + "\"");
    } else {
        doc->SetAttribute(It_->second);
    }

    ++It_;

    return true;
}

}
