#include "image_handler_test.h"
#include "process_html.h"

#include <kernel/indexer/face/blob/markup.h>

#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/testing/unittest/registar.h>

namespace NVideoLib {

    void ReadUrl2Props(const TString& samplePath, TVector<TString>& sample) {
        TFileInput file(samplePath);
        TString tmp;
        while (file.ReadLine(tmp))
            sample.push_back(tmp);

        std::sort(sample.begin(), sample.end());
    }

    void ProcessSegmentator(const TString &filePath, ECharset encode, TUrl2Props &url2props, const TString& url) {
        TBuffer numeratorEvents;
        TBuffer zone;
        TBuffer zoneImg;
        ProcessHtml(filePath, encode, numeratorEvents, zone, zoneImg, url);
        TBuffer segmResult = ProcessSegmentator(numeratorEvents, zone);

        TNumeratorEvents events(numeratorEvents);

        TImageHandler img;
        events.Numerate(img, zoneImg.Data(), zoneImg.Size());

        const NIndexerCore::TDirectMarkupHolder extraDirectText(
            NIndexerCore::TDirectMarkupRef(segmResult.Data(), segmResult.Size())
        );

        bool hasMainContent = false;
        img.ProcessSegment(*extraDirectText.GetDirectText(), url2props, hasMainContent);
    }

    void CompareTestResult(const TString& samplePath, const TUrl2Props& url2props, bool isCanonize) {
        TVector<TString> result;
        for (const auto& i : url2props) {
            TStringStream tmp;
            tmp << i.first << "\t";
            for (const auto& j : i.second) {
                THashMap<TString, TString> dump;
                j.DumpProps(dump);
                for (const auto& n : dump) {
                    tmp << n.first << "\t" << n.second << "\t";
                }
            }
            result.push_back(tmp.Str());
        }

        if (isCanonize) {
            TUnbufferedFileOutput out(samplePath);
            for (const auto& text : result)
                out << text << Endl;
        }

        std::sort(result.begin(), result.end());

        TVector<TString> sample;
        ReadUrl2Props(samplePath, sample);

        for (size_t i = 0; i != sample.size(); ++i)
            UNIT_ASSERT_EQUAL(sample[i], result[i]);
    }

    void TestImageHandler(const TString& filePath, ECharset encode, const TString& samplePath, const TString& url, bool isCanonize) {
        TUrl2Props url2props;
        ProcessSegmentator(filePath, encode, url2props, url);
        CompareTestResult(samplePath, url2props, isCanonize);
    }

}  // namespace NVideoLib
