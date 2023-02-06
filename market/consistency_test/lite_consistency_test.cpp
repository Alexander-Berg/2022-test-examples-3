#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/folder/path.h>
#include <util/folder/filelist.h>
#include <util/generic/algorithm.h>
#include <util/generic/hash_set.h>
#include <util/stream/file.h>
#include <util/string/join.h>
#include <util/string/split.h>
#include <util/string/strip.h>
#include <iterator>
#include <filesystem>

namespace fs = std::filesystem;

Y_UNIT_TEST_SUITE(LiteConsistencySuite) {
    Y_UNIT_TEST(TestThatAllTestPysAreInYaMake) {
        const TString liteDir = ArcadiaSourceRoot() + "/market/report/lite/";

        TSet<TString> filenames;
        TFileList fileList;
        fileList.Fill(liteDir, "test_", ".py", 1, false);
        while (const char* name = fileList.Next()) {
            filenames.insert(name);
        }

        TSet<TString> filenamesInYaMake;
        const TString liteYaMakeFn = ArcadiaSourceRoot() + "/market/report/lite/ya.make.common.tests";
        TFileInput liteYaMake{liteYaMakeFn};
        TString s;
        while (liteYaMake.ReadLine(s)) {
            s = StripInPlace(s);
            if (s.StartsWith("test_") && s.EndsWith(".py")) {
                filenamesInYaMake.insert(s);
            }
        }

        TVector<TString> difference;
        SetSymmetricDifference(filenames.begin(), filenames.end(), filenamesInYaMake.begin(), filenamesInYaMake.end(), std::back_inserter(difference));

        UNIT_ASSERT_C((filenames == filenamesInYaMake), TStringBuilder() << "Difference: " << JoinSeq(" ", difference));
    }

    void demo_perms(fs::perms p) {
        Cerr << ((p & fs::perms::owner_read) != fs::perms::none ? "r" : "-")
             << ((p & fs::perms::owner_write) != fs::perms::none ? "w" : "-")
             << ((p & fs::perms::owner_exec) != fs::perms::none ? "x" : "-")
             << ((p & fs::perms::group_read) != fs::perms::none ? "r" : "-")
             << ((p & fs::perms::group_write) != fs::perms::none ? "w" : "-")
             << ((p & fs::perms::group_exec) != fs::perms::none ? "x" : "-")
             << ((p & fs::perms::others_read) != fs::perms::none ? "r" : "-")
             << ((p & fs::perms::others_write) != fs::perms::none ? "w" : "-")
             << ((p & fs::perms::others_exec) != fs::perms::none ? "x" : "-")
             << ' ';
    }

    bool is_executable(fs::perms p) {
        return ((p & fs::perms::owner_exec) != fs::perms::none) && ((p & fs::perms::group_exec) != fs::perms::none) && ((p & fs::perms::others_exec) != fs::perms::none);
    }

    Y_UNIT_TEST(TestThatAllTestFilesAreExecutable) {
        const TString liteDir = ArcadiaSourceRoot() + "/market/report/lite/";

        TSet<TString> failedFilenames;
        TFileList fileList;
        fileList.Fill(liteDir, "test_", ".py", 1, false);
        while (const char* name = fileList.Next()) {
            TString path = liteDir + name;
            if (!is_executable(fs::status(path.c_str()).permissions())) {
                demo_perms(fs::status(path.c_str()).permissions());
                Cerr << name << "\n";
                failedFilenames.insert(name);
            }
        }
        UNIT_ASSERT_C(failedFilenames.size() == 0, TStringBuilder() << "These files have no permiissions to execute:\n"
                                                                    << JoinSeq("\n", failedFilenames));
    }

    Y_UNIT_TEST(TestThatAllTestFilesHasCorrectImportRunner) {
        const TString liteDir = ArcadiaSourceRoot() + "/market/report/lite/";

        TString start = "#!/usr/bin/env python\n# -*- coding: utf-8 -*-\n\nimport runner  # noqa";

        TSet<TString> failedFilenames;
        TFileList fileList;
        fileList.Fill(liteDir, "test_", ".py", 1, false);
        while (const char* name = fileList.Next()) {
            TString path = ArcadiaSourceRoot() + "/market/report/lite/" + name;
            const auto test_text = TFileInput(path).ReadAll();
            TVector<TString> v;
            StringSplitter(test_text).Split('\n').SkipEmpty().Limit(4).Collect(&v);
            TString fileStart = v[0] + "\n" + v[1] + "\n\n" + v[2] + "\n";
            if (!fileStart.StartsWith(start)) {
                failedFilenames.insert(name);
            }
        }
        UNIT_ASSERT_C(failedFilenames.size() == 0, TStringBuilder() << "These files have wrong beginning:\n"
                                                                    << JoinSeq("\n", failedFilenames) << "\n\n\nStart your test with:\n"
                                                                    << start);
    }
}
