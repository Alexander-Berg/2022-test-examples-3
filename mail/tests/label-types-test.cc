#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/label.h>
#include <macs/test_environment/rands.h>

namespace macs {
    inline std::ostream & operator << (std::ostream & s, const Label::Type& type) {
        return s << "type {" << type.code() << ", " << type.title() << "}";
    }
}

namespace {
    using namespace ::testing;
    using namespace macs;

    TEST(LabelTypesTest, getByTitle_withEmptyString_returnsTypeUser) {
        ASSERT_EQ(Label::Type::user, Label::Type::getByTitle(""));
    }

    TEST(LabelTypesTest, getByTitle_withNonExistingTitle_returnsTypeUser) {
        ASSERT_EQ(Label::Type::user, Label::Type::getByTitle(Rands::getString(1, 64)));
    }

    TEST(LabelTypesTest, getByTitle_withExistingTitle_returnsCorrectType) {
        ASSERT_EQ(Label::Type::social, Label::Type::getByTitle("social"));
    }

    TEST(LabelTypesTest, getByCode_withNonExistingCode_returnsTypeUser) {
        ASSERT_EQ(Label::Type::user, Label::Type::getByCode(666));
    }
}
