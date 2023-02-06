#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/inline_processor.h>
#include <mail/sendbernar/composer/include/inline_from_verstka.h>
#include <mail/sendbernar/composer/include/inline_content.h>
#include <mail/sendbernar/composer/include/smiles.h>
#include <macs/mime_part_factory.h>

#include <mail/http_getter/client/mock/mock.h>
#include <mail/sendbernar/composer/tests/mock/mail_getter.h>
#include <mail/sendbernar/composer/tests/mock/metadata.h>
#include <mail/sendbernar/composer/tests/mock/sanitizer.h>

using namespace testing;
using namespace http_getter;
using namespace std::string_literals;

inline bool operator==(const RemoteAttachment& a, const RemoteAttachment& b) {
    return a.body == b.body && a.encoding == b.encoding &&
           a.name == b.name && a.disposition == b.disposition &&
           a.cid == b.cid && a.remote_path == b.remote_path &&
           a.original_url == b.original_url;
}

inline std::ostream& operator<<(std::ostream& out, const RemoteAttachment& a) {
    out << "\n[ "
        << "body="         << a.body        << ", "
        << "encoding="     << a.encoding    << ", "
        << "name="         << a.name        << ", "
        << "disposition="  << a.disposition << ", "
        << "cid="          << a.cid         << ", "
        << "remote_path="  << a.remote_path << ", "
        << "original_url=" << a.original_url
        << " ]";

    return out;
}

namespace sendbernar::tests {

struct BaseData {
    std::string uid() const {
        return "uid";
    }

    std::string requestId() const {
        return "requestId";
    }

    std::string host() const {
        return "host";
    }

    void init() {
        body = "|attachment|";
        metadata = std::make_shared<StrictMock<MockMetadata>>();
        storageService = std::make_shared<StrictMock<MockService>>();
        recognizer = std::make_shared<StrictMock<MockRecognizerWrapper>>();
        sanitizer = std::make_shared<StrictMock<MockSanitizer>>();
        attachmentStorage = std::make_shared<StrictMock<MockAttachmentStorage>>();
        inlineProcessor = makeInlineProcessor(createTypedDummy(yhttp::response{.status=200}));
    }

    InlineProcessorPtr makeInlineProcessor(http_getter::TypedClientPtr h) {
        return std::make_shared<InlineProcessor>(InlineProcessor{
            metadata,
            sanitizer,
            storageService,
            *recognizer,
            keyContainer,
            handlers,
            getContextLogger("", uid()),
            h,
            uid(),
            host()
        });
    }

    std::string body;
    InlineClassHandlers handlers;
    std::shared_ptr<StrictMock<MockMetadata>> metadata;
    std::shared_ptr<StrictMock<MockService>> storageService;
    std::shared_ptr<Recognizer::Wrapper> recognizer;
    std::shared_ptr<StrictMock<MockSanitizer>> sanitizer;
    SanitizerConfiguration sanitizerConfig;
    mail_getter::attach_sid::KeyContainer keyContainer;
    std::shared_ptr<StrictMock<MockAttachmentStorage>> attachmentStorage;
    InlineProcessorPtr inlineProcessor;
};

struct SmilesData {
    std::string classId;
    std::shared_ptr<Smiles> smiles;
    http_getter::TypedEndpoint endpoint;
    std::string smilesHost;
    std::string smileBody;

    void init() {
        classId = "_smile";
        endpoint = endpoint.fromData("/method{smile_id}", "", nullptr);
        smilesHost = "url";

        smiles = std::make_shared<Smiles>(endpoint, smilesHost);
        smileBody = "smile_body";
    }
};

struct MockInlineFromVerstka: public InlineFromVerstka {
protected:
    std::string decryptStid(const std::string& classId, InlineHandlerAttributes&) const override {
        return classId;
    }

    std::string generateCid() const override {
        return "content_id";
    }
};

struct InlineFromVerstkaData {
    void init() {
        inlineFromVerstka = std::make_shared<MockInlineFromVerstka>();
    }
    std::shared_ptr<InlineFromVerstka> inlineFromVerstka;
};

struct InlineContentData {
    InlineHandlerAttributes::Stids validStids;
    InlineHandlerAttributes::Stids invalidStids;
    InlineHandlerAttributes::Messages messages;
    mail_getter::MessageAccessParams messageAccessParams;

    std::shared_ptr<StrictMock<MockMessageAccess>> messageAccess;
    std::shared_ptr<InlineContent> inlineContent;

    void init() {
        inlineContent = std::make_shared<InlineContent>();
        messageAccess = std::make_shared<StrictMock<MockMessageAccess>>();
        validStids = {};
        invalidStids = {};
        messages.clear();
    }
};

inline macs::Envelope envelopeWithStid(const std::string& stid) {
    auto envelopeData = std::make_shared<macs::EnvelopeData>();
    envelopeData->stid = stid;

    return macs::Envelope(envelopeData);
}

}
