#pragma once

#include <travel/proto/dicts/rasp/timezone.pb.h>

namespace NRasp {
    namespace NDumper {
        NData::TTimeZone CreateTimeZone(i32 id, const TString& code);
    }
}
