#include <gtest/gtest.h>
#include <internal/dkim_parser.h>

namespace msg_body {

using namespace testing;

TEST( DkimTest, succeeded_withValid_returnsTrue ) {
    ASSERT_TRUE(Dkim(Dkim::valid).succeeded());
}

TEST( DkimTest, succeeded_withNosig_returnsFalse ) {
    ASSERT_FALSE(Dkim(Dkim::nosig).succeeded());
}

TEST( DkimTest, succeeded_withBadsig_returnsFalse ) {
    ASSERT_FALSE(Dkim(Dkim::badsig).succeeded());
}

TEST( DkimTest, succeeded_withUnknown_returnsFalse ) {
    ASSERT_FALSE(Dkim(Dkim::unknown).succeeded());
}

TEST( DkimTest, ctor_defaultStatus_unknown ) {
    ASSERT_EQ(Dkim().status(), Dkim::unknown);
}

TEST( DkimTest, domain_withHeaderiLoginDomain_returnsDomain ) {
    Dkim dkim(Dkim::unknown, "login@domain");
    ASSERT_EQ(dkim.domain(),"domain");
}

TEST( DkimTest, domain_withHeaderiDomainOnly_returnsDomain ) {
    Dkim dkim(Dkim::unknown, "@domain");
    ASSERT_EQ(dkim.domain(),"domain");
}

TEST( DkimTest, domain_withHeaderiLoginOnly_returnsEmpty ) {
    Dkim dkim(Dkim::unknown, "login@");
    ASSERT_EQ(dkim.domain(),"");
}

TEST( DkimTest, domain_withHeaderiNoDogSign_returnsDomain ) {
    Dkim dkim(Dkim::unknown, "domain");
    ASSERT_EQ(dkim.domain(),"domain");
}

TEST( DkimParserTest, parse_withPayPalStatusHeaderi_returnsHeaderi ) {
    const std::string header = "mxfront18.mail.yandex.net; spf=pass "
            "(mxfront18.mail.yandex.net: domain of paypal.com designates "
            "66.211.168.230 as permitted sender) smtp.mail=payment@paypal.com; "
            "dkim=pass header.i=service@paypal.com";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.headeri(), "service@paypal.com");
}

TEST( DkimParserTest, parse_withStatusHeaderi_returnsHeaderi ) {
    const std::string header = "rosenborg.yandex.ru; dkim=pass (2048-bit key)\n\r\t"
    "header.i=@gmail.com header.b=fTG+PUU9; dkim-adsp=none;";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.headeri(), "@gmail.com");
}

TEST( DkimParserTest, parse_withPassStatus_returnsValidDkim ) {
    const std::string header = "mxfront18.mail.yandex.net; spf=pass "
            "(mxfront18.mail.yandex.net: domain of paypal.com designates "
            "66.211.168.230 as permitted sender) smtp.mail=payment@paypal.com; "
            "dkim=pass header.i=service@paypal.com";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.status(), Dkim::valid);
}

TEST( DkimParserTest, parse_withFailStatus_returnsBadsigDkim ) {
    const std::string header = "mxfront18.mail.yandex.net; spf=pass "
            "(mxfront18.mail.yandex.net: domain of paypal.com designates "
            "66.211.168.230 as permitted sender) smtp.mail=payment@paypal.com; "
            "dkim=fail header.i=service@paypal.com";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.status(), Dkim::badsig);
}

TEST( DkimParserTest, parse_withNoStatus_returnsNosigDkim ) {
    const std::string header = "mxfront18.mail.yandex.net; spf=pass "
            "(mxfront18.mail.yandex.net: domain of paypal.com designates "
            "66.211.168.230 as permitted sender) smtp.mail=payment@paypal.com; ";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.status(), Dkim::nosig);
}

TEST( DkimParserTest, parse_withUnknownStatus_returnsBadsigDkim ) {
    const std::string header = "mxfront18.mail.yandex.net; spf=pass "
            "(mxfront18.mail.yandex.net: domain of paypal.com designates "
            "66.211.168.230 as permitted sender) smtp.mail=payment@paypal.com; "
            "dkim=zzz header.i=service@paypal.com";
    Dkim dkim(DkimParser().parse(header));
    ASSERT_EQ(dkim.status(), Dkim::unknown);
}

}
