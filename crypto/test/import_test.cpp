#include <crypta/graph/rtmr/lib/common/get_source_by_ip.h>
#include <crypta/lib/native/ext_fp/constants.h>
#include <crypta/lib/native/resource_service/parsers/extfp_source_parser/extfp_source_parser.h>
#include <crypta/lib/native/string_archive/string_archive.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>

using namespace NCrypta::NResourceService;

namespace {
    struct TSourceTestDesc {
        TString SourceId;
        bool Enabled;
        bool Delayed;
        TString FirstValidIp;
        TString LastValidIp;
        TString NotValidIp;
        TVector<TString> IdsToMatch;
    };

    const NGeobase::TLookup& GetGeoData() {
        static const NGeobase::TLookup geoData(GetWorkPath() + "/geodata6.bin");
        return geoData;
    }

    bool VerifySource(const TSourcesDescriptions& sources, const TSourceTestDesc& sd) {
        using NCrypta::NGraph::GetSourceByIpSafe;

        const auto& it = std::find_if(sources.begin(), sources.end(),
                                      [&sd](const auto& source) {
                                          return (source.GetSource() == sd.SourceId);
                                      });

        UNIT_ASSERT(it != sources.end());

        UNIT_ASSERT(it->IsEnabled() == sd.Enabled);
        UNIT_ASSERT(it->IsDelayed() == sd.Delayed);

        UNIT_ASSERT(it->GetIdsToMatch() == sd.IdsToMatch);

        UNIT_ASSERT(it->IsPassing(sd.FirstValidIp, GetSourceByIpSafe(GetGeoData(), sd.FirstValidIp)));
        UNIT_ASSERT(it->IsPassing(sd.LastValidIp, GetSourceByIpSafe(GetGeoData(), sd.LastValidIp)));
        UNIT_ASSERT(!it->IsPassing(sd.NotValidIp, GetSourceByIpSafe(GetGeoData(), sd.NotValidIp)));

        return true;
    }
}

Y_UNIT_TEST_SUITE(TestImportExtfpDescription) {
    Y_UNIT_TEST(TestImportExtfpDescription) {
        const TString sourcesDescFile{"sources.yaml"};
        auto archive = NCrypta::NStringArchive::Archive({{sourcesDescFile, NResource::Find(sourcesDescFile)}});
        const auto& sources = TExtfpSourceParser::Parse(archive);

        const TVector<TSourceTestDesc> testDescriptions{
            {NCrypta::NExtFp::BEELINE_SOURCE_ID, true, false, "81.9.126.0", "217.118.92.223", "8.8.8.8", {}},
            {NCrypta::NExtFp::ER_TELECOM_SOURCE_ID, true, false, "5.3.62.0", "217.174.164.63", "8.8.8.8", {}},
            {NCrypta::NExtFp::INTENTAI_SOURCE_ID, true, false, "87.241.176.0", "87.241.189.255", "8.8.8.8", {}},
            {NCrypta::NExtFp::MTS_SOURCE_ID, true, true, "5.227.120.0", "2a00:1fa3:7fff:ffff:ffff:ffff:ffff:ffff", "5.3.62.0", {}},
            {NCrypta::NExtFp::ROSTELECOM_SOURCE_ID, true, true, "85.173.209.70", "90.188.78.122", "8.8.8.8", {}},
        };

        UNIT_ASSERT(5 == sources.size());
        for (const auto& source : testDescriptions) {
            UNIT_ASSERT(VerifySource(sources, source));
        }
    }
}
