#include "fabrics.h"

using namespace NRasp::NData;

namespace NRasp {
    namespace NDumper {
        TTimeZone CreateTimeZone(i32 id, const TString& code) {
            TTimeZone t;
            t.SetId(id);
            t.SetCode(code);
            return t;
        }
    }
}
