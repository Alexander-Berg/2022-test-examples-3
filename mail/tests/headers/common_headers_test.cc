#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/common_headers.h>
#include <mail/sendbernar/core/include/account.h>
#include "mock_rfc_message.h"
#include <mail/sendbernar/composer/tests/mock/metadata.h>
#include <macs/label_factory.h>
#include <mail/http_getter/client/mock/mock.h>

using namespace testing;

namespace sendbernar {

using namespace http_getter;

std::set<std::string> domainsNotToLowercase;

RecipientsHeaders makeRecipients() {
    params::SaveDraft p;
    p.recipients = params::Recipients {
        std::string(), std::string(), std::string()
    };
    return RecipientsHeaders(domainsNotToLowercase, 1, p);
}

struct CommonHeadersTest: public ::testing::Test {
    const std::string emptyMobileCaller = "";
    const std::string mobileCaller = "mobile";
    const std::string fromName = "fromName";
    const std::string defaultEmail = "default@ya.ru";
    const std::time_t now = 100500;
    const std::string hostname = "hostname.yandex.net";
    const bool lowPriority = false;
    const bool highPriority = true;
    const std::time_t sendDate = 0;
    const unsigned maxRecipients = 0;
    const bool draft = true;
    const bool doNotDraft = false;
    const macs::Label label = macs::LabelFactory().lid("lid").symbol(macs::Label::Symbol::important_label).product();
    RecipientsHeaders recipients = makeRecipients();

    Account account;
    Settings config;
    std::shared_ptr<NiceMock<MockRfcMessage>> rfcMessage;
    std::shared_ptr<StrictMock<tests::MockMetadata>> metadata;

    void SetUp() override {
        account = Account {
            .login = "login",
            .domain = "yandex.ru"
        };
        rfcMessage = std::make_shared<NiceMock<MockRfcMessage>>();

        metadata = std::make_shared<StrictMock<tests::MockMetadata>>();
    }

    LazySettings profile() {
        return LazySettings(
            account,
            nullptr,
            getContextLogger("", boost::none),
            Settings::Values {
                .defaultEmail = "default@ya.ru",
                .fromName = "fromName"
            }
        );
    }
};

struct HeaderMessageIdTest: public ::testing::Test {
    const std::string validMessageId = "<valid@messageid>";
    const std::string validMessageId2 = "<valid2@messageid>";
    const std::string invalidMessageId = "valid@messageid";
    const std::string emptyMessageId = "";
};

TEST_F(HeaderMessageIdTest, checkValidateHeaderMessageIdCases) {
    EXPECT_FALSE(validateHeaderMessageId(""));
    EXPECT_FALSE(validateHeaderMessageId(std::string(' ', MESSAGE_ID_HEADER_MAX_LENGTH)));
    EXPECT_FALSE(validateHeaderMessageId("< without > at the end"));
    EXPECT_FALSE(validateHeaderMessageId("without < at the start >"));
    EXPECT_FALSE(validateHeaderMessageId("<@@>"));
    EXPECT_FALSE(validateHeaderMessageId("<>"));

    EXPECT_FALSE(validateHeaderMessageId(invalidMessageId));
    EXPECT_FALSE(validateHeaderMessageId(emptyMessageId));
    EXPECT_TRUE(validateHeaderMessageId(validMessageId));
    EXPECT_TRUE(validateHeaderMessageId(validMessageId2));

    EXPECT_TRUE(validateHeaderMessageId("<ololo@ya.ru>"));
}

TEST_F(HeaderMessageIdTest, shouldChooseParamsMessageId) {
    params::SendMessage p;
    p.message.message_id = validMessageId;
    EXPECT_EQ(HeaderMessageId(validMessageId2, p).get(), validMessageId);
}

TEST_F(HeaderMessageIdTest, shouldChooseSourceMidMessageId) {
    params::SendMessage p;
    p.message.message_id = invalidMessageId;
    EXPECT_EQ(HeaderMessageId(validMessageId, p).get(), validMessageId);

    EXPECT_EQ(HeaderMessageId(validMessageId, params::SendMessage{}).get(), validMessageId);

    EXPECT_EQ(HeaderMessageId(invalidMessageId, params::SendMessage{}).get(), invalidMessageId);
}

TEST_F(HeaderMessageIdTest, shouldGenerateMessageId) {
    const auto generate = [] () {
        return "my_message_id";
    };

    params::SendMessage p;
    p.message.message_id = invalidMessageId;
    EXPECT_EQ(HeaderMessageId(emptyMessageId, p, generate).get(), generate());
}

TEST_F(CommonHeadersTest, checkValidateCurrentTime) {
    constexpr unsigned ALLOWED_DIFFERENCE = MAX_DIFFERENCE_BETWEEN_STD_TIME_AND_PASSED_PARAM - 1;
    constexpr unsigned UNALLOWED_DIFFERENCE = MAX_DIFFERENCE_BETWEEN_STD_TIME_AND_PASSED_PARAM;

    EXPECT_EQ(validateCurrentTime(now, boost::none), false);

    EXPECT_EQ(validateCurrentTime(now, now + ALLOWED_DIFFERENCE), true);
    EXPECT_EQ(validateCurrentTime(now, now - ALLOWED_DIFFERENCE), true);

    EXPECT_EQ(validateCurrentTime(now, now + UNALLOWED_DIFFERENCE), false);
    EXPECT_EQ(validateCurrentTime(now, now - UNALLOWED_DIFFERENCE), false);
}

TEST_F(CommonHeadersTest, shouldReturnNonEmptySubjectWhenThereIsNoSubject) {
    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), params::SendMessage());

    EXPECT_EQ(h.nonEmptySubject(), "No subject");
}

TEST_F(CommonHeadersTest, shouldReturnNonEmptySubject) {
    params::SendMessage p;
    p.message.subj = "subj";

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), p);

    EXPECT_EQ(h.nonEmptySubject(), "subj");
}

TEST_F(CommonHeadersTest, shouldUseUserDomainInRealMailbox) {
    account.login = "login";
    account.domain = "ya.ru";

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "yandex.ru", profile(), params::SendMessage());
    EXPECT_EQ(h.realMailbox(), "login@ya.ru");
}

TEST_F(CommonHeadersTest, shouldUseAuthDomainInRealMailbox) {
    account.login = "login";
    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "yandex.ru", profile(), params::SendMessage());
    EXPECT_EQ(h.realMailbox(), "login@yandex.ru");
}

TEST_F(CommonHeadersTest, shouldReturnFromMailboxIfOneIsPresentInAddresses) {
    account.addresses = { defaultEmail };

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), params::SendMessage());
    EXPECT_EQ(h.envelopeFrom(), defaultEmail);
}

TEST_F(CommonHeadersTest, shouldReturnRealMailboxInsteadOfFromMailbox) {
    account.login = "login";
    account.addresses = { "foo@yandex.ru" };

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "yandex.ru", profile(), params::SendMessage());
    EXPECT_EQ(h.envelopeFrom(), "login@yandex.ru");
}

TEST_F(CommonHeadersTest, shouldSetFromMailboxFromComposeInfo) {
    const std::string address = "from@yandex.ru";
    account.addresses = { address };

    params::SendMessage p;
    p.sender.from_mailbox = address;

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), p);

    EXPECT_EQ(h.from(), "fromName <from@yandex.ru>");
}

TEST_F(CommonHeadersTest, shouldSetFromMailboxFromComposeInfoIfAddressIsValidated) {
    const std::string address = "from@yandex.ru";
    account.validated = { address };
    params::SendMessage p;
    p.sender.from_mailbox = address;

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), p);

    EXPECT_EQ(h.from(), "fromName <from@yandex.ru>");
}

TEST_F(CommonHeadersTest, shouldSetFromMailboxFromProfile) {
    account.addresses = { "address@ya.ru" };
    params::SendMessage p;
    p.sender.from_mailbox = "foo@ya.ru";

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", profile(), p);

    EXPECT_EQ(h.from(), "fromName <default@ya.ru>");
}

TEST_F(CommonHeadersTest, shouldNotAddEnvelopeFromIfItIsEqualsToFrom) {
    params::SendMessage p;
    p.sender.from_mailbox = account.login + "@" + account.domain;

    auto pr = profile();
    pr.get().defaultEmail = account.login + "@" + account.domain;

    CommonHeaders h(account, now, CheckCaptchaResult::doNotRequired, hostname, emptyMobileCaller, metadata, 0, "", pr, p);

    EXPECT_CALL(*rfcMessage, addEmailHeader("From", "fromName <login@yandex.ru>")).Times(1);

    ASSERT_EQ(h.apply(*rfcMessage, recipients, "messageId"), ComposeResult::DONE);
}

TEST_F(CommonHeadersTest, shouldSetNonemptyFieldsIfTheyAreNotEmpty) {
    EXPECT_CALL(*metadata, labelBySymbol(macs::Label::Symbol::important_label))
            .WillOnce(Return(label));

    params::SendMessage p;
    p.message.subj = "subject";
    p.references = "references";
    p.inreplyto = params::InReplyTo{.inreplyto = "inReplyTo"};
    p.lids = std::vector<std::string>{ "lid" };

    auto pr = profile();
    pr.get().defaultEmail = account.login + "@" + account.domain;

    CommonHeaders h(account, now, CheckCaptchaResult::good, hostname, mobileCaller, metadata, 0, "", pr, p);

    EXPECT_CALL(*rfcMessage, addUtf8Header("Subject", *p.message.subj)).Times(1);

    EXPECT_CALL(*rfcMessage, addHeader("Received", _)).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("In-Reply-To", p.inreplyto->inreplyto)).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("References", *p.references)).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("MIME-Version", "1.0")).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("X-Mailer", "Yamail [ http://yandex.ru ] 5.0")).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("Date", _)).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("X-Priority", "1")).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("X-Yandex-Mobile-Caller", mobileCaller)).Times(1);
    EXPECT_CALL(*rfcMessage, addHeader("X-Yandex-Captcha-Entered", "yes")).Times(1);

    ASSERT_EQ(h.apply(*rfcMessage, recipients, "messageId"), ComposeResult::DONE);
}

TEST(TimeToStringTest, canonizeTest) {
    EXPECT_EQ(timeToString(0), "Thu, 01 Jan 1970 03:00:00 +0300");
    EXPECT_EQ(timeToString(737996400), "Fri, 21 May 1993 18:00:00 +0300");
    EXPECT_EQ(timeToString(1402319655), "Mon, 09 Jun 2014 16:14:15 +0300");
    EXPECT_EQ(timeToString(1578682151), "Fri, 10 Jan 2020 21:49:11 +0300");
    EXPECT_EQ(timeToString(1924948855), "Tue, 31 Dec 2030 15:00:55 +0300");
}

}
