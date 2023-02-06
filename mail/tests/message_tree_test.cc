
#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/message_access_mock.h>
#include <mail_getter/content_type_mock.h>
#include <macs/mime_part_factory.h>

#include <internal/message_tree.h>

namespace macs {

std::ostream & operator << (std::ostream & s, const MimePart& v) {
    s << v.hid() << ", " << v.boundary() << ", " << v.charset() << ", " <<  v.cid() << ", " <<
            v.contentDisposition() << ", " << v.contentType() << ", " <<  v.contentSubtype() <<
            ", " << v.encoding() << ", " << v.fileName() << ", " <<  v.name() << ", " <<
            v.offsetBegin() << ", " << v.offsetEnd();
    return s;
}

bool operator == (const MimePart& l, const MimePart& r) {
    return l.hid() == r.hid() && l.boundary() == r.boundary() && l.charset() == r.charset() &&
            l.cid() == r.cid() && l.contentDisposition() == r.contentDisposition() &&
            l.contentType() == r.contentType() && l.contentSubtype() == r.contentSubtype() &&
            l.encoding() == r.encoding() && l.fileName() == r.fileName() && l.name() == r.name() &&
            l.offsetBegin() == r.offsetBegin() && l.offsetEnd() == r.offsetEnd();
}

}

namespace {

using namespace testing;
using namespace msg_body;

TEST(LocalHidPartTest, localHidPart_hidWithPoints_returnLastHid) {
    const std::string hid = "1.2.3";
    ASSERT_EQ(3ul, localHidPart(hid));
}

TEST(LocalHidPartTest, localHidPart_hidWithOutPoints_returnHidLexicalCast) {
    const std::string hid = "123";
    ASSERT_EQ(123ul, localHidPart(hid));
}

TEST(LocalHidPartTest, localHidPart_emptyHid_throwBadLexicalCast) {
    const std::string hid = "";
    ASSERT_THROW(localHidPart(hid), MessageTreeException);
}

TEST(LocalHidPartTest, localHidPart_hidWithPointAsLastCharacter_throwBadLexicalCast) {
    const std::string hid = "1.2.";
    ASSERT_THROW(localHidPart(hid), MessageTreeException);
}

class MessageTreeTest: public Test {
public:
    MessageTreeTest()
        : creator_(maMock_, detectorMock_, {})
        , stid_("5208.37381057.3433967167144031187025150241873")
    {
        EXPECT_CALL(maMock_, getBody(_)).WillRepeatedly(Return(std::string()));
    }

    MessageAccessMock maMock_;
    ContentTypeDetectorMock detectorMock_;
    MessageTreeCreator creator_;
    MetaLevel emptyBody_;
    std::string stid_;
};

TEST_F(MessageTreeTest, testCreate_treeWithParts_createTree) {
    MetaPart header = macs::MimePartFactory().contentType("application")
            .contentSubtype("octet-stream").release();

    MetaLevel body;
    body.push_back("1.1");
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(header));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(body)));

    MetaLevel subPartBody;
    subPartBody.push_back("1.1.1");
    subPartBody.push_back("1.1.2");

    EXPECT_CALL(maMock_, getHeaderStruct("1.1")).WillOnce(Return(header));
    EXPECT_CALL(maMock_, getBodyStruct("1.1")).WillOnce(Return(MetaLevel(subPartBody)));

    EXPECT_CALL(maMock_, getHeaderStruct("1.1.1")).WillOnce(Return(header));
    EXPECT_CALL(maMock_, getBodyStruct("1.1.1")).WillOnce(Return(MetaLevel(emptyBody_)));

    EXPECT_CALL(maMock_, getHeaderStruct("1.1.2")).WillOnce(Return(header));
    EXPECT_CALL(maMock_, getBodyStruct("1.1.2")).WillOnce(Return(MetaLevel(emptyBody_)));

    EXPECT_CALL(maMock_, getStId()).Times(4).WillRepeatedly(ReturnRef(stid_));

    MessageTree messageTree = creator_.create("1");
    const MessagePart& messagePart = messageTree.part;

    ASSERT_EQ("1", messagePart.hid);
    ASSERT_EQ(stid_, messagePart.stid);
    ASSERT_EQ(header, messagePart.headerStruct);
    ASSERT_EQ(body, messagePart.bodyStruct);
    ASSERT_EQ(1ul, messageTree.children.size());

    MessageTree::Children::const_iterator iterSubTree = messageTree.children.begin();
    ASSERT_EQ(1ul, iterSubTree->first);
    const MessageTree& subTree = *iterSubTree->second;
    const MessagePart& subPart = subTree.part;
    ASSERT_EQ("1.1", subPart.hid);
    ASSERT_EQ(stid_, messagePart.stid);
    ASSERT_EQ(header, subPart.headerStruct);
    ASSERT_EQ(subPartBody, subPart.bodyStruct);
    ASSERT_EQ(2ul, subTree.children.size());

    MessageTree::Children::const_iterator iterFirstSubSubTree = subTree.children.find(1);
    ASSERT_EQ(1ul, iterFirstSubSubTree->first);
    const MessageTree& firstSubSubTree = *iterFirstSubSubTree->second;
    const MessagePart& firstSubSubPart = firstSubSubTree.part;
    ASSERT_EQ("1.1.1", firstSubSubPart.hid);
    ASSERT_EQ(stid_, messagePart.stid);
    ASSERT_EQ(header, firstSubSubPart.headerStruct);
    ASSERT_EQ(emptyBody_, firstSubSubPart.bodyStruct);

    MessageTree::Children::const_iterator iterSecondSubSubTree = subTree.children.find(2);
    ASSERT_EQ(2ul, iterSecondSubSubTree->first);
    const MessageTree& secondSubSubTree = *iterSecondSubSubTree->second;
    const MessagePart& secondSubSubPart = secondSubSubTree.part;
    ASSERT_EQ("1.1.2", secondSubSubPart.hid);
    ASSERT_EQ(stid_, messagePart.stid);
    ASSERT_EQ(header, secondSubSubPart.headerStruct);
    ASSERT_EQ(emptyBody_, secondSubSubPart.bodyStruct);
}

TEST_F(MessageTreeTest, testCreate_headerWithContentType_setMimeType) {
    MetaPart headerStruct = macs::MimePartFactory().contentType("text")
            .contentSubtype("html").release();

    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));

    MessageTree messageTree = creator_.create("1");
    ASSERT_EQ(MimeType("text", "html"), messageTree.part.contentType);
}

TEST_F(MessageTreeTest, testCreate_headerWithoutContentType_useDefaultMimeType) {
    MetaPart headerStruct;
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));

    MessageTree messageTree = creator_.create("1");
    ASSERT_EQ(MimeType(), messageTree.part.contentType);
}

TEST_F(MessageTreeTest, testCreate_headerWithFilename_detectMimeTypeByFilename) {
    MetaPart headerStruct = macs::MimePartFactory().contentType("application")
            .contentSubtype("octet-stream").name("file.docx").release();
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));
    EXPECT_CALL(detectorMock_, detect("file.docx", "")).WillOnce(Return(MimeType("application", "msword")));

    MessageTree messageTree = creator_.create("1");
    ASSERT_EQ(MimeType("application", "msword"), messageTree.part.contentType);
}

TEST_F(MessageTreeTest, testCreate_headerWithFilenameAndMime_detectMimeTypeByFilename) {
    MetaPart headerStruct = macs::MimePartFactory().contentType("application")
            .contentSubtype("zip").name("file.docx").release();
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));
    EXPECT_CALL(detectorMock_, detect("file.docx", "")).WillOnce(Return(MimeType("application", "msword")));

    MessageTree messageTree = creator_.create("");
    ASSERT_EQ(MimeType("application", "msword"), messageTree.part.contentType);
}

TEST_F(MessageTreeTest, testCreate_headerWithStupidFilenameAndMime_useOldContentType) {
    MetaPart headerStruct = macs::MimePartFactory().contentType("image")
            .contentSubtype("jpeg").name("image.ggg").release();
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));
    EXPECT_CALL(detectorMock_, detect("image.ggg", "")).WillOnce(Return(MimeType("application", "octet-stream")));

    MessageTree messageTree = creator_.create("");
    ASSERT_EQ(MimeType("image", "jpeg"), messageTree.part.contentType);
}

TEST_F(MessageTreeTest, testCreate_contentTypeRfc822_appendInlineMessagePart) {
    MetaPart headerStruct = macs::MimePartFactory().contentType("message")
            .contentSubtype("rfc822").release();
    EXPECT_CALL(maMock_, getHeaderStruct("1")).WillOnce(Return(headerStruct));
    EXPECT_CALL(maMock_, getBodyStruct("1")).WillOnce(Return(MetaLevel(emptyBody_)));
    EXPECT_CALL(maMock_, getStId()).WillOnce(ReturnRef(stid_));

    MetaAttributes inlineHeaderStruct;
    inlineHeaderStruct["content_type.type"] = "text";
    inlineHeaderStruct["content_type.subtype"] = "html";
    EXPECT_CALL(maMock_, getMessageHeaderParsed("1.1")).WillOnce(Return(MetaAttributes(inlineHeaderStruct)));

    MessageTree messageTree = creator_.create("1");
    ASSERT_EQ(stid_, messageTree.part.stid);
    ASSERT_EQ(inlineHeaderStruct, messageTree.part.messageHeader);
    ASSERT_TRUE(messageTree.children.empty());
}

class MessageTreeAttachmentSizeTest : public TestWithParam<std::pair<std::vector<macs::AttachmentDescriptor>, std::size_t>> {};
INSTANTIATE_TEST_SUITE_P(TestAttachSize, MessageTreeAttachmentSizeTest, Values(
        std::make_pair(std::vector<macs::AttachmentDescriptor>{}, 2ul),
        std::make_pair(std::vector<macs::AttachmentDescriptor>{{"1.1", "txt", "filename", 200ul}}, 200ul)
));

TEST_P(MessageTreeAttachmentSizeTest, shouldReturnAttachmentSizeFromOffsets) {
    auto [attachments, expectedSize] = GetParam();

    MessageAccessMock maMock;
    ContentTypeDetectorMock detectorMock;
    MessageTreeCreator creator(maMock, detectorMock, std::move(attachments));
    const std::string stid = "34.12.65903";
    const MetaPart header = macs::MimePartFactory().contentType("application")
            .contentSubtype("octet-stream").offsetBegin(23).offsetEnd(25).release();

    EXPECT_CALL(maMock, getStId()).WillOnce(ReturnRef(stid));
    EXPECT_CALL(maMock, getHeaderStruct("1.1")).WillOnce(Return(header));

    MessageTree messageTree = creator.create("1.1");
    MessagePartTypeInfo typeInfo = getTypeInfo(messageTree.part, detectorMock, AliasClassList());
    EXPECT_EQ(typeInfo.length, expectedSize);
}

} // <anonymous> namespace
