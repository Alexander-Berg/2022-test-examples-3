#include "spaces.h"

namespace NTestShard {

bool TSpacesFilter::IsPassed(const TAttrRefTree& attrs) {
    const TString doubleSpaces = "  ";
    if (attrs.Serialize().find(doubleSpaces) != TString::npos) {
        return false;
    } else {
        return true;
    }
}

}
