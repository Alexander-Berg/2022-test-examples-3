#include <crypta/ext_fp/matcher/lib/matchers/rostelecom_matcher/mc_domain/mc_domain.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NExtFp::NMatcher;

TEST(NMcDomain, GetMcSubdomain) {
    EXPECT_EQ("2724587256.mc.yandex.ru", NMcDomain::GetMcDomainForRostelecom(11111));
    EXPECT_EQ("3622327193.mc.yandex.ru", NMcDomain::GetMcDomainForRostelecom(1628717455134747796ull));
    EXPECT_EQ("523509417.mc.yandex.ru", NMcDomain::GetMcDomainForRostelecom(1628701383861053657ull));
}
