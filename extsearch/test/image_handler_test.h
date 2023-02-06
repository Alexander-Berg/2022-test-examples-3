#pragma once

#include <extsearch/images/robot/parsers/html_parser/imagelib/image_handler.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>

namespace NImageLib {
    void ReadUrl2Props(const TString& samplePath, TVector<TString>& sample);

    void ProcessSegmentator(const TString& filePath, ECharset encode, TUrl2Props& url2props, const TString& url);

    void CompareTestResult(const TString& samplePath, const TUrl2Props& url2props, bool isCanonize = false);

    void TestImageHandler(const TString& filePath, ECharset encode, const TString& samplePath, const TString& url, bool isCanonize = false);

} // namespace NImageLib
