#include <market/idx/models/lib/common/model_info.h>

#include <market/library/base64_protos_serializer/base64_protos_serializer.h>
#include <market/library/mbo_params/mbo_parameters.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/string/vector.h>

using namespace NMarket::NModel;

namespace {
    TInfo PicturesFromProto(const TString &big, const TString &add) {
        TInfo ret;
        ret.MainPicture.ParseFromStringOrThrow(Base64Decode(big));

        Market::ParseBase64Protos<TMboPicture>(add,
                                               [&](const TMboPicture &pic) { ret.AdditionalPictures.push_back(pic); });
        return ret;
    }

    TInfo PicturesToFromProto(const TInfo &in) {
        const TString big = Market::SerializeToBase64String(in.MainPicture);
        const TString add = in.GetAdditionalPicturesProtosString();

        return PicturesFromProto(big, add);
    }

    void AddXLPicture(
            NMarket::NMbo::TExportReportModel &model,
            const TString &index,
            const TString &url,
            const int x,
            const int y,
            const float colorness,
            const float colornessAvg
    ) {
        auto p = model.add_pictures();
        p->set_xslname("XL-Picture" + index);
        p->set_url(url);
        p->set_height(y);
        p->set_width(x);
        p->set_colorness(colorness);
        p->set_colorness_avg(colornessAvg);
    }
}


TEST(TINFO, XL_PICTURES) {
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    AddXLPicture(model, "", "xl_pic_url", 1500, 1700, 1960, 1980);
    AddXLPicture(model, "_2", "xl_pic_url_2", 2500, 2700, 2960, 2980);
    AddXLPicture(model, "_3", "xl_pic_url_3", 3500, 3700, 3960, 3980);

    TInfo info;
    FillInfo(info, model, mboCategory);


    auto pic = info.MainPicture;
    ASSERT_TRUE(IsOk(pic));
    ASSERT_EQ(pic.url(), "xl_pic_url");
    ASSERT_EQ(pic.width(), 1500);
    ASSERT_EQ(pic.height(), 1700);
    ASSERT_EQ(pic.colorness(), 1960.0);
    ASSERT_EQ(pic.colorness_avg(), 1980.0);
    ASSERT_EQ(ToString(pic), "xl_pic_url#1500#1700");

    const TVector<TMboPicture>& pics = info.AdditionalPictures;
    ASSERT_EQ(pics.size(), 4);

    ASSERT_FALSE(IsOk(pics[0]));
    ASSERT_FALSE(IsOk(pics[1]));

    ASSERT_EQ(pics[2].url(), "xl_pic_url_2");
    ASSERT_EQ(pics[2].width(), 2500);
    ASSERT_EQ(pics[2].height(), 2700);
    ASSERT_EQ(pics[2].colorness(), 2960);
    ASSERT_EQ(pics[2].colorness_avg(), 2980);

    ASSERT_EQ(pics[3].url(), "xl_pic_url_3");
    ASSERT_EQ(pics[3].width(), 3500);
    ASSERT_EQ(pics[3].height(), 3700);
    ASSERT_EQ(pics[3].colorness(), 3960);
    ASSERT_EQ(pics[3].colorness_avg(), 3980);

    ASSERT_EQ(info.GetAdditionalPicturesString(), "xl_pic_url_2#2500#2700\txl_pic_url_3#3500#3700");

    const auto info2 = PicturesToFromProto(info);
    ASSERT_EQ(info.GetAdditionalPicturesString(), info2.GetAdditionalPicturesString());

    ASSERT_EQ(SplitString(info.GetAdditionalPicturesProtosString(), "|").size(), 2);
}


TEST(TINFO, XL_PICTURES_ADDITIONAL_ONLY) {
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    AddXLPicture(model, "_2", "xl_pic_url_2", 2500, 2700, 2960, 2980);
    AddXLPicture(model, "_3", "xl_pic_url_3", 3500, 3700, 3960, 3980);

    TInfo info;
    FillInfo(info, model, mboCategory);


    auto pic = info.MainPicture;
    ASSERT_TRUE(!IsOk(pic));

    const TVector<TMboPicture>& pics = info.AdditionalPictures;
    ASSERT_EQ(pics.size(), 4);

    ASSERT_FALSE(IsOk(pics[0]));
    ASSERT_FALSE(IsOk(pics[1]));

    ASSERT_EQ(pics[2].url(), "xl_pic_url_2");
    ASSERT_EQ(pics[2].width(), 2500);
    ASSERT_EQ(pics[2].height(), 2700);
    ASSERT_EQ(pics[2].colorness(), 2960);
    ASSERT_EQ(pics[2].colorness_avg(), 2980);

    ASSERT_EQ(pics[3].url(), "xl_pic_url_3");
    ASSERT_EQ(pics[3].width(), 3500);
    ASSERT_EQ(pics[3].height(), 3700);
    ASSERT_EQ(pics[3].colorness(), 3960);
    ASSERT_EQ(pics[3].colorness_avg(), 3980);

    ASSERT_EQ(info.GetAdditionalPicturesString(), "xl_pic_url_2#2500#2700\txl_pic_url_3#3500#3700");

    const auto info2 = PicturesToFromProto(info);
    ASSERT_EQ(info.GetAdditionalPicturesString(), info2.GetAdditionalPicturesString());

    ASSERT_EQ(SplitString(info.GetAdditionalPicturesProtosString(), "|").size(), 2);
}


TEST(TINFO, XL_PICTURES_INDEX_LIMIT) {
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    AddXLPicture(model, "", "xl_pic_url", 1500, 1700, 1960, 1980);
    AddXLPicture(model, "_2", "xl_pic_url_2", 2500, 2700, 2960, 2980);
    AddXLPicture(model, "_1001", "xl_pic_url_1001", 100, 200, 300, 400);

    TInfo info;
    FillInfo(info, model, mboCategory);


    auto pic = info.MainPicture;
    ASSERT_TRUE(IsOk(pic));
    ASSERT_EQ(pic.url(), "xl_pic_url");
    ASSERT_EQ(pic.width(), 1500);
    ASSERT_EQ(pic.height(), 1700);
    ASSERT_EQ(pic.colorness(), 1960.0);
    ASSERT_EQ(pic.colorness_avg(), 1980.0);
    ASSERT_EQ(ToString(pic), "xl_pic_url#1500#1700");

    const TVector<TMboPicture>& pics = info.AdditionalPictures;
    ASSERT_EQ(pics.size(), 3);

    ASSERT_FALSE(IsOk(pics[0]));
    ASSERT_FALSE(IsOk(pics[1]));

    ASSERT_EQ(pics[2].url(), "xl_pic_url_2");
    ASSERT_EQ(pics[2].width(), 2500);
    ASSERT_EQ(pics[2].height(), 2700);
    ASSERT_EQ(pics[2].colorness(), 2960);
    ASSERT_EQ(pics[2].colorness_avg(), 2980);
}


TEST(TINFO, XL_PICTURE_WITH_INVALID_URL) {
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    AddXLPicture(model, "", "null", 1500, 1700, 1960, 1980);

    TInfo info;
    FillInfo(info, model, mboCategory);


    const TMboPicture& pic = info.MainPicture;
    ASSERT_FALSE(IsOk(pic));
}


TEST(TINFO, DATE) {
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    auto p = model.add_parameter_values();
    p->set_xsl_name("OldestDate");
    p->add_str_value()->set_value("21.02.2017 13:29:34");

    p = model.add_parameter_values();
    p->set_xsl_name("SaleDate");
    p->add_str_value()->set_value("2017-02-21");

    p = model.add_parameter_values();
    p->set_xsl_name("ModelYear");
    p->set_param_id(1);
    p->set_option_id(1);

    NMarket::NMbo::TParameter mboParameter;
    mboParameter.Options.push_back({ui64(1), TString("2017"), true});
    mboCategory.Parameters.insert({ui64(1), mboParameter});

    TInfo info;
    FillInfo(info, model, mboCategory);

    // manushkin@laptop:~$ LC_ALL=C date -d @1487683774 -u
    // Tue Feb 21 13:29:34 UTC 2017
    ASSERT_EQ(info.CreatedTs, time_t(1487683774));

    ASSERT_EQ(info.SaleDate.Seconds(), 1487635200);
    struct tm saleDate;
    info.SaleDate.GmTime(&saleDate);
    ASSERT_EQ(saleDate.tm_year, 117);         // since 1900
    ASSERT_EQ(saleDate.tm_mon, 1);            // 0 .. 11
    ASSERT_EQ(saleDate.tm_mday, 21);          // 1 .. 31

    ASSERT_EQ(info.ModelYear, 2017);
}


TEST(TINFO, DATE_INVALID) {
    // check that invalid date is parsed into empty TInstant()
    NMarket::NMbo::TExportReportModel model;
    NMarket::NMbo::TCategory mboCategory("");

    auto * p = model.add_parameter_values();
    p->set_xsl_name("SaleDate");
    p->add_str_value()->set_value("anno domini MCCXXXIIII");

    TInfo info;
    FillInfo(info, model, mboCategory);

    ASSERT_FALSE(info.SaleDate);
}


TEST(TINFO, ISNEW) {
    const time_t currentTime = time_t(1487672974);             // 21.02.2017 13:29:34
    const time_t oldestDate = time_t(1487672974 - 31 * 86400); // -1 month
    const TInstant saleDate = TInstant::Seconds(oldestDate);

    ASSERT_TRUE(CalcIsNew(currentTime, 0, 0, oldestDate, TInstant()));
    ASSERT_TRUE(CalcIsNew(currentTime, 0, 0, 0, saleDate));
    ASSERT_FALSE(CalcIsNew(currentTime, 0, 0, 0, saleDate - TDuration::Days(31))); // -2 month from currentTime

    ASSERT_TRUE(CalcIsNew(currentTime, 2017, 2017, oldestDate, TInstant()));
    ASSERT_TRUE(CalcIsNew(currentTime, 2017, 2016, 0, TInstant()));

    // check that whole pipeline from SaleDate parsing to IsNew property works as expected
    // here we check corner case when ModelYear == 0 && Now() - SaleDate < 2 months
    {
        NMarket::NMbo::TExportReportModel model;
        NMarket::NMbo::TCategory mboCategory("");

        const auto notSoOldDate = Now() - TDuration::Days(10);
        {
            auto * p = model.add_parameter_values();
            p->set_xsl_name("SaleDate");
            p->add_str_value()->set_value(notSoOldDate.FormatGmTime("%Y-%m-%d"));
        }

        TInfo info;
        FillInfo(info, model, mboCategory);
        ASSERT_TRUE(info.IsNew);
    }

    // check that if we set OldestDate but not SaleDate then IsNew will be true
    {
        NMarket::NMbo::TExportReportModel model;
        NMarket::NMbo::TCategory mboCategory("");

        const auto notSoOldDate = Now() - TDuration::Days(10);
        {
            auto p = model.add_parameter_values();
            p->set_xsl_name("OldestDate");
            p->add_str_value()->set_value(notSoOldDate.FormatGmTime("%d.%m.%Y %H:$M:%S"));
        }

        TInfo info;
        FillInfo(info, model, mboCategory);
        ASSERT_TRUE(info.IsNew);
    }

    // check that if we don't set either OldestDate or SaleDate then IsNew will be false
    {
        NMarket::NMbo::TExportReportModel model;
        NMarket::NMbo::TCategory mboCategory("");

        TInfo info;
        FillInfo(info, model, mboCategory);
        ASSERT_FALSE(info.IsNew);
    }

    // here we check corner case when ModelYear == 0 && Now() - SaleDate > 2 months
    {
        NMarket::NMbo::TExportReportModel model;
        NMarket::NMbo::TCategory mboCategory("");

        auto * p = model.add_parameter_values();
        p->set_xsl_name("SaleDate");
        const auto oldEnoughDate = Now() - TDuration::Days(100);
        p->add_str_value()->set_value(oldEnoughDate.FormatGmTime("%Y-%m-%d"));

        TInfo info;
        FillInfo(info, model, mboCategory);
        ASSERT_FALSE(info.IsNew);
    }
}
