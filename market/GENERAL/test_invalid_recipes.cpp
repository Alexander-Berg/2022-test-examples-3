#include <sstream>

#include <library/cpp/testing/unittest/gtest.h>
#include <market/tools/recipes_xml_to_mmap_converter/ut/util.h>

using namespace NMarket::NRecipes;

void MakeTest(const TString& source, const TString& expected_err) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    std::stringstream buffer;
    std::cerr.rdbuf(buffer.rdbuf());

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(source, NXml::TDocument::String);

    UNIT_ASSERT_STRING_CONTAINS(buffer.str(), expected_err);
}

TEST(PARSER, RECIPE_WITHOUT_FILTERS) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"13\" hid=\"7811944\" name=\"Recipe without filters\" header=\"Recipe without filters\" popularity=\"1\" "
            "       sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Recipe #13. It is invalid because it has no valid filters");
}

TEST(PARSER, RECIPE_WITHOUT_PARAM_ID) {
    const TString source =
            "<recipes>\n"
            "   <recipe id=\"12\" hid=\"7811944\" name=\"No paramId\" header=\"No paramId\" popularity=\"1\" sponsored=\"0\" is_seo=\"1\" "
            "       contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>"
            "</recipes>";

    MakeTest(source, "AttributeNotFound: /recipes/recipe/filter[1]@param_id");
}

TEST(PARSER, MULTIPLE_BOOLEAN_VALUES) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"10\" hid=\"7811944\" name=\"Multiple boolean values\" header=\"Multiple Boolean values\" popularity=\"1\" "
            "       sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"0\" />\n"
            "            <value id=\"1\" />\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Boolean filter with paramId = 3 has more then one value");
}

TEST(PARSER, INVALID_NUMERIC_FILTER) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"11\" hid=\"7811944\" name=\"Invalid numeric filter\" header=\"Invalid numeric filter\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"2\" type=\"number\" />\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Numeric filter with paramId = 2 has neither min_value nor max_value");
}

TEST(PARSER, INVALID_BOOLEAN_VALUE) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"9\" hid=\"7811944\" name=\"Invalid boolean value\" header=\"Invalid Boolean value\" popularity=\"1\" "
            "       sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"qwe\" />\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Failed to convert string \"qwe\" from 'id' to requested type");
}

TEST(PARSER, NO_BOOLEAN_VALUE) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"8\" hid=\"7811944\" name=\"No boolean value\" header=\"No Boolean value\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Boolean filter with paramId = 3 has no values");
}

TEST(PARSER, NO_ENUM_VALUE) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"7\" hid=\"7811944\" name=\"Not valid enum\" header=\"Not valid enum\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "            <value id=\"10977907\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Enum filter with paramId = 1 has no valid values");
}

TEST(PARSER, NO_FILTER_TAG) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"6\" hid=\"7811944\" name=\"No filter tag\" header=\"No filter tag\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <invalid_tag param_id=\"1\" type=\"invalid\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </invalid_tag>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Expected node filter. But node with name \"invalid_tag\" was passed");
}

TEST(PARSER, INVALID_TYPE_OF_FILTER) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"5\" hid=\"7811944\" name=\"Invalid filter's type\" header=\"Invalid filter's type\" popularity=\"1\" "
            "       sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"invalid\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Filter with paramId = 1 has unsupported type (value: \"invalid\")");
}

TEST(PARSER, NO_TYPE_OF_FILTER) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"4\" hid=\"7811944\" name=\"No filter's type\" header=\"No filter's type\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "AttributeNotFound: /recipes/recipe/filter[1]@type");
}

TEST(PARSER, INVALID_PARAM_ID) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"3\" hid=\"7811944\" name=\"param_id=qwe у фильтра\" header=\"param_id=qwe у фильтра\" popularity=\"1\" "
            "       sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"qwe\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "Failed to convert string \"qwe\" from 'param_id' to requested type");
}

TEST(PARSER, INVALID_RECIPE_TAG) {
    const TString source =
            "<recipes>\n"
            "    <invalid_tag id=\"1\" hid=\"7811944\" name=\"Не рецепт\" header=\"Не рецепт\" popularity=\"1\" sponsored=\"0\" "
            "       is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </invalid_tag>\n"
            "</recipes>";

    MakeTest(source, "Expected node recipe. But node with name \"invalid_tag\" was passed");
}

TEST(PARSER, NO_RECIPES_TAG) {
    const TString source =
            "<invalid_tag>\n"
            "    <recipe id=\"1\" hid=\"7811944\" name=\"Валидный\" header=\"Валидный рецепт\" popularity=\"1\" sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</invalid_tag>";

    MakeTest(source, "Expected node recipes. But node with name \"invalid_tag\" was passed");
}

TEST(PARSER, NO_CATEGORY_ID) {
    const TString source =
            "<recipes>\n"
            "    <recipe id=\"1\" name=\"Нет категории\" header=\"Нет категории\" popularity=\"1\" sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "AttributeNotFound: /recipes/recipe@hid");
}

TEST(PARSER, NO_RECIPE_ID) {
    const TString source =
            "<recipes>\n"
            "    <recipe hid=\"7811944\" name=\"Нет категории\" header=\"Нет категории\" popularity=\"1\" sponsored=\"0\" is_seo=\"1\" contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" without_filters=\"0\">\n"
            "        <filter param_id=\"1\" type=\"enum\">\n"
            "            <value id=\"10977906\"/>\n"
            "        </filter>\n"
            "        <filter param_id=\"2\" type=\"number\" min_value=\"1900\" max_value=\"1900\" />\n"
            "        <filter param_id=\"3\" type=\"boolean\">\n"
            "            <value id=\"1\"/>\n"
            "        </filter>\n"
            "    </recipe>\n"
            "</recipes>";

    MakeTest(source, "AttributeNotFound: /recipes/recipe@id");
}
