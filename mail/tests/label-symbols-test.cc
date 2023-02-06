#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/label.h>
#include <macs/test_environment/rands.h>

namespace macs {
    inline std::ostream & operator << (std::ostream & s, const Label::Symbol& symbol) {
        return s << "symbol {" << symbol.code() << ", " << symbol.title() << "}";
    }
}

namespace {
    using namespace ::testing;
    using namespace macs;

    TEST(LabelSymbolsTest, getByTitle_withEmptyString_returnsSymbolNone) {
        ASSERT_EQ(Label::Symbol::none, Label::Symbol::getByTitle(""));
    }

    TEST(LabelSymbolsTest, getByTitle_withNonExistingTitle_returnsSymbolNone) {
        ASSERT_EQ(Label::Symbol::none, Label::Symbol::getByTitle(Rands::getString(1, 64)));
    }

    TEST(LabelSymbolsTest, getByTitle_withExistingTitle_returnsCorrectSymbol) {
        ASSERT_EQ(Label::Symbol::spam_label, Label::Symbol::getByTitle("spam_label"));
    }

    TEST(LabelSymbolsTest, getByCode_withNonExistingCode_returnsSymbolNone) {
        ASSERT_EQ(Label::Symbol::none, Label::Symbol::getByCode(666));
    }

    TEST(LabelSymbolsTest, getByCode_withExistingCode_returnsCorrectSymbol) {
        ASSERT_EQ(Label::Symbol::spam_label, Label::Symbol::getByCode(macs::SYM_SPAM_LBL));
    }
}
