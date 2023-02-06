#include <iostream>
#include <sstream>
#include <parser/uri.h>

using std::string;

#define FAILED_MESSAGE                                                                             \
    {                                                                                              \
        std::cerr << "[FAILED] " << __FILE__ << ":" << __LINE__ << "\n";                           \
        return false;                                                                              \
    }

#define TEST(cond)                                                                                 \
    if (!(cond))                                                                                   \
    {                                                                                              \
        std::cerr << "[FAILED] " << __FILE__ << ":" << __LINE__ << "\n";                           \
        return 1;                                                                                  \
    }

bool test_uri(
    const string& u,
    const string& proto,
    const string& host,
    unsigned port,
    const string& fragment)
{
    ymod_webserver::http_uri result;
    if (!ymod_webserver::parser::parse_uri(u.begin(), u.end(), result)) FAILED_MESSAGE

    if (result.host.proto != proto) FAILED_MESSAGE

    if (result.host.domain != host) FAILED_MESSAGE

    if (result.host.port != port) FAILED_MESSAGE

    if (result.fragment != fragment) FAILED_MESSAGE

    return true;
}

int main()
{
    TEST(test_uri(
        "https://svn.yandex.ru:8080/mail/trunk?p1=a1&p2=a2;p3", "https", "svn.yandex.ru", 8080, ""))
    TEST(test_uri(
        "http://svn.yandex.ru/mail/trunk??dsa=&query=%D1%82%D1%83%D1%81%D1%82",
        "http",
        "svn.yandex.ru",
        80,
        ""))
    TEST(test_uri(
        "https://svn.yandex.ru:1080/mail/trunk?p1=a1&p2=a2;p3#INBOX",
        "https",
        "svn.yandex.ru",
        1080,
        "INBOX"))
    TEST(test_uri(
        "https://svn.mail.yandex.ru:8080/mail/trunk#INBOX",
        "https",
        "svn.mail.yandex.ru",
        8080,
        "INBOX"))
    TEST(test_uri("https://svn.yandex.ru:8080/?p1=a1&p2=a2;p3", "https", "svn.yandex.ru", 8080, ""))
    TEST(test_uri("https://svn.yandex.ru:8080/#INBOX", "https", "svn.yandex.ru", 8080, "INBOX"))

    TEST(test_uri("/mail/trunk?p1=a1&p2=a2;p3", "http", "", 80, ""))
    TEST(test_uri("/trunk??dsa=&query=%D1%82%D1%83%D1%81%D1%82", "http", "", 80, ""))
    TEST(test_uri("/?p1=a1&p2=a2;p3#INBOX", "http", "", 80, "INBOX"))
    TEST(test_uri("/mail/trunk#INBOX", "http", "", 80, "INBOX"))
    TEST(test_uri("/#INBOX", "http", "", 80, "INBOX"))
    return 0;
}
