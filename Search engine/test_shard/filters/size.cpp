#include "size.h"

namespace NTestShard {

bool TSizeFilter::IsPassed(const TAttrRefTree& attrs) {
    return attrs.Serialize().size() < MaxSize;
}

}
