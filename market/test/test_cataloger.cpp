#include <market/library/market_servant/logger/logger.h>

#include <market/cataloger/src/components/user_data.h>
#include <market/cataloger/src/servant/cataloger_i.h>
#include <market/cataloger/src/utils/util.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/string/builder.h>

#include <iostream>
#include <string>


const int CATALOG_TELEPHONE_HID = 91461;
const int CATALOG_AUDIO_HID = 90543;

static cataloger_i* g_cataloger;

static cataloger_i& cataloger()
{
    if (!g_cataloger)
    {
        theLogger::Instance().SetBinLogLevel(LogLevel::ERROR | LogLevel::WARN | LogLevel::INFO);
        g_cataloger = new cataloger_i();
    }
    return *g_cataloger;
}

int noffers(std::string& xml, int hid)
{
    std::string shid = int2string(hid);
    size_t id_pos = xml.find("id=\"" + shid);
    if (id_pos == std::string::npos)
        return -1;
    size_t tag_end = xml.find("/>", id_pos);
    if (tag_end == std::string::npos)
        return -1;
    size_t noffers_pos = xml.find("offers_num=", id_pos);
    if (noffers_pos == std::string::npos || noffers_pos > tag_end)
        return -1;
    size_t noffers_end = xml.find("\"", noffers_pos + 12);
    std::string snoffers = xml.substr(noffers_pos + 12, noffers_end - noffers_pos -12);
    return string2int(snoffers);
}

inline std::string getTree1(int hid, const std::string& settings)
{
    const std::string req = TStringBuilder{} << "gettree?depth=1&hid=" << hid << "&" << settings;
    return cataloger().CallFunc(req).ContentStr();
}

TEST(TestCataloger, GetTree)
{
    //Пустые мобильные деревья не в мобильных категориях и немобильные деревья в мобильных категориях
    EXPECT_NO_THROW(getTree1(CATALOG_TELEPHONE_HID, "region=213"));

    std::string result41 = getTree1(CATALOG_AUDIO_HID, "&region=213");
    ASSERT_EQ(noffers(result41, 90544), 5097);
    ASSERT_EQ(noffers(result41, 90549), 5954);
    ASSERT_EQ(noffers(result41, 90548), 50122);

    ASSERT_EQ(noffers(result41, 6334304), -1);

    //от региона результат зависит
    std::string result51 = getTree1(CATALOG_AUDIO_HID, "region=20213");
    ASSERT_EQ(noffers(result51, 90544), 1709);
}

static std::string GetVendorCategories(int vendor_id, int region_id)
{
    std::string req("GetVendorCategories?id=" + std::to_string(vendor_id) + "&region=" + std::to_string(region_id));
    return cataloger().GetVendorCategoriesByRD(req).Content;
}

TEST(TestCataloger, VendorCategories)
{
    std::string result_937865_0 = GetVendorCategories(937865, 0); //"Taurus"
    ASSERT_NE(result_937865_0, "");
    std::string result_937865_213 = GetVendorCategories(937865, 213);
    ASSERT_NE(result_937865_213, "");

    std::string result_8466802_0 = GetVendorCategories(8466802, 0); //"Quelle"
    ASSERT_NE(result_8466802_0.find("<vendor"), std::string::npos);
    std::string result_8466802_213 = GetVendorCategories(8466802, 213);
    ASSERT_NE(result_8466802_213.find("<category"), std::string::npos);
    ASSERT_TRUE(result_8466802_0.size() >= result_8466802_213.size());
    //гуру категорий нет
    ASSERT_EQ(result_8466802_0.find("guru.xml"), std::string::npos);
}

static std::string GetBrandInfo(int vendor_id)
{
    std::string req("GetBrandInfo?id=" + std::to_string(vendor_id) + "&format=json");
    return cataloger().GetBrandInfo(req).Content;
}

TEST(TestCataloger, BrandInfo)
{
    std::string result = GetBrandInfo(152953);
    ASSERT_NE(result.find("\"categoriesCount\":0"), std::string::npos);
    ASSERT_NE(result.find("\"country\":\"Япония\""), std::string::npos);
    ASSERT_NE(result.find("\"description\":\"История компании Pioneer Electronics началась в 1938 году в городе Токио.\""), std::string::npos);
    ASSERT_NE(result.find("\"entity\":\"vendor\""), std::string::npos);
    ASSERT_NE(result.find("\"foundationYear\":\"1938\""), std::string::npos);
    ASSERT_NE(result.find("\"hasArticle\":false"), std::string::npos);
    ASSERT_NE(result.find("\"id\":152953"), std::string::npos);
    ASSERT_NE(result.find("\"name\":\"Pioneer\""), std::string::npos);
    ASSERT_NE(result.find("\"offersCount\":0"), std::string::npos);
    ASSERT_NE(result.find("\"seoTitle\":\"My seo title"), std::string::npos);
    ASSERT_NE(result.find("\"seoDescription\":\"My seo description"), std::string::npos);
    ASSERT_NE(result.find("\"website\":\"http://www.pioneer-rus.ru\""), std::string::npos);
}

TEST(TestCataloger, BrandInfoNoFoundationYear)
{
    std::string result = GetBrandInfo(995536);
    ASSERT_NE(result.find("\"name\":\"Fischer\""), std::string::npos);
    ASSERT_EQ(result.find("foundationYear"), std::string::npos);
    ASSERT_EQ(result.find("country"), std::string::npos);
}

TEST(TestCataloger, BrandInfoNoSeoData)
{
    std::string result = GetBrandInfo(995536);
    ASSERT_NE(result.find("\"name\":\"Fischer\""), std::string::npos);
    ASSERT_EQ(result.find("seoTitle"), std::string::npos);
    ASSERT_EQ(result.find("seoDescription"), std::string::npos);
}
