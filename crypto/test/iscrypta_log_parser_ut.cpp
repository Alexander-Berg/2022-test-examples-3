#include <crypta/lib/native/iscrypta_log_parser/iscrypta_log_fields.h>
#include <crypta/lib/native/iscrypta_log_parser/iscrypta_log_parser.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NIscryptaLogParser;
using namespace NCrypta::NIscryptaLogFields;

static const TString VALID_UPLOAD_REQUEST = "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: true track_back_reference: false }}";

static const TString VALID_IDENTIFY_REQUEST = "header { type: IDENTIFY version: VERSION_4_10 tag: \"1111\" } [iscrypta.identify.RequestBody.ext] { type: WITH_INFO inspect: false ext_id: \"tag.external_id\" }";

Y_UNIT_TEST_SUITE(Upload) {
    Y_UNIT_TEST(HasExtIdUploadRequest) {
        UNIT_ASSERT(HasExtIdUploadRequest(NYT::TNode()(REQUEST, VALID_UPLOAD_REQUEST)));
        UNIT_ASSERT(!HasExtIdUploadRequest(NYT::TNode::CreateMap()));
        UNIT_ASSERT(!HasExtIdUploadRequest(NYT::TNode()(REQUEST, NYT::TNode::CreateEntity())));
        UNIT_ASSERT(!HasExtIdUploadRequest(NYT::TNode()(REQUEST, TYPE_UPLOAD)));
        UNIT_ASSERT(!HasExtIdUploadRequest(NYT::TNode()(REQUEST, TYPE_EXT_ID)));
    }

    Y_UNIT_TEST(MissingRequestField) {
        const auto& row = NYT::TNode()(UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION(ParseUploadInfo(row), yexception);
    }

    Y_UNIT_TEST(MissingUnixtimeField) {
        const auto& row = NYT::TNode()(REQUEST, VALID_UPLOAD_REQUEST);
        UNIT_ASSERT_EXCEPTION(ParseUploadInfo(row), yexception);
    }

    Y_UNIT_TEST(NotUploadType) {
        const auto& request = "header { type: XXX version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: true track_back_reference: false }";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseUploadInfo(row),
            yexception,
            "TYPE_UPLOAD is missing in header"
        );
    }

    Y_UNIT_TEST(NotExtIdType) {
        const auto& request = "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: XXX ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: true track_back_reference: false }";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseUploadInfo(row),
            yexception,
            "TYPE_EXT_ID is missing in body"
        );
    }

    Y_UNIT_TEST(Incomplete) {
        const auto& row = NYT::TNode()
            (REQUEST, "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID")
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ParseUploadInfo(row), yexception, "Can't find body boundaries");
    }

    Y_UNIT_TEST(IncompleteBracket) {
        const auto& row = NYT::TNode()
            (REQUEST, "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: true track_back_reference: false }")
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ParseUploadInfo(row), yexception, "Can't find end of message with external id");
    }

    Y_UNIT_TEST(Invalid) {
        const auto& row = NYT::TNode()
            (REQUEST, "header } type: UPLOAD version: VERSION_4_10 { type: EXT_ID")
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ParseUploadInfo(row), yexception, "Can't find header boundaries");
    }

    Y_UNIT_TEST(Positive) {
        const auto& row = NYT::TNode()
            (REQUEST, VALID_UPLOAD_REQUEST)
            (UNIXTIME, "1500000000");
        const auto& uploadInfo = TUploadInfo{
            .Match = TExtIdMatch{
                .ExtId = {
                    .Tag = "tag",
                    .Value = "external_id"
                },
                .Yandexuid = "11111111101444000000",
                .Synthetic = true
            },
            .Timestamp = 1500000000u,
        };
        UNIT_ASSERT_EQUAL(ParseUploadInfo(row), uploadInfo);
    }

    Y_UNIT_TEST(NoSyntheticMatchField) {
        const auto& request = "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" }}";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");
        const auto& uploadInfo = TUploadInfo{
            .Match = TExtIdMatch{
                .ExtId = {
                    .Tag = "tag",
                    .Value = "external_id"
                },
                .Yandexuid = "11111111101444000000",
                .Synthetic = false
            },
            .Timestamp = 1500000000u,
        };
        UNIT_ASSERT_EQUAL(ParseUploadInfo(row), uploadInfo);
    }

    Y_UNIT_TEST(InvalidBooleanValue) {
        const auto& request = "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: xxx track_back_reference: false }}";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ParseUploadInfo(row), yexception, "Invalid boolean value 'xxx' for field 'synthetic_match'");
    }

    Y_UNIT_TEST(SyntheticMatchFieldValueWithoutEnd) {
        const auto& request = "header { type: UPLOAD version: VERSION_4_10 } [iscrypta.upload.RequestBody.ext] { type: EXT_ID ttl: 86400 ext_id { ext_id: \"tag.external_id\" yuid: \"11111111101444000000\" synthetic_match: true}}";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ParseUploadInfo(row), yexception, "Can't find end of 'synthetic_match' value");
    }
}

Y_UNIT_TEST_SUITE(Identify) {
    Y_UNIT_TEST(HasExtIdIdentifyRequest) {
        UNIT_ASSERT(HasExtIdIdentifyRequest(NYT::TNode()(REQUEST, VALID_IDENTIFY_REQUEST)));
        UNIT_ASSERT(!HasExtIdIdentifyRequest(NYT::TNode::CreateMap()));
        UNIT_ASSERT(!HasExtIdIdentifyRequest(NYT::TNode()(REQUEST, NYT::TNode::CreateEntity())));
        UNIT_ASSERT(!HasExtIdIdentifyRequest(NYT::TNode()(REQUEST, TYPE_IDENTIFY)));
        UNIT_ASSERT(!HasExtIdIdentifyRequest(NYT::TNode()(REQUEST, EXT_ID)));
    }

    Y_UNIT_TEST(Positive) {
        const auto& row = NYT::TNode()
            (REQUEST, VALID_IDENTIFY_REQUEST)
            (UNIXTIME, "1500000000");
        const auto& identifyInfo = TIdentifyInfo{
            .ExtId = {
                .Tag = "tag",
                .Value = "external_id"
            },
            .Timestamp = 1500000000u,
        };
        UNIT_ASSERT_EQUAL(ParseIdentifyInfo(row), identifyInfo);
    }

    Y_UNIT_TEST(MissingRequestField) {
        const auto& row = NYT::TNode()(UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION(ParseIdentifyInfo(row), yexception);
    }

    Y_UNIT_TEST(MissingUnixtimeField) {
        const auto& row = NYT::TNode()(REQUEST, VALID_IDENTIFY_REQUEST);
        UNIT_ASSERT_EXCEPTION(ParseIdentifyInfo(row), yexception);
    }

    Y_UNIT_TEST(InvalidUnixtimeField) {
        const auto& row = NYT::TNode()
            (REQUEST, VALID_IDENTIFY_REQUEST)
            (UNIXTIME, "xxx");
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseIdentifyInfo(row),
            yexception,
            "Unixtime is undefined"
        );
    }

    Y_UNIT_TEST(NotIdentifyType) {
        const auto& request = "header { type: UPLOAD version: VERSION_4_10 tag: \"1111\" } [iscrypta.identify.RequestBody.ext] { type: WITH_INFO inspect: false ext_id: \"tag.external_id\" }";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseIdentifyInfo(row),
            yexception,
            "TYPE_IDENTIFY is missing in header"
        );
    }

    Y_UNIT_TEST(ExtIdIsMissing) {
        const auto& request = "header { type: IDENTIFY version: VERSION_4_10 tag: \"1111\" } [iscrypta.identify.RequestBody.ext] { type: WITH_INFO inspect: false gaid: \"444\" }";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseIdentifyInfo(row),
            yexception,
            "Can't find value of ext_id field or value is empty"
        );
    }

    Y_UNIT_TEST(Incomplete) {
        const auto& request = "header { type: IDENTIFY version: VERSION_4_10 tag: \"1111\" } [iscrypta.identify.RequestBody.ext] { type: WITH_INFO inspect: false ext_id: \"4";
        const auto& row = NYT::TNode()
            (REQUEST, request)
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseIdentifyInfo(row),
            yexception,
            "Can't find body boundaries"
        );
    }

    Y_UNIT_TEST(Invalid) {
        const auto& row = NYT::TNode()
            (REQUEST, "header } type: IDENTIFY version: VERSION_4_10 { ext_id: ")
            (UNIXTIME, "1500000000");
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            ParseIdentifyInfo(row),
            yexception,
            "Can't find header boundaries"
        );
    }
}
