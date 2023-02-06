#include "test_jsons.h"

#include <library/cpp/archive/yarchive.h>

#include <util/generic/ptr.h>
#include <util/generic/strbuf.h>
#include <util/memory/blob.h>
#include <util/stream/input.h>
#include <util/system/types.h>

extern "C" {
    extern const ui8 TEST_OBJECT_JSON_DATA[];
    extern const ui32 TEST_OBJECT_JSON_DATASize;
};

static THolder<IInputStream> GetTestObjectJSONStream() {
    TArchiveReader reader{TBlob::NoCopy(TEST_OBJECT_JSON_DATA, TEST_OBJECT_JSON_DATASize)};
    return reader.ObjectByKey("/test_object.json");
}

TString GetTestObjectJSON() {
    return GetTestObjectJSONStream()->ReadAll();
}
