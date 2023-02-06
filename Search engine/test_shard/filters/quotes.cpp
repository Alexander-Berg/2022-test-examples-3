#include "quotes.h"

namespace NTestShard {

bool TQuotesFilter::IsPassed(const TAttrRefTree& attrs) {
    TVector<TStringBuf> urlAttrs;
    attrs.TraverseLeaves([&](TStringBuf attr) {
        if (attr.StartsWith("url:")) {
            TStringBuf prefix = TStringBuf("url:\"");
            urlAttrs.push_back(attr.SubStr(prefix.size(), attr.size() - prefix.size() - 1));
        }
    });
    for (TStringBuf urlAttr : urlAttrs) {
        const TVector<TString> BadChars { "\"", "«", "»" };
        for (const TString& s : BadChars) {
            if (urlAttr.find(s) != TStringBuf::npos) {
                return false;
            }
        }
    }
    return true;
}

}
