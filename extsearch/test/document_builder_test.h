#pragma once

#include <library/cpp/charset/doccodes.h>
#include <library/cpp/langs/langs.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>

namespace NImageLib {
    void TestDocumentBuilder(const TString& url, const TString& filePath, ECharset encode, const TString& samplePath, bool isCanonize = false);

    void CompareTestResult(const TString& samplePath, const TString& result, bool isCanonize);

} // namespace NImageLib
