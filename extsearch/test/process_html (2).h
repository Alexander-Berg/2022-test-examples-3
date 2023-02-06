#pragma once

#include <library/cpp/charset/doccodes.h>
#include <library/cpp/langs/langs.h>

#include <util/generic/buffer.h>
#include <util/generic/string.h>

namespace NVideoLib {

    void ProcessHtml(const TString& htmlPath, ECharset enc, TBuffer& numeratorEvents, TBuffer& zone, TBuffer& zoneImg, const TString& url);

    TBuffer ProcessSegmentator(const TBuffer& numeratorEvents, const TBuffer& zone);

} // namespace NVideoLib
