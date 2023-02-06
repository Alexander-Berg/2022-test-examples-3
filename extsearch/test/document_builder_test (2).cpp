#include "document_builder_test.h"
#include "image_handler_test.h"
#include "process_html.h"

#include <extsearch/video/robot/parsers/html_parser/imagelib/document_builder.h>
#include <extsearch/images/robot/library/io/proto_io.h>

#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <yweb/protos/outlinks/links.pb.h>
#include <yweb/robot/preparat/io/io.h>
#include <util/string/split.h>

namespace NVideoLib {

    static void ProcessDocumentBuilder(const TString& url, const TString& filePath, ECharset encode, TString& result) {
        TBuffer numeratorEvents;
        TBuffer zone;
        TBuffer zoneImg;
        ProcessHtml(filePath, encode, numeratorEvents, zone, zoneImg, url);

        THtProcessor HtProcessor;
        HtProcessor.Configure((ArcadiaSourceRoot() + "/extsearch/images/robot/parsers/html_parser/config/htparser.linktext.ini").data());
        static constexpr bool writeLog = false;
        NVideoLib::Document doc(writeLog);
        TNlpInputDecoder decoder(encode);
        NVideoLib::TDocumentBuilder documentBuilder(url, doc, writeLog, &HtProcessor, &decoder);
        TNumeratorEvents events(numeratorEvents);
        events.Numerate(documentBuilder, zoneImg.Data(), zoneImg.Size());

        TUrl2Props url2props;
        ProcessSegmentator(filePath, encode, url2props, url);
        doc.DoAnalysis(url2props);

        TBuffer imageRes = doc.InsertKiwiResults();

        TOutputLinksData data;
        Y_PROTOBUF_SUPPRESS_NODISCARD data.ParseFromArray(imageRes.Data(), imageRes.Size());

        NPreparat::TTextualReader reader(url, &data.GetText());
        TStringOutput output(result);
        for (const TOutputLinksData::TLink& link : data.GetLink()) {
            output << reader.GetUrl(link.GetOriginalUrlKey()) << "\t";
            output << reader.GetText(link.GetTextKey()) << "\t";

            const NImages::NLinkDB::TLinkAttrsPB& linkAttrs = link.GetImagesLinkAttrs();
            TStringStream linkAttrsOut;
            NImages::NIO::SaveJsonPB(linkAttrsOut, linkAttrs);
            output << TString(linkAttrsOut.Str().data(), linkAttrsOut.Str().size()) << Endl;
        }
    }

    void CompareTestResult(const TString& samplePath, const TString& result, bool isCanonize) {
        if (isCanonize) {
            printf("Canonizing %s...\n", samplePath.data());
            TUnbufferedFileOutput out(samplePath);
            out << result << Endl;
        }

        TVector<TString> current;
        StringSplitter(result).Split('\n').AddTo(&current);

        std::sort(current.begin(), current.end());

        TVector<TString> sample;
        ReadUrl2Props(samplePath, sample);

        for (size_t i = 0; i != sample.size(); ++i) {
            if (sample[i] != current[i]) {
                printf("sample = %s\ncurrent = %s\n\n", sample[i].c_str(), current[i].c_str());
            }
            UNIT_ASSERT_EQUAL(sample[i], current[i]);
        }
    }

    void TestDocumentBuilder(const TString& url, const TString& filePath, ECharset encode, const TString& samplePath, bool isCanonize) {
        TString result;
        ProcessDocumentBuilder(url, filePath, encode, result);
        CompareTestResult(samplePath, result, isCanonize);
    }

}  // namespace NVideoLib
