#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <butil/network/real_ip_from_headers.h>

#include <string>
#include <optional>
#include <boost/optional.hpp>

namespace {

using namespace ::testing;

const std::string remoteIp = "ololo";

template <typename T>
struct NullOpt;

template <>
struct NullOpt<std::optional<std::string>> {
    static auto value() {
        return std::nullopt;
    }
};

template <>
struct NullOpt<boost::optional<std::string>> {
    static auto value() {
        return boost::none;
    }
};

template <typename T>
struct GetRealIpTest : Test {
    const T emptyString = std::string("");

    const T xForwardFromWithOneIp = std::string("first");
    const T xForwardFromWithTwoIp = *xForwardFromWithOneIp + ", second";
    const T xForwardFromWithTwoIpAndSpaces = std::string("first,    second");

    const T xRealIp = std::string("real ip");

    static auto nullOpt() {
        return NullOpt<T>::value();
    }
};

using TestingTypes = Types<std::optional<std::string>, boost::optional<std::string>>;
TYPED_TEST_SUITE(GetRealIpTest, TestingTypes);

TYPED_TEST(GetRealIpTest, shouldReturnAddressFromXForwardFrom) {
    EXPECT_EQ(real_ip::getFromHeaders(this->xForwardFromWithOneIp, this->xRealIp, remoteIp), *this->xForwardFromWithOneIp);
}

TYPED_TEST(GetRealIpTest, shouldReturnFirstAddressFromXForwardFrom) {
    EXPECT_EQ(real_ip::getFromHeaders(this->xForwardFromWithTwoIp, this->xRealIp, remoteIp), *this->xForwardFromWithOneIp);
    EXPECT_EQ(real_ip::getFromHeaders(this->xForwardFromWithTwoIpAndSpaces, this->xRealIp, remoteIp), *this->xForwardFromWithOneIp);
}

TYPED_TEST(GetRealIpTest, shouldReturnXRealIpIfXForwardFromIsEmpty) {
    EXPECT_EQ(real_ip::getFromHeaders(this->nullOpt(), this->xRealIp, remoteIp), *this->xRealIp);
    EXPECT_EQ(real_ip::getFromHeaders(this->emptyString, this->xRealIp, remoteIp), *this->xRealIp);
}

TYPED_TEST(GetRealIpTest, shouldReturnRemoteIpIfAnotherParamsAreEmpty) {
    EXPECT_EQ(real_ip::getFromHeaders(this->nullOpt(), this->nullOpt(), remoteIp), remoteIp);
    EXPECT_EQ(real_ip::getFromHeaders(this->emptyString, this->emptyString, remoteIp), remoteIp);
}

} // namespace
