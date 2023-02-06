#include <library/cpp/testing/unittest/registar.h>

#include <market/gumoful/tools/utils/mbo_model.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
#include <util/stream/file.h>
#include <market/gumoful/tools/utils/mbo_protobuf_renderer.h>
#include <market/proto/content/mbo/CsGumoful.pb.h>

using namespace NGumoful;

Y_UNIT_TEST_SUITE(TestMboProtobufRenderer)
{
    Y_UNIT_TEST(TestRealTemplates)
    {
        using namespace ru::yandex::market::mbo::Export;
        const auto categoryMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/category_91491.pb")).ReadAll();
        const auto modelMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/model_10495456.pb")).ReadAll();

        TVarIntLenValueProtoReader categoryReader(categoryMessage);
        const auto category = *categoryReader.Read<TExportCategory>();

        TVarIntLenValueProtoReader modelReader(modelMessage);
        auto modelPb = *modelReader.Read<TExportReportModel>();

        // Добавляем значения из скю
        UngroupingInfo skuParams;
        ParameterValue param;
        // первая скю
        param.set_param_id(4940921);
        param.set_option_id(13475069); // - смартфон
        param.set_xsl_name("Type");
        *skuParams.add_parameter_values() = param;
        param.set_option_id(13475319); // - телефон для детей
        *skuParams.add_parameter_values() = param;
        *modelPb.add_blue_ungrouping_info() = std::move(skuParams);
        // вторая скю
        param.set_option_id(13475319); // - телефон для детей
        *skuParams.add_parameter_values() = param;
        param.set_option_id(13475071); // - телефон для пожилых
        *skuParams.add_parameter_values() = param;
        *modelPb.add_blue_ungrouping_info() = std::move(skuParams);

        // Добавляем гипотезы
        ParameterValueHypothesis hypothesis;
        hypothesis.set_param_id(4940921);
        hypothesis.set_xsl_name("Type");
        Market::Mbo::Parameters::Word hypValue;
        hypValue.set_name("без кнопок");
        *hypothesis.add_str_value() = hypValue;
        *modelPb.add_parameter_value_hypothesis() = hypothesis;

        TMboCategory categoryDataAdapter(category);

        NGumoful::TMboProtobufRenderer renderer;

        const auto result = renderer.Render(categoryDataAdapter, modelPb);

        UNIT_ASSERT_EQUAL(10495456, result.model_id());

        UNIT_ASSERT_EQUAL(6, result.template_rendering_results().size());

        TSet<mbo::TTemplateRenderingResult::ETemplateType> differentTemplateTypes;
        for (const auto& templateResult : result.template_rendering_results())
        {
            UNIT_ASSERT(!templateResult.rendering_result().empty());
            UNIT_ASSERT_EQUAL(0, templateResult.errors().size());
            differentTemplateTypes.insert(templateResult.template_type());
        }

        UNIT_ASSERT_EQUAL(6, differentTemplateTypes.size());

        UNIT_ASSERT_STRINGS_EQUAL("смартфон, телефон для детей, телефон для пожилых, без кнопок 3G, MP3, Bluetooth, Wi-Fi, GPS,  4G LTE, фотокамера камера --",
                                  result.micro_model_search_result());

        // Рендеринг cs_templates
        UNIT_ASSERT(result.has_cs_templates());
        UNIT_ASSERT_EQUAL("смартфон, телефон для детей, телефон для пожилых, без кнопок 3G, MP3, Bluetooth, Wi-Fi, GPS,  4G LTE, фотокамера камера --", result.cs_templates().GetMicro());
        UNIT_ASSERT(result.cs_templates().HasSeo());
        UNIT_ASSERT(result.cs_templates().GetFriendly().size() > 0);
        UNIT_ASSERT(result.cs_templates().GetFriendlyExt().size() > 0);
        UNIT_ASSERT(result.cs_templates().GetFull().size() > 0);

        // Строим только cs_templates
        const auto result_cs = renderer.Render(categoryDataAdapter, modelPb, /* addOldRenderingResult */ false);

        UNIT_ASSERT_EQUAL(10495456, result_cs.model_id());

        UNIT_ASSERT_EQUAL(0, result_cs.template_rendering_results().size());
        UNIT_ASSERT(!result_cs.has_micro_model_search_result());

        // Рендеринг cs_templates
        UNIT_ASSERT(result_cs.has_cs_templates());
        UNIT_ASSERT_EQUAL("смартфон, телефон для детей, телефон для пожилых, без кнопок 3G, MP3, Bluetooth, Wi-Fi, GPS,  4G LTE, фотокамера камера --", result_cs.cs_templates().GetMicro());
        UNIT_ASSERT(result_cs.cs_templates().HasSeo());
        UNIT_ASSERT(result_cs.cs_templates().GetFriendly().size() > 0);
        UNIT_ASSERT(result_cs.cs_templates().GetFriendlyExt().size() > 0);
        UNIT_ASSERT(result_cs.cs_templates().GetFull().size() > 0);

        // Проверяем логику с параметрами
        for (const auto& group : result_cs.cs_templates().GetFull()) {
            for (const auto& spec : group.GetGroupSpecs()) {
                if (spec.GetName() == "Тип") {
                    UNIT_ASSERT_EQUAL(spec.GetValue(), "смартфон, телефон для детей, телефон для пожилых, без кнопок");
                    for (const auto& usedParam : spec.GetUsedParamsWithValues()) {
                        if (usedParam.GetId() == 4940921) {
                            UNIT_ASSERT_EQUAL(usedParam.GetName(), "Тип");
                            UNIT_ASSERT_EQUAL(usedParam.GetUsedValues().size(), 4);
                            // Порядок важен
                            UNIT_ASSERT_EQUAL(usedParam.GetUsedValues(0).GetValue(), "13475069");
                            UNIT_ASSERT_EQUAL(usedParam.GetUsedValues(1).GetValue(), "13475071");
                            UNIT_ASSERT_EQUAL(usedParam.GetUsedValues(2).GetValue(), "13475319");
                            UNIT_ASSERT_EQUAL(usedParam.GetUsedValues(3).GetValue(), "без кнопок");
                        }
                    }
                }
            }
        }
    }

    Y_UNIT_TEST(TestCorrectTemplateWithoutModel) {
        const auto categoryMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/category_91491.pb")).ReadAll();
        TVarIntLenValueProtoReader categoryReader(categoryMessage);
        const auto category = *categoryReader.Read<TExportCategory>();
        TMboCategory categoryDataAdapter(category);
        NGumoful::TMboProtobufRenderer renderer;

        const auto result = renderer.RenderDummyModels(categoryDataAdapter);
        UNIT_ASSERT_EQUAL(12, result.template_rendering_results().size());
        for (const auto& templateResult : result.template_rendering_results())
        {
            UNIT_ASSERT(!templateResult.rendering_result().empty());
            UNIT_ASSERT_EQUAL(0, templateResult.errors().size());
        }

    }

    Y_UNIT_TEST(TestIncorrectTemplateWithoutModel) {
        const auto categoryTemplate = "{#else}";
        TExportCategory category;
        *category.mutable_micro_model_template() = categoryTemplate;
        *category.mutable_model_template() = categoryTemplate;
        *category.mutable_brief_model_template() = categoryTemplate;
        *category.mutable_micro_model_search_template() = categoryTemplate;
        *category.mutable_friendly_model_template() = categoryTemplate;
        *category.mutable_seo_template() = categoryTemplate;

        TMboCategory categoryDataAdapter(category);
        NGumoful::TMboProtobufRenderer renderer;

        const auto result = renderer.RenderDummyModels(categoryDataAdapter);
        UNIT_ASSERT_EQUAL(12, result.template_rendering_results().size());
        for (const auto& templateResult : result.template_rendering_results())
        {
            UNIT_ASSERT(!templateResult.errors().empty());
        }
    }
}
