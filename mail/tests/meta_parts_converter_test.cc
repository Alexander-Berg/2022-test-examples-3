#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/mime_part_factory.h>
#include <mail_getter/message_access_mock.h>
#include <internal/meta_parts_converter_impl.h>
#include <mail_getter/mulcagate/errors.h>
#include "storage/service_mock.h"
#include "mime_part.h"
#include "recognizer_instance.h"

namespace {

using namespace mail_getter;
using namespace testing;

class MetaPartsConverterImplTest: public Test {
public:
    MetaPartsConverterImplTest()
        : storageServiceMock_(new storage::ServiceMock)
        , converter_(storageServiceMock_, getRecognizer())
    {}

protected:
    std::shared_ptr<storage::ServiceMock> storageServiceMock_;
    MetaPartsConverterImpl converter_;
};

const std::string validXml =
"<?xml version=\"1.0\" encoding=\"utf-8\"?>"
"<message>"
"   <part id=\"1\" offset=\"1578\" length=\"75770\""
"       content_type.type=\"multipart\""
"       content_type.subtype=\"mixed\""
"       content_type.charset=\"US-ASCII\""
"       content_transfer_encoding=\"7bit\""
"   >"
"       <part id=\"1.1\" offset=\"1696\" length=\"99\""
"           content_type.type=\"text\""
"           content_type.subtype=\"html\""
"           content_type.charset=\"utf-8\""
"           content_transfer_encoding=\"8bit\""
"       >"
"       </part>"
"       <part id=\"1.2\" offset=\"1977\" length=\"75326\""
"           content_type.type=\"image\""
"           content_type.subtype=\"png\""
"           content_type.charset=\"US-ASCII\""
"           content_type.name=\"icon2.png\""
"           content_transfer_encoding=\"base64\""
"           content_disposition.value=\"attachment\""
"           content_disposition.filename=\"icon2.png\""
"       >"
"       </part>"
"   </part>"
"</message>";

const std::string invalidXml =
"<?xml version=\"1.0\" encoding=\"utf-8\"?>"
"<message>";

const std::string inlineMessage =
"Received: from mxback10h.mail.yandex.net ([127.0.0.1])\r\n"
"\tby mxback10h.mail.yandex.net with LMTP id nLLjJztf\r\n"
"\tfor <test-dskut2@yandex.ru>; Tue, 27 Jun 2017 23:32:42 +0300\r\n"
"Received: from mxback10h.mail.yandex.net (localhost.localdomain [127.0.0.1])\r\n"
"\tby mxback10h.mail.yandex.net (Yandex) with ESMTP id 8957C1441089\r\n"
"\tfor <test-dskut2@yandex.ru>; Tue, 27 Jun 2017 23:32:42 +0300 (MSK)\r\n"
"Received: from web44j.yandex.ru (web44j.yandex.ru [5.45.198.147])\r\n"
"\tby mxback10h.mail.yandex.net (nwsmtp/Yandex) with ESMTP id 3m6q8bLt10-Wgm4w3Ge;\r\n"
"\tTue, 27 Jun 2017 23:32:42 +0300\r\n"
"X-Yandex-Front: mxback10h.mail.yandex.net\r\n"
"X-Yandex-TimeMark: 1498595562\r\n"
"DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1498595562;\r\n"
"\tbh=kZWZBuN9/QE/YoprzKtAzS7E3yQudk/lO3pPUiHKmCk=;\r\n"
"\th=From:To:Subject:Message-Id:Date;\r\n"
"\tb=s0Ulk5wxzAnscNw229UM4Coz6l55vWniMWoJVbWpKbijxIrFueRFrR0E9nvHea4wO\r\n"
"\t m1bnCuQK01+xixDEjZX6SlPv61Z9nDztlYC8S2nKOBe28vKXnz1gM3c1CEcO83bPkV\r\n"
"\t jT0Rz9kpBbYgKS9NaWf8bmhJTOxpvaEFnAC9JDGU=\r\n"
"Authentication-Results: mxback10h.mail.yandex.net; dkim=pass header.i=@yandex.ru\r\n"
"X-Yandex-Spam: 1\r\n"
"X-Yandex-Sender-Uid: 210779027\r\n"
"Received: by web44j.yandex.ru with HTTP;\r\n"
"\tTue, 27 Jun 2017 23:32:42 +0300\r\n"
"From: Test Denis Kutukov <test-dskut@yandex.ru>\r\n"
"To: test-dskut2 <test-dskut2@yandex.ru>\r\n"
"Subject: test inline\r\n"
"MIME-Version: 1.0\r\n"
"Message-Id: <542631498595562@web44j.yandex.ru>\r\n"
"X-Mailer: Yamail [ http://yandex.ru ] 5.0\r\n"
"Date: Tue, 27 Jun 2017 23:32:42 +0300\r\n"
"Content-Transfer-Encoding: 7bit\r\n"
"Content-Type: text/html\r\n"
"Return-Path: test-dskut@yandex.ru\r\n"
"X-Yandex-Forward: 0e39c9c014e4bb5ba685894ddad39943\r\n"
"\r\n"
"<div>hello</div>\r\n";

TEST_F(MetaPartsConverterImplTest, getMetaPartsFromXml_fetchAndParseXmlWithoutErrors_returnCorrectMetaParts) {
    EXPECT_CALL(*storageServiceMock_, asyncGetXml("111.222.333", _))
        .WillOnce(InvokeArgument<1>(mail_errors::error_code(), validXml));
    const MetaParts actual = converter_.getMetaPartsFromXml("111.222.333", boost::none);
    const MetaParts expected = {
        {"1", macs::MimePartFactory()
                .hid("1")
                .contentType("multipart")
                .contentSubtype("mixed")
                .charset("US-ASCII")
                .encoding("7bit")
                .offsetBegin(1578 + validXml.size())
                .offsetEnd(1578 + 75770 + validXml.size())
                .release()},
        {"1.1", macs::MimePartFactory()
                .hid("1.1")
                .contentType("text")
                .contentSubtype("html")
                .charset("utf-8")
                .encoding("8bit")
                .offsetBegin(1696 + validXml.size())
                .offsetEnd(1696 + 99 + validXml.size())
                .release()},
        {"1.2", macs::MimePartFactory()
                .hid("1.2")
                .contentType("image")
                .contentSubtype("png")
                .name("icon2.png")
                .charset("US-ASCII")
                .encoding("base64")
                .contentDisposition("attachment")
                .fileName("icon2.png")
                .offsetBegin(1977 + validXml.size())
                .offsetEnd(1977 + 75326 + validXml.size())
                .release()},
    };
    EXPECT_EQ(expected, actual);
}

TEST_F(MetaPartsConverterImplTest, getMetaPartsFromXml_fetchError_throwException) {
    EXPECT_CALL(*storageServiceMock_, asyncGetXml("111.222.333", _))
        .WillOnce(InvokeArgument<1>(mulcagate::make_error_code(mulcagate::Errors::internal), ""));
    EXPECT_THROW(converter_.getMetaPartsFromXml("111.222.333", boost::none), std::runtime_error);
}

TEST_F(MetaPartsConverterImplTest, getMetaPartsFromXml_invalidXml_throwException) {
    EXPECT_CALL(*storageServiceMock_, asyncGetXml("111.222.333", _))
        .WillOnce(InvokeArgument<1>(mail_errors::error_code(), invalidXml));
    EXPECT_THROW(converter_.getMetaPartsFromXml("111.222.333", boost::none), std::runtime_error);
}

TEST_F(MetaPartsConverterImplTest, extractMetaPartsFromInline_parseInline_returnCorrectMetaParts) {
    const MetaPart inlineMessagePart = macs::MimePartFactory().hid("1.2")
        .contentType("message").contentSubtype("rfc822")
        .offsetBegin(2910).offsetEnd(4550)
        .release();

    const auto maMock = std::make_shared<MessageAccessMock>();
    EXPECT_CALL(*maMock, getBody("1.2")).WillOnce(Return(inlineMessage));

    const MetaParts actual = converter_.extractMetaPartsFromInline(maMock, inlineMessagePart, boost::none);
    const MetaParts expected = {
        {"1.2.1", macs::MimePartFactory().hid("1.2.1")
            .contentType("text").contentSubtype("html")
            .charset("US-ASCII").encoding("7bit")
            .offsetBegin(4532).offsetEnd(4550)
            .release()}
    };
    EXPECT_EQ(expected, actual);
}

} // namespace

