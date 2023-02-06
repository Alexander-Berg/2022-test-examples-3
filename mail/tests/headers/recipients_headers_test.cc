#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/recipients_headers.h>
#include <mail/sendbernar/core/include/account.h>
#include "mock_rfc_message.h"

inline bool operator==(const Email& e1, const Email& e2) {
    return e1.displayName() == e2.displayName() && e1.local() == e2.local() && e1.domain() == e2.domain();
}

inline std::ostream& operator<<(std::ostream& out, const Email& e) {
    out << "local: '" << e.local() << "' domain: '" << e.domain()
        << "' name: '" << e.displayName() << "'" << std::endl;
    return out;
}


using namespace testing;
using namespace std::literals;

namespace sendbernar {

struct RecipientsHeadersTest: public ::testing::Test {
    const unsigned maxRecipients = 10;
    const bool draft = true;
    const bool doNotDraft = false;

    std::shared_ptr<StrictMock<MockRfcMessage>> rfcMessage;
    std::string usual = "to@ya.ru";
    std::string another = "another@ya.ru";
    std::string wrong = "какая-то дичь";
    std::string empty = "";
    std::set<std::string> notToLowercase;

    void SetUp() override {
        rfcMessage = std::make_shared<StrictMock<MockRfcMessage>>();
    }
};


TEST_F(RecipientsHeadersTest, shouldSetToCcBccHeadersToMessage) {
    params::SendMessage p;
    p.recipients = params::Recipients {
        boost::make_optional("to1@ya.ru,to2@ya.ru"s),
        boost::make_optional("cc1@ya.ru, cc2@ya.ru"s),
        boost::make_optional("bcc1@ya.ru, \"Bcc\" bcc2@ya.ru"s)
    };

    RecipientsHeaders h(notToLowercase, maxRecipients, p);

    EXPECT_CALL(*rfcMessage, addEmailHeader("To", "to1@ya.ru,to2@ya.ru")).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Cc", "cc1@ya.ru,cc2@ya.ru")).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Bcc", "bcc1@ya.ru,Bcc <bcc2@ya.ru>")).Times(1);

    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::DONE);

    const RecipientsRepository& recipients = h.recipients();
    EXPECT_THAT(recipients.to(), UnorderedElementsAre(Email("to1", "ya.ru"), Email("to2", "ya.ru")));
    EXPECT_THAT(recipients.cc(), UnorderedElementsAre(Email("cc1", "ya.ru"), Email("cc2", "ya.ru")));
    EXPECT_THAT(recipients.bcc(), UnorderedElementsAre(Email("bcc1", "ya.ru"), Email("bcc2", "ya.ru", "Bcc")));
}

TEST_F(RecipientsHeadersTest, shouldNotLowercaseAddressesWithSpecialDomains) {
    params::SendMessage p;
    p.recipients = params::Recipients {
        boost::make_optional("to1@ya.ru,TO2@Yandex.ru"s),
        boost::make_optional("cc1@ya.ru, CC2@Yandex.ru"s),
        boost::make_optional("bcc1@ya.ru, \"Bcc\" BcC2@Yandex.ru"s)
    };

    std::set<std::string> domains = {"Yandex.ru"};

    RecipientsHeaders h(domains, maxRecipients, p);

    EXPECT_CALL(*rfcMessage, addEmailHeader("To", "to1@ya.ru,TO2@Yandex.ru")).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Cc", "cc1@ya.ru,CC2@Yandex.ru")).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Bcc", "bcc1@ya.ru,Bcc <BcC2@Yandex.ru>")).Times(1);

    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::DONE);

    const RecipientsRepository& recipients = h.recipients();
    EXPECT_THAT(recipients.to(), UnorderedElementsAre(Email("to1", "ya.ru"), Email("TO2", "Yandex.ru")));
    EXPECT_THAT(recipients.cc(), UnorderedElementsAre(Email("cc1", "ya.ru"), Email("CC2", "Yandex.ru")));
    EXPECT_THAT(recipients.bcc(), UnorderedElementsAre(Email("bcc1", "ya.ru"), Email("BcC2", "Yandex.ru", "Bcc")));
}

TEST_F(RecipientsHeadersTest, shouldSetToCcBccHeadersToDraftAsIs) {
    const std::string to = "совсем не адрес";
    const std::string cc = "cc1@ya.ru, а тут адрес";
    const std::string bcc = "bcc1@ya.ru, \"Bcc\" bcc2@ya.ru";

    params::SaveDraft p;
    p.recipients = params::Recipients {
        to, cc, bcc
    };

    RecipientsHeaders h(notToLowercase, maxRecipients, p);

    EXPECT_CALL(*rfcMessage, addEmailHeaderDraft("To", to)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeaderDraft("Cc", cc)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeaderDraft("Bcc", bcc)).Times(1);

    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::DONE);
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfWrongTo) {
    EXPECT_CALL(*rfcMessage, addEmailHeader("Cc", usual)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Bcc", another)).Times(1);

    params::SendMessage p;
    p.recipients = params::Recipients {
        wrong, usual, another
    };

    RecipientsHeaders h(notToLowercase, maxRecipients, p);
    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::TO_INVALID);

    const RecipientsRepository& recipients = h.recipients();

    EXPECT_THAT(recipients.cc(), UnorderedElementsAre(Email("to", "ya.ru")));
    EXPECT_THAT(recipients.bcc(), UnorderedElementsAre(Email("another", "ya.ru")));
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfWrongCc) {
    EXPECT_CALL(*rfcMessage, addEmailHeader("To", usual)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Bcc", another)).Times(1);

    params::SendMessage p;
    p.recipients = params::Recipients {
        usual, wrong, another
    };

    RecipientsHeaders h(notToLowercase, maxRecipients, p);
    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::CC_INVALID);

    const RecipientsRepository& recipients = h.recipients();

    EXPECT_THAT(recipients.to(), UnorderedElementsAre(Email("to", "ya.ru")));
    EXPECT_THAT(recipients.bcc(), UnorderedElementsAre(Email("another", "ya.ru")));
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfWrongBcc) {
    EXPECT_CALL(*rfcMessage, addEmailHeader("To", usual)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Cc", another)).Times(1);

    params::SendMessage p;
    p.recipients = params::Recipients {
        usual, another, wrong
    };

    RecipientsHeaders h(notToLowercase, maxRecipients, p);
    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::BCC_INVALID);

    const RecipientsRepository& recipients = h.recipients();

    EXPECT_THAT(recipients.to(), UnorderedElementsAre(Email("to", "ya.ru")));
    EXPECT_THAT(recipients.cc(), UnorderedElementsAre(Email("another", "ya.ru")));
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfEmptyRecipients) {
    params::SendMessage p;
    p.recipients = params::Recipients {
        empty, empty, empty
    };

    ASSERT_EQ(RecipientsHeaders(notToLowercase, maxRecipients, p).apply(*rfcMessage), ComposeResult::TO_CC_BCC_EMPTY);
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfTooManyRecipients) {
    EXPECT_CALL(*rfcMessage, addEmailHeader("To", usual)).Times(1);
    EXPECT_CALL(*rfcMessage, addEmailHeader("Cc", another)).Times(1);

    params::SendMessage p;
    p.recipients = params::Recipients {
        usual, another, empty
    };

    ASSERT_EQ(RecipientsHeaders(notToLowercase, 1, p).apply(*rfcMessage), ComposeResult::MAX_EMAIL_ADDR_REACHED);
}

TEST_F(RecipientsHeadersTest, shouldRememberFirstError) {
    params::SendMessage p;
    p.recipients = params::Recipients {
        wrong, wrong, empty
    };
    ASSERT_EQ(RecipientsHeaders(notToLowercase, maxRecipients, p).apply(*rfcMessage), ComposeResult::TO_INVALID);
}

TEST_F(RecipientsHeadersTest, shouldReturnErrorInCaseOfEmailThatCannotBeCorrectlyParsedWithOurBrokenParser) {
    EXPECT_CALL(*rfcMessage, addEmailHeader("To", _)).Times(1);
    const auto correctEmail = Email("\"should <be> escaped\"", "another-mail.ru", "ыЙёЪь");
    params::SendMessage p = {
        .recipients = {.to = EmailHelpers::toString(correctEmail)}
    };
    
    RecipientsHeaders h(notToLowercase, maxRecipients, p);
    ASSERT_EQ(h.apply(*rfcMessage), ComposeResult::TO_INVALID);

    const RecipientsRepository& recipients = h.recipients();

    ASSERT_EQ(recipients.to().size(), 1U);
    ASSERT_NE(recipients.to().front(), correctEmail); // not equal here because of bug in the parser
}

}
