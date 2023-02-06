#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <market/gumoful/tools/utils/mbo_model.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
#include <util/stream/file.h>
#include <market/gumoful/tools/utils/mbo_protobuf_renderer.h>
#include <util/generic/maybe.h>
#include <util/string/vector.h>
#include <library/cpp/json/json_writer.h>

class TTestMicroformats: public ::testing::Test
{
protected:
    void SetUp() override {
        const auto categoryMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/category_91491.pb")).ReadAll();
        const auto modelMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/model_10495456.pb")).ReadAll();

        TVarIntLenValueProtoReader categoryReader(categoryMessage);
        const auto categoryMsg = *categoryReader.Read<TExportCategory>();

        TVarIntLenValueProtoReader modelReader(modelMessage);
        const auto modelPb = *modelReader.Read<TExportReportModel>();

        category.Reset(new TMboCategory(categoryMsg));
        model.Reset(new TMboModel(modelPb, *category));
    }

    template <NGumoful::ETemplateType Type>
    void AssertError(const std::string& templateSourceCode, const std::string& expectedError)
    {
        const auto errors = gumoful.Render<Type>(*category, *model, templateSourceCode).Exceptions;
        ASSERT_TRUE(!errors.empty());
        UNIT_ASSERT_STRING_CONTAINS(errors[0].Message, expectedError);
    }

    template <NGumoful::ETemplateType Type>
    void AssertNoErrors(const std::string& templateSourceCode)
    {
        const auto errors = gumoful.Render<Type>(*category, *model, templateSourceCode).Exceptions;
        ASSERT_TRUE(errors.empty());
    }

    NGumoful::TModelTemplateRenderer gumoful;
    THolder<TMboCategory> category;
    THolder<NGumoful::IModel> model;
};

TEST_F(TTestMicroformats, TestMicroModelTemplate)
{
    const auto expected = "GSM, LTE, смартфон, вес 112 г, ШхВхТ 58.6x123.8x7.6 мм, экран 4\", 1136x640, "
            "Bluetooth, Wi-Fi, GPS, ГЛОНАСС, фотокамера 8 МП, память 16 Гб, аккумулятор 1560 мА⋅ч";

    const auto result = gumoful.Render<NGumoful::ETemplateType::MICRO_MODEL>(*category, *model,
                                       category->GetMicroModelTemplate());

    ASSERT_TRUE(result.Exceptions.empty());
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult, expected);
}

TEST_F(TTestMicroformats, TestBriefModelTemplate)
{
    const auto expected = "3G, 4\", 1136x640, 16Гб, 112г, камера 8МП, Bluetooth";
    const auto result = gumoful.Render<NGumoful::ETemplateType::BRIEF_MODEL>(*category, *model,
                                       category->GetBriefModelTemplate());

    ASSERT_TRUE(result.Exceptions.empty());
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult, expected);
}

TEST_F(TTestMicroformats, TestMicroModelSearchTemplate)
{
    const auto expected = "смартфон 3G, MP3, Bluetooth, Wi-Fi, GPS,  4G LTE, фотокамера камера --";
    const auto result = gumoful.Render<NGumoful::ETemplateType::MICRO_MODEL_SEARCH>(*category, *model,
                                       category->GetMicroModelSearchTemplate());

    ASSERT_TRUE(result.Exceptions.empty());
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult, expected);
}

TEST_F(TTestMicroformats, TestSeoTemplate)
{
    const auto result = gumoful.Render<NGumoful::ETemplateType::SEO>(*category, *model, category->GetSeoTemplate());

    ASSERT_TRUE(result.Exceptions.empty());
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult.Nominative, "смартфон");
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult.Dative, "смартфону");
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult.Genitive, "смартфона");
    UNIT_ASSERT_STRINGS_EQUAL(result.RenderResult.Accusative, "смартфон");
}

TEST_F(TTestMicroformats, TestFriendlyTemplate)
{
    const auto expected = "смартфон|экран 4\", разрешение 1136x640|камера 8 МП, автофокус|"
                          "память 16 Гб, без слота для карт памяти|3G, 4G LTE, Wi-Fi, Bluetooth, GPS, ГЛОНАСС|"
                          "аккумулятор 1560 мА⋅ч|вес 112 г, ШxВxТ 58.60x123.80x7.60 мм";
    const auto result = gumoful.Render<NGumoful::ETemplateType::FRIENDLY_MODEL>(*category, *model,
                                       category->GetFriendlyModelTemplate());

    ASSERT_TRUE(result.Exceptions.empty());

    TVector<TString> specValues;
    specValues.resize(result.RenderResult.Specs.size());
    std::transform(result.RenderResult.Specs.begin(), result.RenderResult.Specs.end(),
                   specValues.begin(),
                   [](const auto& spec) { return spec.Value; });

    UNIT_ASSERT_STRINGS_EQUAL(JoinStrings(specValues, "|"), expected);
}

TEST_F(TTestMicroformats, TestModelTemplate)
{
    const auto result = gumoful.Render<NGumoful::ETemplateType::MODEL>(*category, *model,
                                                                       category->GetModelTemplate());

    ASSERT_EQ(8, result.RenderResult.GroupSpecs.size());
    TVector<std::tuple<TString, TString, bool>> expected = {
            { "Тип", "смартфон", true},
            { "Вес", "112 г", true},
            { "Размеры (ШxВxТ)", "58.6x123.8x7.6 мм", true}
    };

    for(size_t i = 0; i < expected.size(); ++i) {
        UNIT_ASSERT_STRINGS_EQUAL(std::get<0>(expected[i]), result.RenderResult.GroupSpecs[0].Specs[i].Name);
        UNIT_ASSERT_STRINGS_EQUAL(std::get<1>(expected[i]), result.RenderResult.GroupSpecs[0].Specs[i].Value);
        ASSERT_EQ(std::get<2>(expected[i]), result.RenderResult.GroupSpecs[0].Specs[i].IsMainProperty);
    }


    ASSERT_TRUE(result.Exceptions.empty());
}

TEST_F(TTestMicroformats, TestModelSchema)
{
    AssertError<NGumoful::ETemplateType::MODEL>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::MODEL>("<ya_guru_modelcard><block/></ya_guru_modelcard>",
        "'name' attribute expected inside 'block' element in result xml");
    AssertError<NGumoful::ETemplateType::MODEL>("<ya_guru_modelcard>"
                                                    "<block name='x'>"
                                                        "<spec_guru_modelcard>"
                                                            "<used_params><param id='1' xsl_name='XLPictureSizeX'/></used_params>"
                                                        "</spec_guru_modelcard>"
                                                    "</block>"
                                                "</ya_guru_modelcard>",
        "'name' attribute expected inside 'param' element in result xml");
    AssertError<NGumoful::ETemplateType::MODEL>("<ya_guru_modelcard>"
                                                    "<block name='x'>"
                                                        "<spec_guru_modelcard>"
                                                            "<used_params><param name='x' xsl_name='XLPictureSizeX'/></used_params>"
                                                        "</spec_guru_modelcard>"
                                                    "</block>"
                                                "</ya_guru_modelcard>",
        "'id' attribute expected inside 'param' element in result xml");
    AssertError<NGumoful::ETemplateType::MODEL>("<ya_guru_modelcard>"
                                                    "<block name='x'>"
                                                        "<spec_guru_modelcard>"
                                                            "<used_params><param id='1' name='x'/></used_params>"
                                                        "</spec_guru_modelcard>"
                                                    "</block>"
                                                "</ya_guru_modelcard>",
        "'xsl_name' attribute expected inside 'param' element in result xml");
    AssertNoErrors<NGumoful::ETemplateType::MODEL>("<ya_guru_modelcard>"
                                                        "<block name='x'>"
                                                            "<spec_guru_modelcard>"
                                                                "<used_params><param name='x' id='1' xsl_name='XLPictureSizeX'/></used_params>"
                                                            "</spec_guru_modelcard>"
                                                        "</block>"
                                                    "</ya_guru_modelcard>");
}

TEST_F(TTestMicroformats, TestMicroModelSchema)
{
    AssertError<NGumoful::ETemplateType::MICRO_MODEL>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::MICRO_MODEL>("<ya_guru_modelcard>value</ya_guru_modelcard>",
        "'block' element expected inside 'ya_guru_modelcard' element in result xml");
    AssertNoErrors<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><block>value</block></ya_guru_modelcard>");
}

TEST_F(TTestMicroformats, TestMicroModelSearchSchema)
{
    AssertError<NGumoful::ETemplateType::MICRO_MODEL_SEARCH>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::MICRO_MODEL_SEARCH>("<ya_guru_modelcard>value</ya_guru_modelcard>",
         "'block' element expected inside 'ya_guru_modelcard' element in result xml");
    AssertNoErrors<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><block>value</block></ya_guru_modelcard>");
}

TEST_F(TTestMicroformats, TestBriefModelSchema)
{
    AssertError<NGumoful::ETemplateType::BRIEF_MODEL>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard>value</ya_guru_modelcard>",
         "'block' element expected inside 'ya_guru_modelcard' element in result xml");
    AssertNoErrors<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><block>value</block></ya_guru_modelcard>");
}

TEST_F(TTestMicroformats, TestFriendlyModelSchema)
{
    AssertError<NGumoful::ETemplateType::FRIENDLY_MODEL>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::FRIENDLY_MODEL>("<ya_guru_modelcard>value</ya_guru_modelcard>",
         "'block' element expected inside 'ya_guru_modelcard' element in result xml");
    AssertError<NGumoful::ETemplateType::FRIENDLY_MODEL>("<ya_guru_modelcard>"
                                                                 "<block>"
                                                                     "<spec_guru_modelcard>value</spec_guru_modelcard>"
                                                                 "</block>"
                                                         "</ya_guru_modelcard>",
         "'value' element expected inside 'spec_guru_modelcard' element in result xml");

    AssertNoErrors<NGumoful::ETemplateType::FRIENDLY_MODEL>("<ya_guru_modelcard>"
                                                            "<block>"
                                                            "<spec_guru_modelcard><name>yyy</name><value>xxx</value></spec_guru_modelcard>"
                                                            "</block>"
                                                            "</ya_guru_modelcard>");
}

TEST_F(TTestMicroformats, TestSeoSchema)
{
    AssertError<NGumoful::ETemplateType::SEO>("ThisIsNotXml", "result is not valid xml");
    AssertError<NGumoful::ETemplateType::SEO>("<root>value</root>",
        "'type' element expected inside 'root' element in result xml");
    AssertError<NGumoful::ETemplateType::SEO>("<root><type/></root>",
        "'nominative' attribute expected inside 'type' element in result xml");
    AssertError<NGumoful::ETemplateType::SEO>("<root><type nominative=''/></root>",
        "'genitive' attribute expected inside 'type' element in result xml");
    AssertError<NGumoful::ETemplateType::SEO>("<root><type nominative='' genitive=''/></root>",
        "'dative' attribute expected inside 'type' element in result xml");
    AssertError<NGumoful::ETemplateType::SEO>("<root><type nominative='' genitive='' dative=''/></root>",
        "'accusative' attribute expected inside 'type' element in result xml");
    AssertNoErrors<NGumoful::ETemplateType::SEO>("<root><type nominative='' genitive='' dative='' accusative=''/></root>");
}

TEST_F(TTestMicroformats, TestInvalidXmlDoubleQuotes)
{
    AssertNoErrors<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><big_picture SizeX=\"{XLPictureSizeX}\"></big_picture><block>value</block></ya_guru_modelcard>");
    AssertError<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><big_picture SizeX=\"{XLPictureSizeX}\"\"></big_picture><block>value</block></ya_guru_modelcard>",
        "result is not valid xml");
}

TEST_F(TTestMicroformats, TestInvalidXmlCDATA)
{
    AssertNoErrors<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><![CDATA[{XL-Picture}]]><block>value</block></ya_guru_modelcard>");
    AssertError<NGumoful::ETemplateType::BRIEF_MODEL>("<ya_guru_modelcard><![CDATA[{XL-Picture}]  ]><block>value</block></ya_guru_modelcard>",
                                                      "result is not valid xml");
}
