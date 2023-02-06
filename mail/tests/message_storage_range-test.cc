#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/range/algorithm/transform.hpp>
#include <internal/MessageStorageRange.h>
#include <mail_getter/mulcagate/errors.h>
#include <library/cpp/testing/gtest_boost_extensions/extensions.h>
#include "mime_part.h"
#include "storage/service_mock.h"
#include "logging_mock.h"
#include "recognizer_instance.h"

namespace mail_getter {

inline bool operator == (const Range& l, const Range& r) {
    return l.first == r.first && l.last == r.last;
}

}

namespace {

using namespace ::testing;
using namespace ::mail_getter;
using MulcagateErrors = ::mail_getter::mulcagate::Errors;

const std::string inlineMessage =
"Received: by web-tst1j.yandex.ru with HTTP;\r\n"
"\tSat, 06 May 2017 15:44:44 +0300\r\n"
"From: InlineFrom <inline.from@ya.ru>\r\n"
"To: to2@yandex.ru\r\n"
"Subject: inline with attach\r\n"
"Message-Id: <111494074684@web-tst1j.yandex.ru>\r\n"
"Date: Sat, 06 May 2017 15:44:44 +0300\r\n"
"Content-Type: multipart/mixed;\r\n"
"\tboundary=\"----==--bound.12.web-tst1j.yandex.ru\"\r\n"
"\r\n"
"\r\n"
"------==--bound.12.web-tst1j.yandex.ru\r\n"
"Content-Transfer-Encoding: 7bit\r\n"
"Content-Type: text/html\r\n"
"\r\n"
"<div>test inline message with attach</div>\r\n"
"------==--bound.12.web-tst1j.yandex.ru\r\n"
"Content-Disposition: attachment;\r\n"
"\tfilename=\"test_attach1.txt\"\r\n"
"Content-Transfer-Encoding: base64\r\n"
"Content-Type: text/plain;\r\n"
"\tname=\"test_attach1.txt\"\r\n"
"\r\n"
"VGVzdCBhdHRhY2ggZmlsZS4NCk9uZS4=\r\n"
"------==--bound.12.web-tst1j.yandex.ru--\r\n";

const MetaParts defaultMetaParts = {
        {"1", macs::MimePartFactory().hid("1").contentType("multipart").contentSubtype("mixed")
                .charset("US-ASCII").encoding("7bit").offsetBegin(345).offsetEnd(1399).release()},
        {"1.1", macs::MimePartFactory().hid("1.1").contentType("text").contentSubtype("html")
                .charset("US-ASCII").encoding("7bit").offsetBegin(447).offsetEnd(496).release()},
        {"1.2", macs::MimePartFactory().hid("1.2").contentType("message").contentSubtype("rfc822")
                .charset("US-ASCII").encoding("8bit").offsetBegin(603).offsetEnd(1355).release()},

};

const MetaParts newMetaPartsForInlineMessages = {
        {"1", macs::MimePartFactory().hid("1").contentType("multipart").contentSubtype("mixed")
                .charset("US-ASCII").encoding("7bit").offsetBegin(345).offsetEnd(1399).release()},
        {"1.1", macs::MimePartFactory().hid("1.1").contentType("text").contentSubtype("html")
                .charset("US-ASCII").encoding("7bit").offsetBegin(447).offsetEnd(496).release()},
        {"1.2", macs::MimePartFactory().hid("1.2").contentType("message").contentSubtype("rfc822")
                .charset("US-ASCII").encoding("8bit").offsetBegin(603).offsetEnd(1355).release()},
        {"1.2.1.1", macs::MimePartFactory().hid("1.2.1.1").contentType("text").contentSubtype("plain")
                .charset("US-ASCII").encoding("8bit").offsetBegin(1005).offsetEnd(1074).release()},
        {"1.2.1.2", macs::MimePartFactory().hid("1.2.1.1").contentType("text").contentSubtype("plain")
                .charset("US-ASCII").encoding("8bit").offsetBegin(1112).offsetEnd(1201).release()}

};

struct MessageStorageRangeTest : public Test {
    storage::ServiceMockPtr storageService;
    logging::LogMockPtr logger;

    MessageStorageRangeTest() : storageService(std::make_shared<storage::ServiceMock>()),
            logger(std::make_shared<logging::LogMock>()) {
    }

    MessageStorageRange makeStorage(MetaParts metaParts) const {
        return MessageStorageRange("1.1.1", storageService, logger, std::move(metaParts), getRecognizer());
    }

    MessageStorageRange makeStorage() const {
        return makeStorage(defaultMetaParts);
    }
};


TEST_F(MessageStorageRangeTest, construct_withEmptyStid_throwsInvalidArgumentException) {
    EXPECT_THROW(MessageStorageRange("", storageService, logger, defaultMetaParts, getRecognizer()),
            std::invalid_argument);
}

TEST_F(MessageStorageRangeTest, construct_withoutStorageService_throwsException) {
    EXPECT_THROW(MessageStorageRange("1.1.1", nullptr, logger, defaultMetaParts, getRecognizer()),
            std::runtime_error);
}


TEST_F(MessageStorageRangeTest, getBodyStruct_nonExistentHid_returnsEmptyBodyStruct) {
    auto storage = makeStorage();
    EXPECT_EQ(storage.getBodyStruct("111.111"), MetaLevel());
}

TEST_F(MessageStorageRangeTest, getBodyStruct_existentHid_returnsBodyStructForThisHid) {
    auto storage = makeStorage();

    const MetaLevel expected = {"1.1", "1.2"};
    EXPECT_EQ(storage.getBodyStruct("1"), expected);
}

TEST_F(MessageStorageRangeTest, getBodyStruct_inlineMessageHid_returnsBodyStructForThisHid) {
    auto storage = makeStorage();

    const MetaLevel expected = {"1.2.1"};
    EXPECT_EQ(storage.getBodyStruct("1.2"), expected);
}

TEST_F(MessageStorageRangeTest, getBodyStruct_existentHidInInlineMessage_returnsBodyStructForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const MetaLevel expected = {"1.2.1.1", "1.2.1.2"};
    EXPECT_EQ(storage.getBodyStruct("1.2.1"), expected);
}

TEST_F(MessageStorageRangeTest, getBodyStruct_nonExistentHidInInlineMessage_returnsEmptyBodyStruct) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    EXPECT_EQ(storage.getBodyStruct("1.2.9"), MetaLevel());
}

TEST_F(MessageStorageRangeTest, getBodyStruct_hidInInlineMessage_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getBodyStruct("1.2.1"), std::runtime_error);
}


TEST_F(MessageStorageRangeTest, getBody_nonExistentHid_returnsNone) {
    auto storage = makeStorage();
    EXPECT_EQ(storage.getBody("111.111"), boost::none);
}

TEST_F(MessageStorageRangeTest, getBody_existentHid_returnsBodyForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.1");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, "body"));

    EXPECT_EQ(storage.getBody("1.1"), OptString("body"));
}

TEST_F(MessageStorageRangeTest, getBody_rawMessage_wholeMessageHid_returnsWholeMessage) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "whole_message"));

    EXPECT_EQ(storage.getBody(wholeMessageHid), OptString("whole_message"));
}

TEST_F(MessageStorageRangeTest, getBody_rawMessageWithMessageTag_wholeMessageHid_returnsWholeMessageWithMessageTag) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "Message_start<message>data</message>\nMessage_end"));

    EXPECT_EQ(storage.getBody(wholeMessageHid),
            OptString("Message_start<message>data</message>\nMessage_end"));
}

TEST_F(MessageStorageRangeTest, getBody_xmlMessage_wholeMessageHid_returnsWholeMessageWithoutXmlPart) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<message>message_xml_data</message>\nWhole_message"));

    EXPECT_EQ(storage.getBody(wholeMessageHid), OptString("Whole_message"));
}

TEST_F(MessageStorageRangeTest, getBody_existentHidInInlineMessage_returnsBodyForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const OptString expected = std::string("<div>test inline message with attach</div>");
    EXPECT_EQ(storage.getBody("1.2.1.1"), expected);
}

TEST_F(MessageStorageRangeTest,
        for_new_meta_part_format_getBody_existentHidInInlineMessage_returnsBodyForThisHid) {
    auto storage = makeStorage(newMetaPartsForInlineMessages);

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const OptString expected = std::string("<div>test inline message with attach</div>");
    EXPECT_EQ(storage.getBody("1.2.1.1"), expected);
}

TEST_F(MessageStorageRangeTest, getBody_nonExistentHidInInlineMessage_returnsNone) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    EXPECT_EQ(storage.getBody("1.2.1.9"), boost::none);
}

TEST_F(MessageStorageRangeTest, getBody_existentHid_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.1");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getBody("1.1"), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getBody_wholeMessageHid_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getBody(wholeMessageHid), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getBody_hidInInlineMessage_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getBody("1.2.1.1"), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getHeader_nonExistentHid_returnsNone) {
    auto storage = makeStorage();
    EXPECT_EQ(storage.getHeader("111.111"), boost::none);
}

TEST_F(MessageStorageRangeTest, getHeader_existentHid_returnsHeaderForThisHid) {
    auto rootMetaPart = macs::MimePartFactory().hid("1.1").offsetBegin(37).offsetEnd(40).release();
    const MetaParts metaParts = { {"1.1", std::move(rootMetaPart)} };
    auto storage = makeStorage(metaParts);

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "Message_part\r\n--boundary\r\nHeaders\r\n\r\nBody"));

    EXPECT_EQ(storage.getHeader("1.1"), OptString("Headers\r\n\r\n"));
}

TEST_F(MessageStorageRangeTest, getHeader_rawMessage_rootHid_returnsHeaderForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at(rootHid);
    const mail_getter::Range range = {0, mp.offsetBegin() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, "message_headers"));

    EXPECT_EQ(storage.getHeader(rootHid), OptString("message_headers"));
}

TEST_F(MessageStorageRangeTest, getHeader_xmlMessage_rootHid_returnsHeaderForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at(rootHid);
    const mail_getter::Range range = {0, mp.offsetBegin() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<message>message_xml_data</message>\nMessage_headers"));

    EXPECT_EQ(storage.getHeader(rootHid), OptString("Message_headers"));
}

TEST_F(MessageStorageRangeTest, getHeader_rootHid_rawMessageWithoutHeaders_returnsEmptyHeader) {
    auto rootMetaPart = macs::MimePartFactory().hid(rootHid).offsetBegin(0).offsetEnd(708).release();
    const MetaParts metaParts = { {rootHid, std::move(rootMetaPart)} };
    auto storage = makeStorage(metaParts);

    EXPECT_CALL(*logger, notice("MessageStorageRange", "no headers in message"));

    EXPECT_EQ(storage.getHeader(rootHid), OptString(""));
}

TEST_F(MessageStorageRangeTest, getHeader_existentHidInInlineMessage_returnsHeaderForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const OptString expected = std::string("Content-Transfer-Encoding: 7bit\r\nContent-Type: text/html\r\n\r\n");
    EXPECT_EQ(storage.getHeader("1.2.1.1"), expected);
}

TEST_F(MessageStorageRangeTest, getHeader_nonExistentHidInInlineMessage_returnsNone) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    EXPECT_EQ(storage.getHeader("1.2.1.9"), boost::none);
}

TEST_F(MessageStorageRangeTest,
        for_new_meta_part_format_getHeader_existentHidInInlineMessage_returnsHeaderForThisHid) {
    auto storage = makeStorage(newMetaPartsForInlineMessages);

    const auto& mp = newMetaPartsForInlineMessages.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const OptString expected = std::string("Content-Transfer-Encoding: 7bit\r\nContent-Type: text/html\r\n\r\n");
    EXPECT_EQ(storage.getHeader("1.2.1.1"), expected);
}

TEST_F(MessageStorageRangeTest,
        for_new_meta_part_format_getHeader_nonExistentHidInInlineMessage_returnsNone) {
    auto storage = makeStorage(newMetaPartsForInlineMessages);

    const auto& mp = newMetaPartsForInlineMessages.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    EXPECT_EQ(storage.getHeader("1.2.1.9"), boost::none);
}

TEST_F(MessageStorageRangeTest, getHeader_existentHid_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getHeader("1.1"), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getHeader_rootHid_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at(rootHid);
    const mail_getter::Range range = {0, mp.offsetBegin() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getHeader(rootHid), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getHeader_hidInInlineMessage_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getHeader("1.2.1.1"), std::runtime_error);
}


TEST_F(MessageStorageRangeTest, getHeaderStruct_nonExistentHid_returnsNone) {
    auto storage = makeStorage();
    EXPECT_EQ(storage.getHeaderStruct("111.111"), boost::none);
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_existentHid_returnsHeaderStructForThisHid) {
    auto storage = makeStorage();

    EXPECT_EQ(storage.getHeaderStruct("1.1"), defaultMetaParts.at("1.1"));
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_wholeMessageHid_returnsWholeMessageHeaderStruct) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at(rootHid);
    const mail_getter::Range range = {0, mp.offsetBegin() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, "Subject: Message with inline mail\r\nOther_Headers: some data\r\n"));

    const auto expected = macs::MimePartFactory().hid(wholeMessageHid).contentType("message")
            .contentSubtype("rfc822").fileName("Message with inline mail.txt").release();

    EXPECT_EQ(storage.getHeaderStruct(wholeMessageHid), expected);
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_existentHidInInlineMessage_returnsHeaderStructForThisHid) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    const auto expected = macs::MimePartFactory().hid("1.2.1.2").contentDisposition("attachment")
                .contentType("text").contentSubtype("plain").encoding("base64").charset("US-ASCII")
                .fileName("test_attach1.txt").name("test_attach1.txt")
                .offsetBegin(676).offsetEnd(708).release();
    EXPECT_EQ(storage.getHeaderStruct("1.2.1.2"), expected);
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_nonExistentHidInInlineMessage_returnsNone) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{}, inlineMessage));

    EXPECT_EQ(storage.getHeaderStruct("1.2.1.9"), boost::none);
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_hidInInlineMessage_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at("1.2");
    const mail_getter::Range range = {mp.offsetBegin(), mp.offsetEnd() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getHeaderStruct("1.2.1.2"), std::runtime_error);
}

TEST_F(MessageStorageRangeTest, getHeaderStruct_wholeMessageHid_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    const auto& mp = defaultMetaParts.at(rootHid);
    const mail_getter::Range range = {0, mp.offsetBegin() - 1};
    EXPECT_CALL(*storageService, asyncGetByRange("1.1.1", range, _)).WillOnce(
            InvokeArgument<2>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getHeaderStruct(wholeMessageHid), std::runtime_error);
}


TEST_F(MessageStorageRangeTest, getWhole_rawMessage_wholeMessageHid_returnsWholeMessage) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "whole_message"));

    EXPECT_EQ(storage.getWhole(), "whole_message");
}

TEST_F(MessageStorageRangeTest, getWhole_rawMessageWithMessageTag_returnsWholeMessageWithMessageTag) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "Message_start<message>data</message>\nMessage_end"));

    EXPECT_EQ(storage.getWhole(), "Message_start<message>data</message>\nMessage_end");
}

TEST_F(MessageStorageRangeTest, getWhole_xmlMessage_returnsWholeMessageWithoutXmlPart) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{}, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<message>message_xml_data</message>\nWhole_message"));

    EXPECT_EQ(storage.getWhole(), "Whole_message");
}

TEST_F(MessageStorageRangeTest, getWhole_storageServiceReturnsError_weThrowException) {
    auto storage = makeStorage();

    EXPECT_CALL(*storageService, asyncGetBlob("1.1.1", _)).WillOnce(
            InvokeArgument<1>(error_code{MulcagateErrors::internal}, ""));

    EXPECT_THROW(storage.getWhole(), std::runtime_error);
}

}
