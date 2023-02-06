#include <mail/notsolitesrv/src/meta_save_op/util/firstline.h>

#include <mail/message_types/lib/message_types.h>

#include <gtest/gtest.h>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::TEmailAddress;

TEST(TestFindPartForFirstline, for_part_unavailable_must_return_empty_plain_part) {
    const auto plain{false};
    const std::string plainPart;
    TPartMap parts;
    parts["0"].content_type = "text";
    parts["1"].data = "Data";
    EXPECT_EQ(std::make_pair(plain, std::move(plainPart)), FindPartForFirstline(parts));
}

TEST(TestFindPartForFirstline, for_html_part_available_must_return_html_part) {
    const auto html{true};
    const std::string htmlPart{"HtmlData"};
    TPartMap parts;
    parts["0"].content_type = "text";
    parts["0"].content_subtype = "plain";
    parts["0"].data = "PlainData";
    parts["1"].content_type = "text";
    parts["1"].content_subtype = "html";
    parts["1"].data = htmlPart;
    EXPECT_EQ(std::make_pair(html, std::move(htmlPart)), FindPartForFirstline(parts));
}

TEST(TestFindPartForFirstline, for_html_part_unavailable_must_return_first_plain_part) {
    const auto plain{false};
    const std::string plainPart{"PlainData0"};
    TPartMap parts;
    parts["0"].content_type = "text";
    parts["0"].content_subtype = "plain";
    parts["0"].data = plainPart;
    parts["1"] = parts["0"];
    parts["1"].data = "PlainData1";
    EXPECT_EQ(std::make_pair(plain, std::move(plainPart)), FindPartForFirstline(parts));
}

TEST(TestPeopleTypePresent, must_be_able_to_detect_people_type) {
    EXPECT_FALSE(PeopleTypePresent({NMail::MT_DELIVERY, NMail::MT_REGISTRATION, NMail::MT_ETICKET}));
    EXPECT_TRUE(PeopleTypePresent({NMail::MT_DELIVERY, NMail::MT_REGISTRATION, NMail::MT_PEOPLE}));
}

TEST(TestMakeFirstlineRequest, must_make_firstline_request) {
    TRequest request;
    request.message.subject = "Subject";
    request.message.from.emplace_back(TEmailAddress{"Local", "Domain", "DisplayName"});
    request.message.parts["0"].content_type = "text";
    request.message.parts["0"].content_subtype = "html";
    request.message.parts["0"].data = "Data";
    request.types = {NMail::MT_DELIVERY, NMail::MT_REGISTRATION, NMail::MT_PEOPLE};
    const auto firstlineRequest = MakeFirstlineRequest(request);
    EXPECT_TRUE(firstlineRequest.IsHtml);
    EXPECT_EQ(request.message.parts["0"].data, firstlineRequest.Part);
    EXPECT_EQ(request.message.subject, firstlineRequest.Subject);
    const auto& from = request.message.from.front();
    EXPECT_EQ(from.Local, firstlineRequest.From.Local);
    EXPECT_EQ(from.Domain, firstlineRequest.From.Domain);
    EXPECT_EQ(from.DisplayName, firstlineRequest.From.DisplayName);
    EXPECT_TRUE(firstlineRequest.IsPeopleType);
}

}
