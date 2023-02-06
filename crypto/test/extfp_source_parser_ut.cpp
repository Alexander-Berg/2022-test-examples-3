#include <crypta/lib/native/resource_service/parsers/extfp_source_parser/extfp_source_parser.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>

#include <crypta/lib/native/string_archive/string_archive.h>

using namespace NCrypta::NResourceService;

namespace {
    const TSourceDescription* FirstSourceIfPassing(const TSourcesDescriptions& sources, const TString& ip, const TString& sourceId) {
        if (auto it = FindIf(sources.begin(), sources.end(), [&ip, &sourceId](const auto& source) {
                return source.IsPassing(ip, sourceId);
            });
            it != sources.end()) {
            return &(*it);
        } else {
            return nullptr;
        };
    }

    TString FirstSourceIdIfPassing(const TSourcesDescriptions& sources, const TString& ip, const TString& sourceId) {
        auto source = FirstSourceIfPassing(sources, ip, sourceId);
        if (source) {
            return source->GetSource();
        } else {
            return TString{};
        }
    }
}

Y_UNIT_TEST_SUITE(TExtfpSourceParser) {
    using namespace NCrypta::NStringArchive;

    Y_UNIT_TEST(InvalidResources) {
        {
            UNIT_ASSERT_EXCEPTION_CONTAINS(TExtfpSourceParser::Parse(Archive({})), yexception, "No sources were found");
        }

        {
            UNIT_ASSERT_EXCEPTION_CONTAINS(TExtfpSourceParser::Parse(Archive({{}})), yexception, "root is expected to be a map");
        }

        {
            const TString fileData{R"XXX(
            sources:
                unused:
                    ips:
                        - 1.2.3.4 - 1.22.3.4
                        - 1.12.0.0 - 1.17.255.255
            )XXX"};

            UNIT_ASSERT_EXCEPTION_CONTAINS(TExtfpSourceParser::Parse(Archive({{"overlap_ranges.txt", fileData}})), yexception, "Range intersection between 1.2.3.4-1.22.3.4 and 1.12.0.0-1.17.255.255");
        }

        {
            const TString fileData{R"XXX(
            sources:
                unused:
                    ips:
                        - 1.2.3.4 - 6.7.8.9
                        - 3.3.3.3 - 3.3.3.2
            )XXX"};

            UNIT_ASSERT_EXCEPTION_CONTAINS(TExtfpSourceParser::Parse(Archive({{"invalid_range.txt", fileData}})), yexception, "from 3.3.3.3 to 3.3.3.2");
        }
    }

    Y_UNIT_TEST(CheckSingleFile) {
        const TString fileData{R"XXX(
        sources:
            r_telecom:
                ips:
                    - 5.3.0.0 - 5.3.255.255
                    - 5.16.0.0 - 5.19.255.255
                    - 5.164.0.0 - 5.167.255.255
                    - 217.119.80.0 - 217.119.95.255
        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"file.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "5.3.0.0", {}), "r_telecom");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "217.119.95.255", {}), "r_telecom");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "8.8.8.8", {}), TString{});
    }

    Y_UNIT_TEST(CheckTwoFiles) {
        const TString networks1{R"XXX(
        sources:
            networks1:
                ips:
                    - 8.8.8.0 - 8.8.8.255
        )XXX"};

        const TString networks2{R"XXX(
        sources:
            networks2:
                ips:
                    - 1.1.0.0 - 1.1.22.255
        )XXX"};

        auto archiveData = Archive({{"networks1.txt", networks1},
                                    {"networks2.txt", networks2}});
        auto sources = TExtfpSourceParser::Parse(archiveData);

        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "8.8.8.8", {}), "networks1");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.1.2.1", {}), "networks2");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.1.23.0", {}), TString{});
    }

    Y_UNIT_TEST(CheckDisablilty) {
        const TString fileData{R"XXX(
        sources:
            enabled:
                ips:
                    - 1.2.3.0 - 1.2.3.255
            disabled:
                ips:
                    - 1.3.4.0 - 1.3.4.255
                enabled: False
        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"disabled.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.2.3.4", {}), "enabled");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.3.4.5", {}), TString{});
    }

    Y_UNIT_TEST(CheckDelayed) {
        const TString fileData{R"XXX(
        sources:
            enabled:
                ips:
                    - 1.2.3.0 - 1.2.3.255
            disabled:
                ips:
                    - 1.3.4.0 - 1.3.4.255
                delayed: True
        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"delayed.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIfPassing(sources, "1.2.3.4", {})->IsDelayed(), false);
        UNIT_ASSERT_EQUAL(FirstSourceIfPassing(sources, "1.3.4.5", {})->IsDelayed(), true);
    }

    Y_UNIT_TEST(CheckMatchItpOnly) {
        const TString fileData{R"XXX(
        sources:
            match_itp_enabled:
                ips:
                    - 1.2.3.0 - 1.2.3.255

            match_all:
                ips:
                    - 1.3.4.0 - 1.3.4.255
                match_itp_only: False
        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"delayed.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIfPassing(sources, "1.2.3.4", {})->MatchItpOnly(), true);
        UNIT_ASSERT_EQUAL(FirstSourceIfPassing(sources, "1.3.4.5", {})->MatchItpOnly(), false);
    }

    Y_UNIT_TEST(CheckBySource) {
        const TString fileData{R"XXX(
        sources:
            enabled:
                ids_to_match:
                    - source_id1
            disabled:
                ids_to_match:
                    - source_id2
                enabled: False
            several_sources:
                ids_to_match:
                    - source_id11
                    - source_id12
                    - source_id13

        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"delayed.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, {}, TString{"doesnotexist"}), TString{});
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, {}, TString{"source_id1"}), "enabled");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, {}, TString{"source_id2"}), TString{});
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, {}, TString{"source_id12"}), "several_sources");
        UNIT_ASSERT_EQUAL(FirstSourceIfPassing(sources, {}, TString{"source_id13"})->GetIdsToMatch(), TVector<TString>({"source_id11", "source_id12", "source_id13"}));
    }

    Y_UNIT_TEST(CheckBySourceAndIp) {
        const TString fileData{R"XXX(
        sources:
            enabled1:
                ids_to_match:
                    - "matchme with spaces"
            enabled2:
                ids_to_match:
                    - matchmealso
                ips:
                    - 1.2.3.4 - 1.2.3.5
        )XXX"};

        auto sources = TExtfpSourceParser::Parse(Archive({{"delayed.txt", fileData}}));

        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, {}, "matchme with spaces"), "enabled1");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.2.3.4", "noid"), "enabled2");
    }

    Y_UNIT_TEST(Smoke) {
        UNIT_ASSERT_EXCEPTION(MakeSourcesFromResource("empty_file.tgz"), yexception);

        auto sources = MakeSourcesFromResource("smoke.tgz");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "1.2.3.4", {}), "source1");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "2.3.4.5", {}), TString{});
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "3.4.5.6", {}), "source3");
        UNIT_ASSERT_EQUAL(FirstSourceIdIfPassing(sources, "4.5.6.7", {}), TString{});
    }
}
