
#include <gtest/gtest.h>
#include <boost/algorithm/string/join.hpp>
#include <butil/network/rfc2822.h>

namespace {

using namespace rfc2822ns;

struct Address {
    std::string display;
    std::string address;
    std::string local;
    std::string domain;

    Address()
    {}

    Address(const std::string& display_, const std::string& address_,
            const std::string& local_, const std::string& domain_)
        : display(display_), address(address_), local(local_), domain(domain_)
    {}

    Address(const address_iterator& iter)
        : display(iter.display()), address(iter.address()), local(iter.local()), domain(iter.domain())
    {}

    bool operator==(const Address& other) const {
        return display == other.display && address == other.address
            && local == other.local && domain == other.domain;
    }
};

typedef std::vector<Address> Addresses;

std::ostream& operator<<(std::ostream& o, const Address& addr) {
    return o << "{" << addr.display << ", " << addr.address << ", "
        << addr.local << ", " << addr.domain << "}";
}

Addresses parseAddressString(const std::string& addrStr) {
    Addresses addrs;
    for (rfc2822ns::address_iterator iter(addrStr), end; iter != end; ++iter) {
        Address addr(iter);
        addrs.push_back(addr);
    }
    return addrs;
}

TEST(Rfc2822Test, testSingleAddress_parseCorrect) {
    const std::string addrStr = "Vasya Pupkin <vasya.pupkin@yandex.ru>";

    Addresses expected;
    expected.push_back(Address("Vasya Pupkin", "vasya.pupkin@yandex.ru", "vasya.pupkin", "yandex.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testManyAddresses_parseCorrect) {
    const std::string addrStr = "Vasya Pupkin <vasya.pupkin@yandex.ru>, vova@gmail.com; <kuzya1234@mail.ru>";

    Addresses expected;
    expected.push_back(Address("Vasya Pupkin", "vasya.pupkin@yandex.ru", "vasya.pupkin", "yandex.ru"));
    expected.push_back(Address("", "vova@gmail.com", "vova", "gmail.com"));
    expected.push_back(Address("", "kuzya1234@mail.ru", "kuzya1234", "mail.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testGroup_parseCorrect) {
    const std::string addrStr = "undisclosed-recipients: Vasya Pupkin <vasya.pupkin@yandex.ru>, vova@gmail.com; <kuzya1234@mail.ru>";

    Addresses expected;
    expected.push_back(Address("Vasya Pupkin", "vasya.pupkin@yandex.ru", "vasya.pupkin", "yandex.ru"));
    expected.push_back(Address("", "vova@gmail.com", "vova", "gmail.com"));
    expected.push_back(Address("", "kuzya1234@mail.ru", "kuzya1234", "mail.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testEmptyGroup_parseCorrect) {
    const std::string addrStr = "undisclosed-recipients:;, Vasya Pupkin <vasya.pupkin@yandex.ru>, vova@gmail.com; <kuzya1234@mail.ru>";

    Addresses expected;
    expected.push_back(Address("Vasya Pupkin", "vasya.pupkin@yandex.ru", "vasya.pupkin", "yandex.ru"));
    expected.push_back(Address("", "vova@gmail.com", "vova", "gmail.com"));
    expected.push_back(Address("", "kuzya1234@mail.ru", "kuzya1234", "mail.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testEmptyGroupAtEnd_parseCorrect) {
    const std::string addrStr = "vova@gmail.com; undisclosed-recipients:;";

    Addresses expected;
    expected.push_back(Address("", "vova@gmail.com", "vova", "gmail.com"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testEmptyString_returnEmptyAddresses) {
    const std::string addrStr = "";
    ASSERT_EQ(Addresses(), parseAddressString(addrStr));
}

TEST(Rfc2822Test, testEmptyGroup_returnEmptyAddresses) {
    const std::string addrStr = "recipients:;,";
    ASSERT_EQ(Addresses(), parseAddressString(addrStr));
}

TEST(Rfc2822Test, testNanobears_returnSomething) {
    const std::string addrStr = "\"ШТОШТО:::???Если мишк1и! nano-bears-technologies!\" <testix001@ya.ru>,\"ix004 test\" <testix004@ya.ru>";

    Addresses expected;
    expected.push_back(Address("\"ШТОШТО:::???Если мишк1и! nano-bears-technologies!\"", "testix001@ya.ru", "testix001", "ya.ru"));
    expected.push_back(Address("ix004 test", "testix004@ya.ru", "testix004", "ya.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

TEST(Rfc2822Test, testNanobearsWithoutQuotes_returnDisplayNameAfterSemicolon) {
    const std::string addrStr = "ШТОШТО:::???Если мишк1и! nano-bears-technologies! <testix001@ya.ru>,\"ix004 test\" <testix004@ya.ru>";

    Addresses expected;
    expected.push_back(Address("???Если мишк1и! nano-bears-technologies!", "testix001@ya.ru", "testix001", "ya.ru"));
    expected.push_back(Address("ix004 test", "testix004@ya.ru", "testix004", "ya.ru"));

    const Addresses addrs = parseAddressString(addrStr);
    ASSERT_EQ(expected, addrs);
}

} // namespace
