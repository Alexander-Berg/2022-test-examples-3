#include <library/cpp/testing/unittest/registar.h>

#include <market/gumoful/tools/utils/mbo_model.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
#include <util/stream/file.h>
using namespace NGumoful;

namespace {
    Market::Mbo::Parameters::Word CreateWord(const TString& word) {
        Market::Mbo::Parameters::Word result;
        result.set_name(word);
        return result;
    }
}

Y_UNIT_TEST_SUITE(TestMboModel)
{
    Y_UNIT_TEST(TestRealProperties)
    {
        const auto categoryMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/category_91491.pb")).ReadAll();
        const auto modelMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/model_10495456.pb")).ReadAll();

        TVarIntLenValueProtoReader categoryReader(categoryMessage);
        const auto category = *categoryReader.Read<TExportCategory>();

        TVarIntLenValueProtoReader modelReader(modelMessage);
        const auto modelPb = *modelReader.Read<TExportReportModel>();

        TMboCategory mboCategory(category);
        TMboModel model(modelPb, category);

        UNIT_ASSERT_EQUAL(mv_bool, model.GetField("GPS").type());
        UNIT_ASSERT_EQUAL(true, model.GetField("GPS").as_bool());

        UNIT_ASSERT_EQUAL(mv_bool, model.GetField("twosim").type());
        UNIT_ASSERT_EQUAL(false, model.GetField("twosim").as_bool());

        UNIT_ASSERT_EQUAL(mv_numeric, model.GetField("MemoryVolumeGB").type());
        UNIT_ASSERT_DOUBLES_EQUAL(16.0, model.GetField("MemoryVolumeGB").as_double(), 1e-10);

        UNIT_ASSERT_EQUAL(mv_string, model.GetField("AnnounceDate").type());
        UNIT_ASSERT_STRINGS_EQUAL("2013-09-10", model.GetField("AnnounceDate").c_str());

        UNIT_ASSERT_EQUAL(mv_string, model.GetField("Type").type());
        UNIT_ASSERT_STRINGS_EQUAL("смартфон", model.GetField("Type").c_str());

        UNIT_ASSERT_EQUAL(mv_string, model.GetField("SmartPhoneOS").type());
        UNIT_ASSERT(model.GetField("SmartPhoneOS").is_null());
    }

    Y_UNIT_TEST(TestMultivalueVariable)
    {
        using MboValueType = Market::Mbo::Parameters::ValueType;

        TExportCategory category;
        auto *multiParam = category.add_parameter();
        multiParam->set_multivalue(true);
        multiParam->set_id(42);
        multiParam->set_xsl_name("MultivalueParam");
        multiParam->set_value_type(MboValueType::ENUM);

        auto* option = multiParam->add_option();
        option->set_id(123);
        *option->add_name() = CreateWord("ValueA");

        option = multiParam->add_option();
        option->set_id(124);
        *option->add_name() = CreateWord("ValueB");

        option = multiParam->add_option();
        option->set_id(125);
        *option->add_name() = CreateWord("ValueC");

        TMboCategory mboCategory(category);

        TExportReportModel pbModel;
        pbModel.set_category_id(42);

        auto* parameter = pbModel.add_parameter_values();
        parameter->set_param_id(42);
        parameter->set_type_id(1);
        parameter->set_option_id(125);
        parameter->set_xsl_name("MultivalueParam");

        parameter = pbModel.add_parameter_values();
        parameter->set_param_id(42);
        parameter->set_type_id(1);
        parameter->set_option_id(123);
        parameter->set_xsl_name("MultivalueParam");

        TMboModel model(pbModel, category);
        UNIT_ASSERT_EQUAL(mv_string, model.GetField("MultivalueParam").type());
        UNIT_ASSERT_STRINGS_EQUAL("ValueA, ValueC", model.GetField("MultivalueParam").c_str());

        // Testing with hypothesis
        using MboValueType = Market::Mbo::Parameters::ValueType;

        auto* hypothes = pbModel.add_parameter_value_hypothesis();
        hypothes->set_param_id(42);
        hypothes->set_xsl_name("MultivalueParam");
        auto* str_value = hypothes->add_str_value();
        str_value->set_name("ValueE");

        hypothes = pbModel.add_parameter_value_hypothesis();
        hypothes->set_param_id(42);
        hypothes->set_xsl_name("MultivalueParam");
        str_value = hypothes->add_str_value();
        str_value->set_name("ValueD");

        TMboModel modelWithHypothesis(pbModel, category);
        UNIT_ASSERT_EQUAL(mv_string, modelWithHypothesis.GetField("MultivalueParam").type());
        UNIT_ASSERT_STRINGS_EQUAL("ValueA, ValueC, ValueD, ValueE", modelWithHypothesis.GetField("MultivalueParam").c_str());
    }
}
