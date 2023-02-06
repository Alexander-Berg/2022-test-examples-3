#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <market/gumoful/tools/utils/mbo_model.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
#include <util/stream/file.h>
#include <market/gumoful/tools/utils/mbo_protobuf_renderer.h>
#include <util/generic/maybe.h>

class TTestGumofulErrors : public ::testing::Test
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

    void AssertErrorCommon(const std::string& templateSourceCode,
                     const std::string& expectedError,
                     const TMaybe<NGumoful::TPosition>& expectedPosition,
                     const TMaybe<std::string>& expectedUnknownFieldName)
    {
        const auto errors = gumoful.Render<NGumoful::ETemplateType::PLAIN_STRING>(*category, *model, templateSourceCode).Exceptions;
        ASSERT_TRUE(!errors.empty());
        UNIT_ASSERT_STRING_CONTAINS(errors[0].Message, expectedError);
        if (expectedPosition) {
            ASSERT_TRUE(errors[0].ErrorPosition.Defined());
            ASSERT_EQ(expectedPosition->Offset, errors[0].ErrorPosition->Offset);
            ASSERT_EQ(expectedPosition->Length, errors[0].ErrorPosition->Length);
        }

        if (expectedUnknownFieldName) {
            ASSERT_TRUE(errors[0].UnknownFieldName.Defined());
            ASSERT_EQ(*expectedUnknownFieldName, *errors[0].UnknownFieldName);
        }
    }

    void AssertError(const std::string& templateSourceCode, const std::string& expectedError) {
        AssertErrorCommon(templateSourceCode, expectedError, Nothing(), Nothing());
    }

    void AssertError(const std::string& templateSourceCode,
                     const std::string& expectedError,
                     const NGumoful::TPosition& expectedPosition) {
        AssertErrorCommon(templateSourceCode, expectedError, expectedPosition, Nothing());
    }

    void AssertFieldNotFound(const std::string& templateSourceCode,
                     const std::string& expectedError,
                     const std::string& unknownFiledName) {
        AssertErrorCommon(templateSourceCode, expectedError, Nothing(), unknownFiledName);
    }

    void AssertNoErrors(const std::string& templateSourceCode) {
        const auto errors = gumoful.Render<NGumoful::ETemplateType::PLAIN_STRING>(*category, *model, templateSourceCode).Exceptions;
        ASSERT_TRUE(errors.empty());
    }


    NGumoful::TModelTemplateRenderer gumoful;
    THolder<TMboCategory> category;
    THolder<NGumoful::IModel> model;
};

TEST_F(TTestGumofulErrors, TestTextOnly)
{
    AssertNoErrors("");
    AssertNoErrors("Hello world!");
    AssertNoErrors("<ya_guru_modelcard>\n\n<block name=\"Технические характеристики\">");
}

TEST_F(TTestGumofulErrors, TestGumofulErorrs)
{
    AssertError("{#else}", "'else' is not expected", {2, 4});
    AssertError("Hello {#else}", "'else' is not expected", {8, 4});
    AssertError("if hello {#else}", "'else' is not expected", {11, 4});
    AssertError("{#endif}", "'endif' is not expected", {2, 5});
    AssertError("{GPS#ifnz}", "1 'endif' at the end of card template", {10, 0});
    AssertError("{GPS#ifz}", "1 'endif' at the end of card template", {9, 0});
    AssertError("<![CDATA[{GPS#ifz}]]{#endif}", "expected 1 'endif' befor ]]", {18, 0});


    AssertNoErrors("{GPS#ifnz}if hello {#else}{#endif}");
    AssertNoErrors("{GPS#ifnz}hello{#endif}");
    AssertNoErrors("{GPS#ifz}hello{#endif}");
}

TEST_F(TTestGumofulErrors, TestExecSyntaxErrors)
{
    AssertError("{ string ;  #exec}", "No ident in var def", {9, 0});
    AssertError("{ somefunc() ;  #exec}", "call to undefined function", {2, 8});
    AssertError("{ (int 5 ;  #exec}", "')' after '('", {7, 0});
    AssertError("{ (5 + 4 * 7 ;  #exec}", "')' after '('", {13, 0});
    AssertError("{ 4 == 5 ? 2 ;  #exec}", "':' for '?'", {13, 0});
    AssertError("{ int x = 4  #exec}", "';' in initializer", {11, 0});
    AssertError("{ int x  #exec}", "';' in initializer", {7, 0});
    AssertError("{ if x > 5  #exec}", "'(' in if", {5, 0});
    AssertError("{ if (x > 5  #exec}", "')' in if", {11, 0});
    AssertError("{ while x > 5  #exec}", "'(' in while", {8, 0});
    AssertError("{ while (x > 5  #exec}", "')' in while", {14, 0});
    AssertError("{ for x = 0; x < 5; x++ #exec}", "'(' in for", {6, 0});
    AssertError("{ for (x = 0 x < 5; x++ #exec}", "1st ';' in for", {13, 0});
    AssertError("{ for (x = 0; x < 5 x++ #exec}", "2nd ';' in for", {20, 0});
    AssertError("{ for (x = 0; x < 5; x++ #exec}", "')' in for", {24, 0});
    AssertError("{ return 5 #exec}", "';' after return", {10, 0});
    AssertError("{ if (x > 5) { return 4; #exec}", "'}'expected", {24, 0});
    AssertError("{ print 3 #exec}", "';' after print", {9, 0});
    AssertError("{ function () return 4;  #exec}", "no func name in func def", {11, 0});
    AssertError("{ function f) return 4;  #exec}", "'(' in function definition", {12, 0});
    AssertError("{ function f(5) return 4;  #exec}", "no arg name in func def", {13, 0});
    AssertError("{ function f(x return 4;  #exec}", "')' in function definition", {15, 0});
    AssertError("{ function f(x) return 4;  #exec}", "'{' in function definition",  {16, 0});
    AssertError("{ x=4  #exec}", "';' after expression", {5, 0});
}

TEST_F(TTestGumofulErrors, TestGumofulRuntimeErrors)
{
    AssertFieldNotFound("{GPS#ifnz} {GPSLOLO} {#endif}", "Field 'GPSLOLO' not found in category", "GPSLOLO");
    AssertFieldNotFound("{GPSLOLO#ifnz} {#endif}", "Field 'GPSLOLO' not found in category", "GPSLOLO");
}

TEST_F(TTestGumofulErrors, TestExecRuntimeErrors)
{
    AssertFieldNotFound("{ return $UNKNOWNFIELD; #exec}", "Field 'UNKNOWNFIELD' not found in category", "UNKNOWNFIELD");
}
