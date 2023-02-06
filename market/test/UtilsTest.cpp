#include <market/report/library/prepare_cpa_mask/prepare_cpa_mask.h>
#include <market/report/library/relevance/Utils.h>
#include <market/report/library/relevance/money/hybrid_auction_calculator.h>
#include <market/report/library/relevance/money/util.h>
#include <market/report/library/report_utils/Utils.h>
#include <market/report/library/request_utils/vendor_code/vendor_code.h>
#include <market/report/library/string_utils/string_utils.h>

#include <library/cpp/regex/pcre/regexp.h>
#include <library/cpp/digest/md5/md5.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace {
    using namespace NMarketReport;
}

struct TestFunc {
    void operator()(const TString& currentWord, TString& resultStr) const {
        if (!currentWord.empty()) {
            resultStr += "!" + currentWord;
        }
    }
};
TEST(Utils, EvalWordsInString) {
    const TString str = "my good\ttest\nstring\n\tto_change.";
    const TString result = evalWordsInString(str, TestFunc());
    EXPECT_EQ(TString("!my!good!test!string!to_change."), result);
}

TEST(Utils, PrepareCpaMask) {
    EXPECT_EQ(ECpa::No, PrepareCpaMask("no"));
    EXPECT_EQ(ECpa::Real, PrepareCpaMask("real"));
    EXPECT_EQ(static_cast<int>(ECpa::Real), static_cast<int>(PrepareCpaMask("-no")));
    EXPECT_EQ(static_cast<int>(ECpa::No), static_cast<int>(PrepareCpaMask("-real")));
    EXPECT_EQ(static_cast<int>(ECpa::No) | static_cast<int>(ECpa::Real), static_cast<int>(PrepareCpaMask("")));
    EXPECT_EQ((static_cast<int>(ECpa::No) | static_cast<int>(ECpa::Real)), static_cast<int>(PrepareCpaMask("-")));
}

TEST(Utils, ProcessYxTitleIfExist) {
    TString title = "my_title";
    TString hash = processYxTitleIfExist("text=our_text<<yx_title=\"" + title + "\"&rids=213");
    EXPECT_EQ("text=our_text<<yx_hashtitle:\"" + MD5::Calc(title) + "\"&rids=213", hash);
    title = "название";
    hash = processYxTitleIfExist("text=our_text<<title=\"" + title + "\"&rids=213");
    EXPECT_EQ("text=our_text<<title=\"" + title + "\"&rids=213", hash);
}

TEST(Utils, EncodeOpenstat) {
    using namespace NMarketReport::NMoney;
    TString title = "Some long title";
    TString wmd5 = "wgrU12_pd1mqJ6DJm_9nEA";
    TString plain = "market.yandex.ru;"+title+";"+wmd5+";";
    EXPECT_EQ(Base64EncodeUrl(plain),     "bWFya2V0LnlhbmRleC5ydTtTb21lIGxvbmcgdGl0bGU7d2dyVTEyX3BkMW1xSjZESm1fOW5FQTs,");
    EXPECT_EQ(EncodeOpenStat(title, wmd5),  "bWFya2V0LnlhbmRleC5ydTtTb21lIGxvbmcgdGl0bGU7d2dyVTEyX3BkMW1xSjZESm1fOW5FQTs");
}

TEST(Utils, reformatPrice) {
    EXPECT_EQ(TString("12"), reformatPrice("12"));
    EXPECT_EQ(TString("100 000"), reformatPrice("100000"));
    EXPECT_EQ(TString("1 567 389 472"), reformatPrice("1567389472"));
    EXPECT_EQ(TString("1 224.43"), reformatPrice("1224.43"));
    EXPECT_EQ(TString("0.23"), reformatPrice("0.23"));
    EXPECT_EQ(TString("-1"), reformatPrice("-1"));
}

TEST(Utils, reformatPriceWithZeros) {
    EXPECT_EQ(TString("12.00"), reformatPrice("12", true));
    EXPECT_EQ(TString("1 567 389 472.00"), reformatPrice("1567389472", true));
    EXPECT_EQ(TString("-1"), reformatPrice("-1", true));
}

TEST(Utils, ShrinkQueryParams) {
    const TString unescaped = "https://m.market.yandex.ru/product/12789408?hid=90594&show-uid=036624399998229713416010&from=search&text=фирма галатек отзывы";
    const TString escaped = "https://m.market.yandex.ru/product/12789408?hid=90594&show-uid=036624399998229713416010&from=search&text=%D1%84%D0%B8%D1%80%D0%BC%D0%B0%20%D0%B3%D0%B0%D0%BB%D0%B0%D1%82%D0%B5%D0%BA%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B";
    auto r = NMarketReport::NMoney::TryShrinkQueryParams(unescaped, escaped, escaped.size() - 10);
    EXPECT_TRUE(r.Defined());
    const TString shrinked = *r;
    EXPECT_LT(shrinked.size(), escaped.size());
}

TEST(HybridAuctionTool, CpaSerialize) {
    EXPECT_EQ(TString("0.0100"), NHybridAuction::FeeToStringDecimal(100));
    EXPECT_EQ(TString("0.0200"), NHybridAuction::FeeToStringDecimal(200));
    EXPECT_EQ(TString("0.0301"), NHybridAuction::FeeToStringDecimal(301));
    EXPECT_EQ(TString("0.0000"), NHybridAuction::FeeToStringDecimal(0));
}

TEST(Utils, HasVendorCode) {

    EXPECT_TRUE(HasVendorCode("цифровая последовательность 5 и более символов 12345"));
    EXPECT_TRUE(HasVendorCode("цифровая последовательность с пунктуацией 1-2345-6789"));
    EXPECT_TRUE(HasVendorCode("цифровая последовательность с пунктуацией 1/2345"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность AB13"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность R13"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность AB-X5"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность 18/ABX"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность AB\\23"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность от 3х символов A5B"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность от 3х символов 4N2"));
    EXPECT_TRUE(HasVendorCode("цифро буквенная последовательность от 3х символов 90x60"));

    EXPECT_TRUE(HasVendorCode("очень сложная последовательность url:aktivstyle.ru/taxonomy/term/103/11950%2C11951%2C11952%2C11953%2C11954%2C12871%2C12873%2C12867%2C12870%2C12868%2C12872%2C12869%2C12874%2C12876%2C12865%2C12875%2C12866%2C12877%2C12878%2C12879%2C12942%2C12957%2C13285%2C13286%2C13287%2C13288%2C13398%2C13400%2C13403%2C13401%2C13399%2C13404%2C13407%2C13408%2C13406%2C13405%2C13595%2C13596%2C13597%2C13598%2C13599%2C13691%2C13692%2C13693%2C13712%2C13701%2C13711%2C13710%2C13700%2C13703%2C13699%2C13709%2C13708%2C13697%2C13698%2C13707%2C13706%2C13705%2C13704%2C13702%2C13882%2C13887%2C13893%2C13894%2C13895%2C13995%2C14054%2C14055%2C14056%2C14057%2C14058%2C14059%2C14060%2C14061%2C14062%2C14063%2C14064%2C14065%2C14066%2C14067%2C14069%2C14071%2C14076%2C14080%2C14081%2C14082%2C14083%2C14085%2C14349%2C14350%2C14351%2C14352%2C14353%2C14354%2C14496%2C14497%2C14499%2C14501%2C14503%2C14504%2C14505%2C14506%2C14507%2C14508%2C14509%2C14510%2C14511%2C14512%2C14513%2C14514%2C14515%2C14516%2C14517%2C14518%2C14519%2C14594%2C14595%2C14596%2C14597%2C14599%2C14601%2C14606%2C14607%2C14608%2C14609%2C14610%2C14611%2C14613%2C14614%2C14615%2C14616%2C14617%2C14618%2C14619%2C14620%2C14624%2C14625%2C14626%2C14632%2C14637%2C14707%2C14709%2C14711%2C14712%2C14714%2C14720%2C14721%2C14724%2C14725%2C14727%2C14728%2C14729%2C14732%2C14733%2C14734%2C14741%2C14742%2C14744%2C14745%2C14746%2C14747%2C14748%2C15179%2C15182%2C15185%2C15186%2C15207"));

    EXPECT_FALSE(HasVendorCode("Слишком короткая цифро буквенная последовательность S7"));
    EXPECT_FALSE(HasVendorCode("Единицы измерения 1000mm"));
    EXPECT_FALSE(HasVendorCode("Русские буквы АИ95"));
    EXPECT_FALSE(HasVendorCode("дробные числа 5,5mm"));
    EXPECT_FALSE(HasVendorCode("дробные числа 123.4 метра"));
    EXPECT_FALSE(HasVendorCode("слова через дефис Not-vendor-code"));
}

TEST(Utils, WholeRequestIsVendorCode) {
    EXPECT_TRUE(WholeRequestIsVendorCode("1-2345-6789", "1-2345-6789"));
    EXPECT_TRUE(WholeRequestIsVendorCode("12345", "12345"));
    EXPECT_TRUE(WholeRequestIsVendorCode("1-2345-6789 12345", "1-2345-6789 12345"));
    EXPECT_TRUE(WholeRequestIsVendorCode("AB13", "Ab13"));
    EXPECT_TRUE(WholeRequestIsVendorCode("18/ABX 90x60[]{}();:,?!'<>", "18/ABX 90x60"));
    EXPECT_TRUE(WholeRequestIsVendorCode("[]{}();:,?!'<>1/2345", "1/2345"));
    EXPECT_TRUE(WholeRequestIsVendorCode("ab13", "AB13"));
    EXPECT_TRUE(WholeRequestIsVendorCode("AB-X5,18/ABX.1-2345-6789:90x60", "abx518abx12345678990x60"));
    EXPECT_TRUE(WholeRequestIsVendorCode("AB-X5", "AB-X5"));
    EXPECT_TRUE(WholeRequestIsVendorCode("AB-X5 abc", "ab-X5 abc"));

    EXPECT_FALSE(WholeRequestIsVendorCode("AB-X5", "Vendor AB-X5"));
    EXPECT_FALSE(WholeRequestIsVendorCode("AB-X5 abc", "Vendor ab-X5 abc"));
    EXPECT_FALSE(WholeRequestIsVendorCode("ab13", "AB13 АБ13"));
    EXPECT_FALSE(WholeRequestIsVendorCode("ab13", "АБ13"));
    EXPECT_FALSE(WholeRequestIsVendorCode("12345", "этот 12345 код"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1-2345-6789", "1-2345-6780"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1-2345-6789 12345", "12345 1-2345-6789"));
    EXPECT_FALSE(WholeRequestIsVendorCode("18/ABX 90x60", "18/ABX a 90x60"));
    EXPECT_FALSE(WholeRequestIsVendorCode("ab13[]{}();:,?!'<>abx5", "abx5 ab13"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1-2345-6789", "1-2345-678"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1-2345-6789", "1-2345-67890"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1-2345-6789-123", "1-2345-6789"));
    EXPECT_FALSE(WholeRequestIsVendorCode("123-VendorCode", "VendorCode"));
    EXPECT_FALSE(WholeRequestIsVendorCode("1234", "1234"));
    EXPECT_FALSE(WholeRequestIsVendorCode("аи95", "аи95"));
    EXPECT_FALSE(WholeRequestIsVendorCode("not-vendor-code", "not-vendor-code"));
    EXPECT_FALSE(WholeRequestIsVendorCode("аи95", "vendorCode аи95"));
}
