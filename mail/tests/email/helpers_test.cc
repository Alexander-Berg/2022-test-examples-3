#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/email/helpers.h>

inline bool operator==(const Email& e1, const Email& e2)
{
    return e1.displayName() == e2.displayName() && e1.local() == e2.local() && e1.domain() == e2.domain();
}
inline bool operator!=(const Email& e1, const Email& e2)
{
    return !(e1==e2);
}

namespace {

using namespace testing;

typedef Test EmailHelpersTest;

TEST_F(EmailHelpersTest, isEmptyAddress_EmptyLocal_ReturnsTrue)
{
    Email email("","ya.ru");
    ASSERT_TRUE(EmailHelpers::isEmptyAddress(email));
}

TEST_F(EmailHelpersTest, isEmptyAddress_EmptyDomain_ReturnsTrue)
{
    Email email("super","");
    ASSERT_TRUE(EmailHelpers::isEmptyAddress(email));
}

TEST_F(EmailHelpersTest, isEmptyAddress_EmptyAddress_ReturnsTrue)
{
    Email email("","");
    ASSERT_TRUE(EmailHelpers::isEmptyAddress(email));
}

TEST_F(EmailHelpersTest, isEmptyAddress_NonEmptyAddress_ReturnsFalse)
{
    Email email("acme","supermail.com");
    ASSERT_FALSE(EmailHelpers::isEmptyAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_InvalidNonYandexAddress_ReturnsFalse)
{
    Email email("acme%","supermail.com");
    ASSERT_FALSE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_InvalidYandexAddress_ReturnsFalse)
{
    Email email("ac#me","yandex.ru");
    ASSERT_FALSE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_ValidYandexAddress_ReturnsTrue)
{
    Email email("a.c.m.e.","yandex.ru");
    ASSERT_TRUE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_ValidDotNonYandexAddress_ReturnsTrue)
{
    Email email("a.c.m.e.","supermail.com");
    ASSERT_TRUE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_ValidNonYandexAddress_ReturnsTrue)
{
    Email email("acme","supermail.com");
    ASSERT_TRUE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_EmptyLocalPart_ReturnsFalse)
{
    Email email("","supermail.com");
    ASSERT_FALSE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_EmptyDomainPart_ReturnsFalse)
{
    Email email("acme","");
    ASSERT_FALSE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, isValidAddress_phoneAddress_ReturnsTrue)
{
    Email email("+79991234567","ya.ru");
    ASSERT_TRUE(EmailHelpers::isValidAddress(email));
}

TEST_F(EmailHelpersTest, toString_ValidStringGenerated_ReturnsDisplayNameAndQuotedAddress)
{
    Email email("acme","supermail.com","Super Man");
    ASSERT_EQ("Super Man <acme@supermail.com>", EmailHelpers::toString(email));
}

TEST_F(EmailHelpersTest, fromString_EmailGenerated_ReturnsString)
{
    Email email("acme","supermail.com","Super Man");
    Email newEmail = EmailHelpers::fromString("Super Man <acme@supermail.com>");
    ASSERT_THAT(email, newEmail);
}

TEST_F(EmailHelpersTest, fromString_EmailGeneratedWithoutQuotesInDisplayName_ReturnsString)
{
    const std::string emailStr = "\"Pavel Durov, inContact.ru Admin\" <ya-autotest-303@yandex.ru>";
    Email newEmail = EmailHelpers::fromString(emailStr);
    ASSERT_EQ(emailStr, EmailHelpers::toString(newEmail));
}

TEST_F(EmailHelpersTest, fromString_EmptyString_ReturnsString)
{
    Email email("","","");
    Email newEmail = EmailHelpers::fromString("");
    ASSERT_THAT(email, newEmail);
}

TEST_F(EmailHelpersTest, fromString_EmptyAddress_ThrowsException)
{
    ASSERT_THROW(EmailHelpers::fromString("<>"), rfc2822ns::invalid_address);
}

TEST_F(EmailHelpersTest, fromString_InvalidAddress_ThrowsException)
{
    ASSERT_THROW(EmailHelpers::fromString("<#man@ya.ru>"), rfc2822ns::invalid_address);
}

TEST_F(EmailHelpersTest, idnaize_latinEmail_notModified) {
    ASSERT_EQ("qwe@rty.ru", EmailHelpers::idnaize("qwe@rty.ru"));
}

TEST_F(EmailHelpersTest, should_not_lowercase_email) {
    Email email("Foo", "Bar.Com");

    rfc2822ns::address_iterator it("Foo@Bar.Com");
    EXPECT_TRUE(EmailHelpers::isEqualEmails(email, EmailHelpers::fromStringWithoutLowercase(it)));
}

TEST_F(EmailHelpersTest, idnaize_cyrillicEmail_punycoded) {
    ASSERT_EQ("qwe@xn--i1aui.xn--p1ai", EmailHelpers::idnaize("qwe@йцу.рф"));
}

TEST_F(EmailHelpersTest, isEqualEmails_NonYandexHosts_ReturnFalse)
{
    Email email("acme","supermail.com");
    Email newEmail("acme1","supermail.com");
    ASSERT_FALSE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_NonYandexHosts_ReturnTrue)
{
    Email email("acme","supermail.com");
    Email newEmail("acme","supermail.com");
    ASSERT_TRUE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_YandexHosts_ReturnFalse)
{
    Email email("acme","yandex.com");
    Email newEmail("acme1","yandex.com");
    ASSERT_FALSE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_YandexHostsInDifferentDomains_ReturnTrue)
{
    Email email("acme","yandex.com");
    Email newEmail("acme","яндекс.рф");
    ASSERT_TRUE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_YandexTeamHosts_ReturnFalse)
{
    Email email("acme","yandex-team.com");
    Email newEmail("acme1","yandex-team.com");
    ASSERT_FALSE(EmailHelpers::isEqualEmails(email, newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_YandexTeamHostsInDifferentDomains_ReturnTrue)
{
    Email email("acme","yandex-team.com");
    Email newEmail("acme","yandex-team.ru");
    ASSERT_TRUE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_caseInsensitiveComparasion)
{
    Email email("acme","gmail.com");
    Email newEmail("aCmE","GmAiL.CoM");
    ASSERT_TRUE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, isEqualEmails_caseInsensitiveComparasionYandexDomains)
{
    Email email("acme","YanDeX.Ru");
    Email newEmail("aCmE","YANDEX.com.TR");
    ASSERT_TRUE(EmailHelpers::isEqualEmails(email,newEmail));
}

TEST_F(EmailHelpersTest, findEqualEmail_EqualEmailExists_ReturnIterator)
{
    EmailVec vec;
    Email email1("acme","yandex.com","Super Man");
    Email email2("woman","yandex.com","Super Woman");
    vec.push_back(email1);
    vec.push_back(email2);
    ASSERT_THAT(email1, *EmailHelpers::findEqualEmail(vec,email1));
}

}
