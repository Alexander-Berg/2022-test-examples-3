#include <crypta/lib/native/string_archive/string_archive.h>
#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TStringArchive) {
    using namespace NCrypta::NStringArchive;

    Y_UNIT_TEST(Empty) {
        auto archive = Archive({});
        auto unarchive = Unarchive(archive);

        UNIT_ASSERT(unarchive.empty());
    }

    Y_UNIT_TEST(SingleFile) {
        TVector<TFileInfo> files = {
            {"100.txt", "100"}};

        auto archive = Archive(files);
        auto unarchive = Unarchive(archive);

        UNIT_ASSERT(files == unarchive);
    }

    Y_UNIT_TEST(SeveralFiles) {
        TVector<TFileInfo> files = {
            {"1.txt", "0"},
            {"2.txt", "1"},
            {"3.txt", "2"}};

        auto archive = Archive(files);
        auto unarchive = Unarchive(archive);

        UNIT_ASSERT(files == unarchive);
    }
}
