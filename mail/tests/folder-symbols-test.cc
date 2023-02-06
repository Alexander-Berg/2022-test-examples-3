#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/folder.h>
#include <macs/test_environment/rands.h>

namespace macs {
    inline std::ostream & operator << (std::ostream & s, const Folder::Symbol& symbol) {
        return s << "symbol {" << symbol.code() << ", " << symbol.title() << "}";
    }
}

namespace {
    using namespace ::testing;
    using namespace macs;

    TEST(FolderSymbolsTest, getByTitle_withEmptyString_returnsSymbolNone) {
        ASSERT_EQ(Folder::Symbol::none, Folder::Symbol::getByTitle(""));
    }

    TEST(FolderSymbolsTest, getByTitle_withNonExistingTitle_returnsSymbolNone) {
        ASSERT_EQ(Folder::Symbol::none, Folder::Symbol::getByTitle(Rands::getString(1, 64)));
    }

    TEST(FolderSymbolsTest, getByTitle_withExistingTitle_returnsCorrectSymbol) {
        ASSERT_EQ(Folder::Symbol::inbox, Folder::Symbol::getByTitle("inbox"));
    }

    TEST(FolderSymbolsTest, getByCode_withNonExistingCode_returnsSymbolNone) {
        ASSERT_EQ(Folder::Symbol::none, Folder::Symbol::getByCode(666));
    }

    TEST(FolderSymbolsTest, getByCode_withExistingCode_returnsCorrectSymbol) {
        ASSERT_EQ(Folder::Symbol::inbox, Folder::Symbol::getByCode(macs::SYM_INBOX));
    }
}
