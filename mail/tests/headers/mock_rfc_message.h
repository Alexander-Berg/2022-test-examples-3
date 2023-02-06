#pragma once

#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/rfc_message.h>

namespace sendbernar {

struct MockRfcMessage: public RfcMessage {
    MOCK_METHOD(void, addEmailHeaderDraft, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, addEmailHeader, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, addUtf8Header, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, addHeader, (const std::string&, const std::string&), (override));

    MOCK_METHOD(void, setUtf8, (), (override));
    MOCK_METHOD(void, addBody, (const std::string&, ContentTypeEncoding, bool), (override));
    MOCK_METHOD(MimeParser::Hid, addPart, (RfcMessagePtr), (override));
    MOCK_METHOD(MultipartRelaxedMessagePtr, addRelatedPart, (RfcMessagePtr), (override));
    MOCK_METHOD(void, addNarodAttach, (const std::string&, ContentTypeEncoding), (override));
    MOCK_METHOD(MimeParser::Hid, addBase64File, (MimeType, const std::string&, const std::string&), (override));
    MOCK_METHOD(MimeParser::Hid, addHid, (MimeType, const MetaPart&, const std::string&, const std::string&), (override));
    MOCK_METHOD(MimeParser::Hid, addRfc822Part, (const std::string&), (override));
};

struct MockMultipartRelaxedMessage: public MultipartRelaxedMessage {
    MOCK_METHOD(MimeParser::Hid, addRemoteAttach, (MimeType, const RemoteAttachment&), (override));
};

}
