#include <extsearch/geo/kernel/recommendations/leisure_rubrics.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch;

Y_UNIT_TEST_SUITE(TTestLeisureRubrics) {
    Y_UNIT_TEST(TestGetLeisure) {
        const auto& leisureRubrics = GetLeisureRubrics();
        UNIT_ASSERT(leisureRubrics.contains(184106394));  // restaurant
        UNIT_ASSERT(leisureRubrics.contains(184105868));  // cinema
        UNIT_ASSERT(!leisureRubrics.contains(184105716)); // government

        const auto& leisureRubricsExtended = GetExtendedLeisureRubrics();
        UNIT_ASSERT(leisureRubricsExtended.contains(184106394));  // restaurant
        UNIT_ASSERT(leisureRubricsExtended.contains(184105868));  // cinema
        UNIT_ASSERT(leisureRubricsExtended.contains(184105814));  // beauty studio
        UNIT_ASSERT(!leisureRubricsExtended.contains(184105716)); // government

        const auto& leisureRubricAcronyms = GetExtendedLeisureRubricAcronyms();
        UNIT_ASSERT(leisureRubricAcronyms.at("184106394") == "restaurant"); // restaurant
    }
}
