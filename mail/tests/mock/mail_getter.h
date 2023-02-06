#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/sanitizer/sanitizer_markup.h>
#include <mail_getter/recognizerWrapper.h>
#include <mail_getter/SimpleAttachment.h>
#include <mail_getter/service.h>

namespace sendbernar::tests {

struct MockRecognizerWrapper: public Recognizer::Wrapper {
    MOCK_METHOD(Recognizer::TextTraits, recognize, (const std::string&), (const, override));
    MOCK_METHOD(Recognizer::CodesEnum, recognizeEncoding, (const std::string&), (const, override));
    MOCK_METHOD(std::string, isoNameByLanguageCode, (Recognizer::LangsEnum), (const, override));
};

struct MockService: public mail_getter::Service {
    MOCK_METHOD(MessageAccessPtr, createMessageAccess, (mail_getter::MessageAccessParams,
                                                             const Recognizer::Wrapper&, OptYieldContext), (const, override));
    MOCK_METHOD(MessageAccessPtr, createMessageAccess, (mail_getter::MessageAccessWithWindatParams,
                                                             const Recognizer::Wrapper&, OptYieldContext), (const, override));
    MOCK_METHOD(mail_getter::AttachmentStoragePtr, createAttachmentStorage, (const std::string&), (const, override));
    MOCK_METHOD(mail_getter::MetaPartsConverterPtr, createMetaPartsConverter, (const Recognizer::Wrapper&), (const, override));
};

struct MockAttachmentStorage: public mail_getter::AttachmentStorage {
    MOCK_METHOD(int, add, (const mail_getter::AbstractAttachment&, std::string&,
                          mail_getter::AttachmentStorage::MetaInfo&, std::chrono::seconds,
                          std::chrono::seconds, const mail_getter::attach_sid::Keys&, OptYieldContext), (override));
    MOCK_METHOD(int, get, (const std::vector<std::string>&, AttachmentStorage::VectorOfAttachments&, OptYieldContext), (override));
};

struct MockMessageAccess: public MessageAccess {
    MOCK_METHOD(MetaPart, getHeaderStruct, (const std::string&), (override));
    MOCK_METHOD(MetaLevel, getBodyStruct, (const std::string&), (override));
    MOCK_METHOD(std::string, getHeader, (const std::string&), (override));
    MOCK_METHOD(std::string, getBody, (const std::string&), (override));
    MOCK_METHOD(std::string, getWhole, (), (override));
    MOCK_METHOD(const std::string&, getStId, (), (const, override));
    MOCK_METHOD(MetaAttributes, getMessageHeaderParsed, (const std::string&), (override));
};

}

namespace mail_getter {

inline bool operator==(const MessageAccessParams& a, const MessageAccessParams& b) {
    return a.stid == b.stid;
}

inline std::ostream& operator<<(std::ostream& out, const MarkupPosition& a) {
    out << "(" << a.attributeStart << ", " << a.attributeLength << ", " << a.dataStart << ", " << a.dataLength << ")";
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const SanitizerMarkupEntry& a) {
    out << "[" << a.type << ", " << a.classValue << ", " << a.position << "]";
    return out;
}

inline bool operator==(const MarkupPosition& a, const MarkupPosition& b) {
    return a.attributeStart == b.attributeStart && a.attributeLength == b.attributeLength &&
           a.dataStart == b.dataStart && a.dataLength == b.dataLength;
}

inline bool operator==(const SanitizerMarkupEntry& a, const SanitizerMarkupEntry& b) {
    return a.type == b.type && a.classValue == b.classValue && a.position == b.position;
}

}

namespace boost::asio {

inline std::ostream& operator<<(std::ostream& out, const OptYieldContext& c) {
    out << (c ? "OptYieldContext" : "none");
    return out;
}

}
