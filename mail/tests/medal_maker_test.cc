#include <gtest/gtest.h>
#include <internal/medal_maker.h>

namespace msg_body {

using namespace testing;

TEST( MedalMaker, getSenderDomain_withSimpleAddress_returnsDomain ) {
    ASSERT_EQ(MedalMaker::getSenderDomain("login@domain.com"), "domain.com");
}

TEST( MedalMaker, getSenderDomain_withIdnaDomainInAddress_returnsDecodedDomain ) {
    ASSERT_EQ(MedalMaker::getSenderDomain("login@xn--80a1acny.xn--p1ag"), "почта.ру");
}

TEST( MedalMaker, getSenderDomain_withAngleAddress_returnsDomain ) {
    ASSERT_EQ(MedalMaker::getSenderDomain("<login@domain.com>"), "domain.com");
}

TEST( MedalMaker, getSenderDomain_withDisplayNameInAddress_returnsDomain ) {
    ASSERT_EQ(MedalMaker::getSenderDomain("MyName <login@domain.com>"), "domain.com");
}

TEST( MedalMaker, getSenderDomain_withEmtyAddress_returnsEmpty ) {
    ASSERT_EQ(MedalMaker::getSenderDomain(""), "");
}

}
