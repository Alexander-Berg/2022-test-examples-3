#include <gtest/gtest.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <mail/notsolitesrv/src/message/parser.h>
#include <mail/notsolitesrv/src/meta_save_op/types/request.h>

#include <library/cpp/resource/resource.h>
#include <util/generic/yexception.h>
#include <util/memory/blob.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;

struct TMetaSaveOpPartsAndAttachments: public TestWithParam<std::pair<const std::string, std::vector<std::string>>> {
    auto GetMessage() {
        auto data = NResource::Find("messages/" + GetParam().first);
        MsgData = std::string(data.data(), data.size());
        return ParseMessage(MsgData, GetContext({{"skip_attach_with_content_id", "true"}}));
    }

    const std::vector<std::string>& GetExpected() const {
        return GetParam().second;
    }

    std::string MsgData;
};

struct TMetaSaveOpAttachments: public TMetaSaveOpPartsAndAttachments {};
struct TMetaSaveOpParts: public TMetaSaveOpPartsAndAttachments {};
struct TMetaSaveOpBestTextPart: public TMetaSaveOpPartsAndAttachments {};

const std::map<std::string, std::vector<std::string>> ATTACHMENTS_DATA{
    { "att_all.eml",    {"1.2,application/msword,attachment.doc,23552",
                         "1.3,application/vnd.openxmlformats-officedocument.wordprocessingml.document,filename_not_equal_to_name.docx,14893",
                         "1.4,application/pdf,filename_is_absent.pdf,117214",
                         "1.5,text/rtf,attachment4.rtf,33705",
                         "1.6,application/vnd.oasis.opendocument.text,attachment5.odt,6079",
                         "1.7,application/vnd.ms-office,attachment6.ppt,102400",
                         "1.8,application/vnd.openxmlformats-officedocument.presentationml.presentation,attachment7.pptx,35932",
                         "1.9,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,attachment8.xlsx,9006",
                         "1.10,application/vnd.ms-excel,attachment9.xls,24576",
                         "1.11,application/vnd.oasis.opendocument.spreadsheet,attachment10.ods,3071"}},
    { "att_alt.eml",    {"1.1.1,application/zip,Noname,49529"}},
    { "doc-docx.eml",   {"1.2,application/msword,attachment.doc,23552",
                         "1.3,application/vnd.openxmlformats-officedocument.wordprocessingml.document,attachment2.docx,14893"}},
    { "mproto2462.eml", {"1.2,text/html,narod_attachment_links.html,227"}},
    { "odt-jpg.eml",    {"1.2,image/jpeg,1-1.JPG,633",
                         "1.3,application/vnd.oasis.opendocument.text,attachment5.odt,6079"}},
                         // also checks that part with content-id header is not attach if skip_attach_with_content_id cfg param is set: see eml, part 1.4
    { "ppt-pptx.eml",   {"1.2,application/vnd.ms-office,attachment6.ppt,102400",
                         "1.3,application/vnd.openxmlformats-officedocument.presentationml.presentation,attachment7.pptx,35932"}},
    { "simple.eml",     {}},
    { "inline.eml",     {"1.3,message/rfc822,Some subject.eml,2242",
                         "1.4,message/rfc822,message,54"}},
    { "att_one.eml",    {"1.3,application/javascript,test.json,27"}},
    { "smime-p7s.eml",  {}}, // checks that part with s/mime signature does not treated as attach even with wrong content-type
    { "wrong_attach_sz.eml",
                        {"1,message/rfc822,Обучение по ISO 9001, 22000, IRCA.eml,1219937"}},
    { "only_two.eml",   {"1.2.3,application/octet-stream,бу1.png,2284613",
                         "1.2.4,application/octet-stream,бу2.png,1694836"}}
};

TEST_P(TMetaSaveOpAttachments, Find) {
    auto message = GetMessage();

    TPartMap parts;
    TAttachmentMap atts;

    FindPartsAndAttachments(message, parts, atts);
    std::vector<std::string> actualAttachments;
    std::transform(atts.begin(), atts.end(), std::back_inserter(actualAttachments),
        [](const auto& att_elem) {
            const auto& [hid, att] = att_elem;
            return hid + "," + att.type + "," + att.name + "," + std::to_string(att.size);
        });
    EXPECT_EQ(actualAttachments, GetExpected());
}

TEST(TMetaSaveOpAttachmentsWithCfgParam, SkipAttachWithContentIdSetToFalse) {
    // see above
    auto data = NResource::Find("messages/odt-jpg.eml");
    std::string msgData{data.data(), data.size()};
    auto message = ParseMessage(msgData, GetContext({{"skip_attach_with_content_id", "false"}}));

    TPartMap parts;
    TAttachmentMap atts;

    FindPartsAndAttachments(message, parts, atts);
    std::vector<std::string> actualAttachments;
    std::transform(atts.begin(), atts.end(), std::back_inserter(actualAttachments),
        [](const auto& att_elem) {
            const auto& [hid, att] = att_elem;
            return hid + "," + att.type + "," + att.name + "," + std::to_string(att.size);
        });
    EXPECT_EQ(actualAttachments,
        (std::vector<std::string>{
            "1.2,image/jpeg,1-1.JPG,633",
            "1.3,application/vnd.oasis.opendocument.text,attachment5.odt,6079",
            "1.4,image/jpeg,this_is_not_attach.JPG,633"
        })
    );
}

INSTANTIATE_TEST_SUITE_P(TMetaSaveOpRequest, TMetaSaveOpAttachments, ValuesIn(ATTACHMENTS_DATA));

const std::map<std::string, std::vector<std::string>> PARTS_DATA{
    { "simple.eml",    {"1,2/11,text/plain,US-ASCII,7bit,--,--,--,--,--"}},
    { "normal-dsn.eml",{"1,2/3954,multipart/report,US-ASCII,7bit, AC0D040FB1.1539180877/forward104j.mail.yandex.net,--,--,--,--",
                        "1.1,186/1337,text/plain,utf-8,7bit,--,--,--,--, anchor",
                        "1.2,1652/672,message/delivery-status,US-ASCII,7bit,--,--,--,--,--",
                        "1.3,2448/1453,message/RFC822,US-ASCII,7bit,--,--,--,--,--",
                        "1.3.1,3845/57,text/html,US-ASCII,7bit,--,--,--,--,--"}},
};

TEST_P(TMetaSaveOpParts, Find) {
    auto message = GetMessage();

    TPartMap parts;
    TAttachmentMap atts;

    FindPartsAndAttachments(message, parts, atts);
    std::vector<std::string> actualParts;
    std::transform(parts.begin(), parts.end(), std::back_inserter(actualParts),
        [](const auto& part_elem) {
            const auto& [hid, part] = part_elem;
            std::ostringstream os;
            os << hid << "," << part.offset << "/" << part.length << ","
                << part.content_type << "/" << part.content_subtype << ","
                << part.charset << "," << part.encoding << "," << part.boundary << ","
                << part.content_disposition << "," << part.name << "," << part.file_name << ","
                << part.content_id;
            return os.str();
        });
    EXPECT_EQ(actualParts, GetExpected());
}

INSTANTIATE_TEST_SUITE_P(TMetaSaveOpRequest, TMetaSaveOpParts, ValuesIn(PARTS_DATA));

const std::map<std::string, std::vector<std::string>> BEST_TEXT_PARTS_DATA{
    { "find_best_text_part_simple.eml",    {"1,Best text part ever!\n"}},
    { "find_best_text_part_simple_b64.eml",{"1,Best text part ever!"}},
    { "find_best_text_part_simple_qp.eml", {"1,Best text part ever!"}},
    { "find_best_text_part_simple_enc.eml",{"1,Лучший текстовый парт"}},
    { "find_best_text_part_multipart.eml", {"1.2,Best text part ever!"}},
    { "find_best_text_part_smime.eml",     {"1.4,Best text part ever!"}},
    { "find_best_text_part_embedded.eml",  {"1.1,Best text part ever!"}}
};

std::vector<std::string> MakeActuaBestPart(const TPartMap& parts) {
    for (const auto& [hid, part]: parts) {
        if (part.data) {
            return {hid + "," + *part.data};
        }
    }
    return {""};
}

TEST_P(TMetaSaveOpBestTextPart, Find) {
    auto message = GetMessage();

    TPartMap parts;
    TAttachmentMap atts;

    FindPartsAndAttachments(message, parts, atts);

    EXPECT_EQ(MakeActuaBestPart(parts), GetExpected());
}

INSTANTIATE_TEST_SUITE_P(TMetaSaveOpRequest, TMetaSaveOpBestTextPart, ValuesIn(BEST_TEXT_PARTS_DATA));

TEST(GetPartOffset, OffsetOutOfBounds) {
    EXPECT_THROW(GetPartOffset(Max<off_t>() - 17, -17), yexception);
    EXPECT_THROW(GetPartOffset(17, 17), yexception);
}
