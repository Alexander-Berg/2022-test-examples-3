#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/envelope/envelope.h>
#include <macs/envelope_factory.h>
#include <internal/label/fake.h>
#include <boost/utility.hpp>
#include <boost/tuple/tuple_comparison.hpp>
#include <boost/tuple/tuple_io.hpp>
#include "mapper_mock.h"

namespace {

using namespace testing;

using macs::Envelope;
using macs::EnvelopeFactory;
using macs::AttachmentDescriptor;
using macs::Label;
using Symbol = Label::Symbol;
using macs::pg::getFakeLabelId;
using macs::pg::MessageAttributes;
using macs::pg::MessageAttach;
using macs::pg::Recipient;

struct EnvelopeHelperTest : public Test {
    MockMapper mapper;
    EnvelopeFactory factory;
    struct Helper : public pgg::query::Helper<Helper, Envelope> {};
    const std::string & lid(const Symbol & s) const {
        return getFakeLabelId(s);
    }
};

#define EXPECT_CALL_ONCE_T( m, call ) EXPECT_CALL(m->mock(), call ).WillOnce(Return())

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolSeen_mapSeenWithTrue) {
    factory.addLabelID(lid(Symbol::seen_label));

    const auto mock = mapper.mapValueMock<bool>();
    EXPECT_CALL_ONCE_T(mock, mapValue(true, "seen"));
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "deleted"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withNoFakeLabelSymbolSeen_mapSeenWithFalse) {
    const auto mock = mapper.mapValueMock<bool>();
    EXPECT_CALL_ONCE_T(mock, mapValue(false, "seen"));
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "deleted"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolDeleted_mapDeletedWithTrue) {
    factory.addLabelID(lid(Symbol::deleted_label));

    const auto mock = mapper.mapValueMock<bool>();
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "seen"));
    EXPECT_CALL_ONCE_T(mock, mapValue(true, "deleted"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withNoFakeLabelSymbolDeleted_mapDeletedWithFalse) {
    const auto mock = mapper.mapValueMock<bool>();
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "seen"));
    EXPECT_CALL_ONCE_T(mock, mapValue(false, "deleted"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolPostmaster_mapPostmasterAttribute) {
    factory.addLabelID(lid(Symbol::postmaster_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::postmaster}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolSpam_mapSpamAttribute) {
    factory.addLabelID(lid(Symbol::spam_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::spam}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolMulcaShared_mapMulcaSharedAttribute) {
    factory.addLabelID(lid(Symbol::mulcaShared_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::mulcaShared}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolAppend_mapAppendAttribute) {
    factory.addLabelID(lid(Symbol::append_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::append}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolCopy_mapCopyAttribute) {
    factory.addLabelID(lid(Symbol::copy_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::copy}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withFakeLabelSymbolSynced_mapSyncedAttribute) {
    factory.addLabelID(lid(Symbol::synced_label));

    using Attrs = std::vector<MessageAttributes>;
    const auto mock = mapper.mapValueMock<Attrs>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attrs{MessageAttributes::synced}, "attributes"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withAttachmentDescriptor_mapAttachments) {
    factory.addAttachment(AttachmentDescriptor("hid", "contentType", "fileName", 1024));

    using Attachments = std::vector<boost::tuple<
            std::string,std::string,std::string,std::size_t>>;
    const auto mock = mapper.mapValueMock<Attachments>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Attachments{{"hid", "contentType", "fileName",
        1024UL}}, "attaches"));
    Helper().envelope(factory.release()).map(mapper);
}

// If recipients from some header HEADER have been already parsed and dumped into field parsedHEADER
// Then we prefer recipients from this field rather than from std::string HEADER field
struct RecipientsHeaderHelperTest :
    public EnvelopeHelperTest,
    public WithParamInterface<Recipient::Type>
{
    void fillStringRecipientsField() {
        std::string strRecipient = "Vasya <vasya@hell.ya>";

        const auto& type = GetParam();
        switch (type) {
            case Recipient::Type::from:
                factory.from(std::move(strRecipient));
                break;
            case Recipient::Type::to:
                factory.to(std::move(strRecipient));
                break;
            case Recipient::Type::cc:
                factory.cc(std::move(strRecipient));
                break;
            case Recipient::Type::bcc:
                factory.bcc(std::move(strRecipient));
                break;
            case Recipient::Type::replyTo:
                factory.replyTo(std::move(strRecipient));
                break;
            case Recipient::Type::sender:
                factory.sender(std::move(strRecipient));
                break;
            default:
                break;
        }
    }

    void fillParsedRecipientsField() {
        std::vector<Email> emails;
        emails.emplace_back("parseduser1", "hell.ya", "Parsed user 1");
        emails.emplace_back("parseduser2", "hell.ya", "Parsed user 2");

        const auto& type = GetParam();
        switch (type) {
            case Recipient::Type::from:
                factory.parsedFrom(std::move(emails));
                break;
            case Recipient::Type::to:
                factory.parsedTo(std::move(emails));
                break;
            case Recipient::Type::cc:
                factory.parsedCc(std::move(emails));
                break;
            case Recipient::Type::bcc:
                factory.parsedBcc(std::move(emails));
                break;
            case Recipient::Type::replyTo:
                factory.parsedReplyTo(std::move(emails));
                break;
            case Recipient::Type::sender:
                factory.parsedSender(std::move(emails));
                break;
            default:
                break;
        }
    }

    using Recipients = std::vector<Recipient::Tuple>;

    Recipients getRecipientsSameAsInStringField() {
        const auto& type = GetParam();
        return Recipients{{type, "Vasya", "vasya@hell.ya"}};
    }

    Recipients getRecipientsSameAsInParsedField() {
        const auto& type = GetParam();
        return Recipients{
            {type, "Parsed user 1", "parseduser1@hell.ya"},
            {type, "Parsed user 2", "parseduser2@hell.ya"},
        };
    }

};

TEST_P(RecipientsHeaderHelperTest, have_string_header_and_dont_have_parsed_header) {
    fillStringRecipientsField();
    const auto mock = mapper.mapValueMock<Recipients>();
    EXPECT_CALL_ONCE_T(mock, mapValue(getRecipientsSameAsInStringField(), "recipients"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_P(RecipientsHeaderHelperTest, have_string_header_and_have_parsed_header) {
    fillStringRecipientsField();
    fillParsedRecipientsField();
    const auto mock = mapper.mapValueMock<Recipients>();
    EXPECT_CALL_ONCE_T(mock, mapValue(getRecipientsSameAsInParsedField(), "recipients"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_P(RecipientsHeaderHelperTest, dont_have_string_header_and_have_parsed_header) {
    fillParsedRecipientsField();
    const auto mock = mapper.mapValueMock<Recipients>();
    EXPECT_CALL_ONCE_T(mock, mapValue(getRecipientsSameAsInParsedField(), "recipients"));
    Helper().envelope(factory.release()).map(mapper);
}

INSTANTIATE_TEST_SUITE_P(
    TestThatWePreferRecipientsFromParsedHeaderIfWeCan,
    RecipientsHeaderHelperTest,
    Values(
        Recipient::Type::to,
        Recipient::Type::from,
        Recipient::Type::cc,
        Recipient::Type::bcc,
        Recipient::Type::replyTo,
        Recipient::Type::sender
    )
);

TEST_F(EnvelopeHelperTest, envelope_withNonZeroTid_mapVariantTid) {
    factory.threadId("33");

    using Tid = boost::optional<std::string>;
    const auto mock = mapper.mapValueMock<Tid>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Tid("33"), "tid"));
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "tab_type"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withZeroTid_mapEmptyVariantTid) {
    factory.threadId("0");

    using Tid = boost::optional<std::string>;
    const auto mock = mapper.mapValueMock<Tid>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Tid(), "tid"));
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "tab_type"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withEmptyTid_mapEmptyVariantTid) {
    factory.threadId("");

    using Tid = boost::optional<std::string>;
    const auto mock = mapper.mapValueMock<Tid>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Tid(), "tid"));
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "tab_type"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withOrdinaryFields_mapFieldsIntoVariables) {
    factory.mid("00000");
    factory.fid("1");
    factory.date(42);
    factory.receiveDate(43);
    factory.subject("subject here");
    factory.stid("0987654321");
    factory.firstline("Something important");
    factory.rfcId("message-id");
    factory.size(4096);
    factory.extraData("extra!");


    const auto strMock = mapper.mapValueMock<std::string>();
    const auto timeMock = mapper.mapValueMock<std::time_t>();
    const auto sizeMock = mapper.mapValueMock<std::size_t>();

    EXPECT_CALL_ONCE_T(strMock, mapValue("00000", "mid"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("1", "fid"));
    EXPECT_CALL_ONCE_T(timeMock, mapValue(42, "hdr_date"));
    EXPECT_CALL_ONCE_T(timeMock, mapValue(43, "received_date"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("subject here", "subject"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("0987654321", "st_id"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("Something important", "firstline"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("message-id", "hdr_message_id"));
    EXPECT_CALL_ONCE_T(sizeMock, mapValue(4096, "size"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("extra!", "extra_data"));

    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withLabelsLids_mapLids) {
    factory.addLabelID("1");
    factory.addLabelID("2");
    factory.addLabelID("3");

    using Lids = std::vector<std::string>;
    const auto mock = mapper.mapValueMock<Lids>();
    EXPECT_CALL_ONCE_T(mock, mapValue(Lids{"1","2","3"}, "lids"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withTab_mapTabType) {
    factory.tab(std::make_optional<macs::Tab::Type>(macs::Tab::Type::news));

    using TabType = boost::optional<std::string>;
    const auto mock = mapper.mapValueMock<TabType>();
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "tid"));
    EXPECT_CALL_ONCE_T(mock, mapValue(TabType("news"), "tab_type"));
    Helper().envelope(factory.release()).map(mapper);
}

TEST_F(EnvelopeHelperTest, envelope_withEmptyTab_mapEmptyTabType) {
    factory.tab(std::nullopt);

    using TabType = boost::optional<std::string>;
    const auto mock = mapper.mapValueMock<TabType>();
    EXPECT_CALL_ONCE_T(mock, mapValue(_, "tid"));
    EXPECT_CALL_ONCE_T(mock, mapValue(TabType(), "tab_type"));
    Helper().envelope(factory.release()).map(mapper);
}

} // namespace
