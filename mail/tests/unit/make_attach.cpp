#include "common.hpp"
#include <src/logic/message_part_real/make_attach.hpp>
#include <src/services/hound/reflection/mimes.hpp>
#include <mail_getter/message_access_mock.h>
#include <macs/mime_part_factory.h>

namespace retriever {

bool operator ==(const MessagePart& lhs, const MessagePart& rhs) {
    return lhs.content == rhs.content
            && lhs.filename == rhs.filename
            && lhs.type == rhs.type
            && lhs.subtype == rhs.subtype
            && lhs.charset == rhs.charset;
}

} // namespace retriever

BOOST_FUSION_ADAPT_STRUCT(retriever::MessagePart,
    (std::string, content)
    (std::string, filename)
    (std::string, type)
    (std::string, subtype)
    (std::string, charset)
)

namespace Recognizer {
class WrapperTestImpl : public Wrapper {
    TextTraits recognize(const std::string&) const override {
        return TextTraits();
    }

    CodesEnum recognizeEncoding(const std::string&) const override {
        return CodesEnum::CODES_UTF8;
    }

    std::string isoNameByLanguageCode(LangsEnum) const override {
        return "";
    }
};
} // namespace Recognizer

namespace {

using namespace testing;
using namespace mail_getter;
using namespace retriever;
using namespace hound;

using MessageAccessMockPtr = std::shared_ptr<MessageAccessMock>;
using WrapperTestImpl = Recognizer::WrapperTestImpl;

struct HoundClientMock : public HoundClient {
    MOCK_METHOD(OptMessageParts, getMessageParts, (TaskContextPtr, const Uid&, const Mid&), (const, override));
};

using HoundClientMockPtr = std::shared_ptr<HoundClientMock>;

struct MakeMessageAccessMock {
    struct Impl {
        MOCK_METHOD(MessageAccessPtr, call, (Stid, macs::MimeParts, StorageServicePtr, const Recognizer::Wrapper&, YieldContext), (const));
    };

    std::shared_ptr<const Impl> impl = std::make_shared<const Impl>();

    MessageAccessPtr operator ()(Stid stid, macs::MimeParts mimeParts, StorageServicePtr storageService, const Recognizer::Wrapper& recognizer, YieldContext yc) const {
        return impl->call(std::move(stid), std::move(mimeParts), std::move(storageService), recognizer, yc);
    }
};

struct MakeAttachTest : public Test {
    const std::string requestId = "requestId";
    const std::string body = "body";
    const MetaPart headerStruct = macs::MimePartFactory()
            .hid("hid")
            .contentType("content_type")
            .contentSubtype("content_subtype")
            .boundary("boundary")
            .name("name")
            .charset("charset")
            .encoding("encoding")
            .contentDisposition("content_disposition")
            .fileName("file_name")
            .cid("cid")
            .offsetBegin(13)
            .offsetEnd(42)
            .release();
    const std::string zipArchiveEncoding = "utf-8";
    const Uid uid = "42";
    const Mid mid = "13";
    const Stid stid = "stid";
    const Stid rootStid = "root.stid";
    const Stid otherStid = "other.stid";
    const Hid hid = "1.2";
    const Hid hid1 = "1.3";
    const Hid hid2 = "1.4";
    const Hid rootHid = "1.5";
    const Hid otherHid = "1.6";
    const Hid otherStidHid = "1";

    MessageAccessMockPtr messageAccess;
    MessageAccessMockPtr rootMessageAccess;
    MessageAccessMockPtr otherMessageAccess;
    HoundClientMockPtr houndClient;
    const MakeMessageAccessMock makeMessageAccess;
    WrapperTestImpl recognizer;

    MakeAttachTest() {
        messageAccess = std::make_shared<MessageAccessMock>();
        houndClient = std::make_shared<HoundClientMock>();
        rootMessageAccess = std::make_shared<MessageAccessMock>();
        otherMessageAccess = std::make_shared<MessageAccessMock>();
    }
};

TEST_F(MakeAttachTest, call_for_old_id_without_zip_archive_encoding_should_return_attach) {
    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*messageAccess, getBody(hid)).WillOnce(Return(body));
        EXPECT_CALL(*messageAccess, getHeaderStruct(hid)).WillOnce(Return(headerStruct));

        const Attach result = makeAttach(part_id::Old {stid, hid});
        Attach expected;
        expected.content = body;
        expected.filename = headerStruct.fileName();
        expected.type = headerStruct.contentType();
        expected.subtype = headerStruct.contentSubtype();
        expected.charset = headerStruct.charset();
        EXPECT_EQ(result, expected);
    });
}

TEST_F(MakeAttachTest, call_for_old_id_without_zip_archive_encoding_but_get_body_or_get_header_struct_throws_exception_should_throw_exception) {
    const part_id::Old partId {stid, hid};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, zipArchiveEncoding,
                                    makeMessageAccess);

        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        ON_CALL(*messageAccess, getBody(hid)).WillByDefault(Throw(std::exception()));
        ON_CALL(*messageAccess, getHeaderStruct(hid)).WillByDefault(Throw(std::exception()));

        EXPECT_THROW(makeAttach(partId), std::exception);
    });
}

TEST_F(MakeAttachTest, call_for_old_id_with_zip_archive_encoding_should_return_attach_with_zip_encoded_content) {
    withIoService(requestId, [&] (auto context) {

    const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, zipArchiveEncoding,
                                makeMessageAccess);
        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*messageAccess, getBody(hid1)).WillOnce(Return("a"));
        EXPECT_CALL(*messageAccess, getBody(hid2)).WillOnce(Return("b"));
        EXPECT_CALL(*messageAccess, getHeaderStruct(hid1)).WillOnce(Return(headerStruct));
        EXPECT_CALL(*messageAccess, getHeaderStruct(hid2)).WillOnce(Return(headerStruct));

        const Attach result = makeAttach(part_id::Old {stid, hid1 + "," + hid2});

        EXPECT_THAT(result.content, StartsWith("PK"));
        EXPECT_EQ(result.type, "application");
        EXPECT_EQ(result.subtype, "zip");
    });
}

TEST_F(MakeAttachTest, call_for_old_id_with_zip_archive_encoding_but_get_body_or_get_header_struct_throws_exception_should_throw_exception) {
    const part_id::Old partId {stid, hid1 + "," + hid2};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, zipArchiveEncoding,
                                    makeMessageAccess);

        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        ON_CALL(*messageAccess, getBody(hid1)).WillByDefault(Throw(std::exception()));
        ON_CALL(*messageAccess, getHeaderStruct(hid1)).WillByDefault(Throw(std::exception()));

        EXPECT_THROW(makeAttach(partId), std::exception);
    });
}

TEST_F(MakeAttachTest, call_for_temporary_id_should_return_attach) {
    withIoService(requestId, [&] (auto context) {

    const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none, makeMessageAccess);
        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*messageAccess, getBody(hid)).WillOnce(Return(body));
        EXPECT_CALL(*messageAccess, getHeaderStruct(hid)).WillOnce(Return(headerStruct));

        const Attach result = makeAttach(part_id::Temporary {stid, hid});
        Attach expected;
        expected.content = body;
        expected.filename = headerStruct.fileName();
        expected.type = headerStruct.contentType();
        expected.subtype = headerStruct.contentSubtype();
        expected.charset = headerStruct.charset();
        EXPECT_EQ(result, expected);
    });
}

TEST_F(MakeAttachTest, call_for_single_message_part_id_but_get_message_parts_throws_exception_should_throw_exception) {
    const part_id::SingleMessagePart partId {uid, mid, hid};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid))
            .WillOnce(Throw(std::runtime_error("")));

        EXPECT_THROW(makeAttach(partId), std::runtime_error);
    });
}

TEST_F(MakeAttachTest, call_for_single_message_part_id_but_no_message_parts_should_throw_exception) {
    const part_id::SingleMessagePart partId {uid, mid, hid};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(boost::none));

        EXPECT_THROW(makeAttach(partId), NoMessageParts);
    });
}

TEST_F(MakeAttachTest, call_for_single_message_part_id_with_part_for_root_hid_should_return_attach_without_calling_get_header_struct) {
    const MessageParts messageParts {
        RootMessagePart {stid, StidHidMimeParts {{hid, headerStruct}}},
        StidMessageParts {}
    };
    const part_id::SingleMessagePart partId {uid, mid, hid};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts({headerStruct}), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(messageParts));
        EXPECT_CALL(*messageAccess, getBody(hid)).WillOnce(Return(body));
        EXPECT_CALL(*messageAccess, getHeaderStruct(_)).Times(0);

        const Attach result = makeAttach(partId);
        Attach expected;
        expected.content = body;
        expected.filename = headerStruct.fileName();
        expected.type = headerStruct.contentType();
        expected.subtype = headerStruct.contentSubtype();
        expected.charset = headerStruct.charset();
        EXPECT_EQ(result, expected);
    });
}

TEST_F(MakeAttachTest, call_for_single_message_part_id_without_part_for_root_hid_should_return_attach_with_calling_get_header_struct) {
    const MessageParts messageParts {
        RootMessagePart {stid, StidHidMimeParts {}},
        StidMessageParts {}
    };
    const part_id::SingleMessagePart partId {uid, mid, hid};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(messageParts));
        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts(), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*messageAccess, getBody(hid)).WillOnce(Return(body));
        EXPECT_CALL(*messageAccess, getHeaderStruct(hid)).WillOnce(Return(headerStruct));

        const Attach result = makeAttach(partId);
        Attach expected;
        expected.content = body;
        expected.filename = headerStruct.fileName();
        expected.type = headerStruct.contentType();
        expected.subtype = headerStruct.contentSubtype();
        expected.charset = headerStruct.charset();
        EXPECT_EQ(result, expected);
    });
}

TEST_F(MakeAttachTest, call_for_single_message_part_id_with_part_for_other_hid_should_return_attach_without_calling_get_header_struct) {
    const MessageParts messageParts {
        RootMessagePart {"root.stid", StidHidMimeParts {}},
        StidMessageParts {{stid, StidHidMessageParts {{hid, StidHidMessagePart {otherStidHid, headerStruct}}}}}
    };
    const part_id::SingleMessagePart partId {uid, mid, hid};
    const auto mimePart = macs::MimePartFactory(headerStruct).hid(otherStidHid).release();

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(messageParts));
        EXPECT_CALL(*makeMessageAccess.impl, call(stid, macs::MimeParts({mimePart}), _, _, _)).WillOnce(Return(messageAccess));
        EXPECT_CALL(*messageAccess, getBody(otherStidHid)).WillOnce(Return(body));
        EXPECT_CALL(*messageAccess, getHeaderStruct(_)).Times(0);

        const Attach result = makeAttach(partId);
        Attach expected;
        expected.content = body;
        expected.filename = headerStruct.fileName();
        expected.type = headerStruct.contentType();
        expected.subtype = headerStruct.contentSubtype();
        expected.charset = headerStruct.charset();
        EXPECT_EQ(result, expected);
    });
}

TEST_F(MakeAttachTest, call_for_multiple_message_part_id_when_zip_archive_encoding_in_not_set_should_throw_exception) {
    const part_id::MultipleMessagePart partId {uid, mid, {rootHid, otherHid}};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, boost::none,
                                    makeMessageAccess);

        EXPECT_THROW(makeAttach(partId), std::invalid_argument);
    });
}

TEST_F(MakeAttachTest, call_for_multiple_message_part_id_but_no_message_parts_should_throw_exception) {
    const part_id::MultipleMessagePart partId {uid, mid, {hid1, hid2}};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, zipArchiveEncoding,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(boost::none));

        EXPECT_THROW(makeAttach(partId), NoMessageParts);
    });
}

TEST_F(MakeAttachTest, call_for_multiple_message_part_id_should_return_attach_with_zip_encoded_content) {
    const MessageParts messageParts {
        RootMessagePart {rootStid, StidHidMimeParts {{rootHid, headerStruct}}},
        StidMessageParts {{otherStid, StidHidMessageParts {{otherHid, StidHidMessagePart {otherStidHid, headerStruct}}}}}
    };
    const auto otherMimePart = macs::MimePartFactory(headerStruct).hid(otherStidHid).release();
    const part_id::MultipleMessagePart partId {uid, mid, {rootHid, otherHid}};

    withIoService(requestId, [&] (auto context) {
        const MakeAttach makeAttach(context, StorageServicePtr(), houndClient, recognizer, zipArchiveEncoding,
                                    makeMessageAccess);

        EXPECT_CALL(*houndClient, getMessageParts(_, uid, mid)).WillOnce(Return(messageParts));
        EXPECT_CALL(*makeMessageAccess.impl, call(rootStid, macs::MimeParts({headerStruct}), _, _, _))
                .WillOnce(Return(rootMessageAccess));
        EXPECT_CALL(*makeMessageAccess.impl, call(otherStid, macs::MimeParts({otherMimePart}), _, _, _))
                .WillOnce(Return(otherMessageAccess));
        EXPECT_CALL(*rootMessageAccess, getBody(rootHid)).WillOnce(Return("a"));
        EXPECT_CALL(*otherMessageAccess, getBody(otherStidHid)).WillOnce(Return("b"));

        const Attach result = makeAttach(partId);

        EXPECT_THAT(result.content, StartsWith("PK"));
        EXPECT_EQ(result.type, "application");
        EXPECT_EQ(result.subtype, "zip");
    });
}

} // namespace
