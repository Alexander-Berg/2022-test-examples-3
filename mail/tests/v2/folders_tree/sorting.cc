#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <algorithm>
#include <random>
#include <array>

#include <boost/locale/encoding_utf.hpp>
#include <boost/locale.hpp>

#include <mail/hound/include/internal/v2/folders_tree/sorting.h>
#include <mail/hound/include/internal/v2/folders_tree/method.h>
#include <mail/hound/include/internal/v2/folders_tree/folder_reflection.h>
#include <macs/folder_factory.h>
#include "../helpers.h"

namespace hound::server::handlers::v2::folders_tree {

bool operator==(const Folder& lhs, const Folder& rhs) {
    return lhs.id() == rhs.id()
        && lhs.subfolders() == rhs.subfolders()
        && lhs.displayName() == rhs.displayName()
        && lhs.parentId() == rhs.parentId();
}

std::ostream& operator<<(std::ostream& o, const Folder& f) {
    using namespace boost::locale::conv;
    o << "{ \"id\": \"" << f.id() << "\", \"name\": \"" << f.displayName() << "\", \"parentId\": \"" << f.parentId() << "\", \"subfolders\": [";
    for (const auto& subfolder : f._subfolders) {
        o << subfolder << ", ";
    }
    o << "] }";
    return o;
}

} // namespace hound::server::handlers::v2::folders

namespace {

using namespace hound::server::handlers::v2::folders_tree;
using namespace hound::testing;
using namespace ::testing;
using macs::FolderFactory;

struct FolderComparators : Test {
    boost::locale::generator gen;
};

TEST_F(FolderComparators, should_sort_folders_by_date) {
    const auto folder = [] (const std::string& fid, const std::string& name, const std::string& unixtime) {
        return std::make_pair(fid, FolderFactory()
                .fid(fid)
                .creationTime(unixtime)
                .name(name)
                .type(macs::Folder::Type::user)
                .product()
        );
    };

    const macs::FolderSet folderSet({
        folder("1", "inbox", "1530000000"),
        folder("20", "russian", "1529999999"),
        folder("22", "русская", "1530000000"),
        folder("23", "азбука", "1529999998"),
        folder("24", "яблоко", "1530000010"),
        folder("21", "Ё", "1530000001"),
        folder("2", "outbox", "1530000010")
    });
    const auto result = foldersSort(folderSet, LessByDateSortKey(),
            PathSortKey(lang2locale("ru").value()));

    EXPECT_THAT(map(result, creationTimeExtractor), ElementsAre("1529999998", "1529999999", "1530000000", "1530000000",
            "1530000001", "1530000010", "1530000010"));
}

TEST_F(FolderComparators, should_sort_folders_by_position) {
    const auto folder = [] (const std::string& fid, const std::string& name, const std::size_t position = 0) {
        return std::make_pair(fid, FolderFactory()
                .fid(fid)
                .name(name)
                .type(macs::Folder::Type::user)
                .position(position)
                .product()
        );
    };

    const macs::FolderSet folderSet({
        folder("1", "inbox"),
        folder("22", "russian"),
        folder("32", "аа"),
        folder("33", "гг"),
        folder("23", "аa", 100),
        folder("24", "бб", 200),
        folder("25", "ёё", 250),
        folder("26", "яя", 300),
    });

    const auto result = foldersSort(folderSet, PositionSortKey(), PathSortKey(gen("C")));
    EXPECT_THAT(map(result, idExtractor), ElementsAre("1", "22", "32", "33", "23", "24", "25", "26"));
}

TEST_F(FolderComparators, sorting_by_position_should_consider_all_folder_symbols_from_macs) {
    using Symbol = macs::Folder::Symbol;
    const auto cmp = SymbolSortKey();
    std::set<Symbol> sorting(cmp._positions.begin(), cmp._positions.end());
    std::set<Symbol> macs;
    std::transform(Symbol::getDict().begin(), Symbol::getDict().end(), std::inserter(macs, macs.end()), [] (const Symbol* smb) { return *smb; });
    EXPECT_THAT(sorting, ContainerEq(macs));
}

TEST_F(FolderComparators, sorting_by_position_for_folders_with_same_position_should_sort_by_name_ignoring_case) {
    const auto folder = [] (const std::string& fid, const std::string& name) {
        return std::make_pair(fid, FolderFactory()
                .fid(fid)
                .name(name)
                .type(macs::Folder::Type::user)
                .product()
        );
    };

    const macs::FolderSet folderSet({
        folder("1", "AA"),
        folder("2", "ZZ"),
        folder("3", "hh"),
    });

    const auto result = foldersSort(folderSet, PositionSortKey(), PathSortKey(gen("C")));
    EXPECT_THAT(map(result, idExtractor), ElementsAre("1", "3", "2"));
}

TEST_F(FolderComparators, should_sort_folders_by_locale_within_one_level) {
    const std::vector<std::wstring> russian = {
            L"Азбука",
            L"азбука",
            L"ёлка",
            L"ягода"
    };
    const std::vector<std::wstring> ukrainian = {
            L"абетка",
            L"Від",
            L"їхньому",
            L"яйцеподібний"
    };
    const std::vector<std::wstring> english = {
            L"Alphabet",
            L"link",
            L"Web",
            L"Z"
    };
    const std::vector<std::wstring> turkish = {
            L"alfabesi",
            L"lüleburgaz",
            L"Şapkalı"
    };
    const std::vector<std::wstring> belorussian = {
            L"алфавіт",
            L"сілкуецца",
            L"узятая"
    };
    std::vector<std::wstring> world;
    for (const auto& i : {russian, ukrainian, english, turkish, belorussian}) {
        std::copy(i.begin(), i.end(), std::back_inserter(world));
    }

    ASSERT_EQ(world.size(), russian.size() + ukrainian.size() + english.size() + turkish.size() + belorussian.size());
    using namespace boost::locale::conv;
    std::map<macs::Fid, const macs::Folder> folders;
    long int fid = 30;
    for (const auto& name : world) {
        const auto newFolder = FolderFactory()
                .name(utf_to_utf<char>(name))
                .fid(std::to_string(fid))
                .type(macs::Folder::Type::user)
                .product();
        ASSERT_EQ(newFolder.name(), macs::detail::normalizeName(utf_to_utf<char>(name)));
        folders.insert({std::to_string(fid), newFolder});
        ++fid;
    }
    macs::FolderSet folderSet(folders);
    ASSERT_EQ(folderSet.size(), world.size());

    const auto result = foldersSort(folderSet, PathSortKey(gen("ru_RU.UTF-8")));

    const std::vector<std::string> sorted = {
            "абетка",
            "азбука",
            "Азбука",
            "алфавіт",
            "Від",
            "ёлка",
            "їхньому",
            "сілкуецца",
            "узятая",
            "ягода",
            "яйцеподібний",
            "alfabesi",
            "Alphabet",
            "link",
            "lüleburgaz",
            "Şapkalı",
            "Web",
            "Z",
    };
    EXPECT_THAT(map(result, displayNameExtractor), ContainerEq(sorted));
}

TEST_F(FolderComparators, should_sort_folders_by_locale_with_hierarchy_by_meta_sort) {
    using Type = macs::Folder::Type;
    using Symbol = macs::Folder::Symbol;

    const auto macsFolder = [] (
            const std::string& fid
            , const std::string &name
            , Type type
            , Symbol symbol
            , const std::string& pfid = macs::Folder::noParent
    ) {
        return std::make_pair(fid, FolderFactory()
                .fid(fid)
                .name(name)
                .parentId(pfid)
                .symbol(symbol)
                .type(type)
                .product()
        );
    };

    const macs::FolderSet folderSet({
        macsFolder("1", "Inbox", Type::system, Symbol::inbox),
        macsFolder("20", "russian", Type::user, Symbol::defValue()),
        macsFolder("22", "русская", Type::user, Symbol::defValue()),
            macsFolder("23", "яблоко", Type::user, Symbol::defValue(), "22"),
            macsFolder("25", "ёлка", Type::user, Symbol::defValue(), "22"),
            macsFolder("26", "Ёлка", Type::user, Symbol::defValue(), "22"),
                macsFolder("32", "гг", Type::user, Symbol::defValue(), "26"),
                macsFolder("31", "ыы", Type::user, Symbol::defValue(), "26"),
                macsFolder("30", "аа", Type::user, Symbol::defValue(), "26"),
            macsFolder("24", "азбука", Type::user, Symbol::defValue(), "22"),
        macsFolder("21", "Ё", Type::user, Symbol::defValue()),
        macsFolder("5", "Outbox", Type::system, Symbol::outbox),
        macsFolder("3", "Trash", Type::system, Symbol::trash)
    });

    const std::vector<Folder> result = foldersSort(folderSet, PathSortKey(gen("ru_RU.UTF-8")));

    const std::vector<Folder> sorted = {
        Folder(folderSet.at("1")),
        Folder(folderSet.at("21")),
        Folder{folderSet.at("22"), {
            Folder(folderSet.at("24")),
            Folder(folderSet.at("25")),
            Folder{folderSet.at("26"), {
                Folder(folderSet.at("30")),
                Folder(folderSet.at("32")),
                Folder(folderSet.at("31"))
            }},
            Folder(folderSet.at("23"))
        }},
        Folder(folderSet.at("20")),
        Folder(folderSet.at("3")),
        Folder(folderSet.at("5"))
    };

    EXPECT_THAT(result, ContainerEq(sorted));
}

TEST_F(FolderComparators, should_return_empty_result_for_empty_input) {
    macs::FolderSet folderSet;
    const std::vector<Folder> result = foldersSort(folderSet, LessByDateSortKey(),
            PathSortKey(lang2locale("ru").value()));
    EXPECT_EQ(0ul, result.size());
}

TEST_F(FolderComparators, should_insert_all_user_folders_after_first_system) {
    using Type = macs::Folder::Type;
    using Symbol = macs::Folder::Symbol;
    const macs::FolderSet folderSet({
        {"1", FolderFactory().fid("1").type(Type::system).symbol(Symbol::inbox).product()},
        {"2", FolderFactory().fid("2").type(Type::system).symbol(Symbol::drafts).product()},
        {"10", FolderFactory().fid("10").type(Type::user).position(200).product()},
        {"20", FolderFactory().fid("20").type(Type::user).position(100).product()}
    });

    const std::vector<Folder> result = foldersSort(folderSet, PositionSortKey(), PathSortKey(gen("C")));
    EXPECT_THAT(map(result, idExtractor), ElementsAre("1", "20", "10", "2"));
}

struct CollationTest : public ::testing::TestWithParam<std::pair<std::string, std::vector<std::string>>> {
    const std::vector<std::wstring> data{
            L"ასტრა",
            L"lüleburgaz",
            L"їдальня",
            L"іноземець",
            L"куәландырушы",
            L"ўбрыкнуць",
            L"luwak",
            L"գործընթացն"
    };
};

INSTANTIATE_TEST_SUITE_P(should_sort_folders_by_all_supported_locale, CollationTest, ::testing::Values(
            std::make_pair("az", std::vector<std::string>({"luwak", "lüleburgaz", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა", "գործընթացն"})),
            std::make_pair("be", std::vector<std::string>({"їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "lüleburgaz", "luwak", "ასტრა", "գործընթացն"})),
            std::make_pair("en", std::vector<std::string>({"lüleburgaz", "luwak", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა", "գործընթացն"})),
            std::make_pair("hy", std::vector<std::string>({"գործընթացն", "lüleburgaz", "luwak", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა"})),
            std::make_pair("ka", std::vector<std::string>({"ასტრა", "lüleburgaz", "luwak", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "գործընթացն"})),
            std::make_pair("ro", std::vector<std::string>({"lüleburgaz", "luwak", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა", "գործընթացն"})),
            std::make_pair("ru", std::vector<std::string>({"їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "lüleburgaz", "luwak", "ასტრა", "գործընթացն"})),
            std::make_pair("kk", std::vector<std::string>({"куәландырушы", "ўбрыкнуць", "їдальня", "іноземець", "lüleburgaz", "luwak", "ასტრა", "գործընթացն"})),
            std::make_pair("tr", std::vector<std::string>({"luwak", "lüleburgaz", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა", "գործընթացն"})),
            std::make_pair("tt", std::vector<std::string>({"lüleburgaz", "luwak", "їдальня", "іноземець", "куәландырушы", "ўбрыкнуць", "ასტრა", "գործընթացն"})),
            std::make_pair("uk", std::vector<std::string>({"іноземець", "їдальня", "куәландырушы", "ўбрыкнуць", "lüleburgaz", "luwak", "ასტრა", "գործընթացն"}))
));

TEST_P(CollationTest, should_sort_folders_by_all_supported_locale) {
    const auto [lang, expected] = GetParam();

    std::map<std::string, const macs::Folder> folders;
    {
        size_t fid = 30;
        using namespace boost::locale::conv;
        for (const auto& name : data) {
            folders.insert({std::to_string(fid), FolderFactory()
                .fid(std::to_string(fid))
                .name(utf_to_utf<char>(name))
                .type(macs::Folder::Type::user)
                .product()});
            ++fid;
        }
    }

    macs::FolderSet folderSet(folders);
    const std::vector<Folder> result = foldersSort(folderSet, PathSortKey(lang2locale(lang).value()));
    EXPECT_THAT(map(result, displayNameExtractor), ContainerEq(expected));
}

} // namespace

