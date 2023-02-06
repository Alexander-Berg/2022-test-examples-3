#include <extsearch/geo/kernel/gazetteer/builder/builder.h>
#include <extsearch/geo/kernel/gazetteer/finder/finder.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/generic/yexception.h>

namespace {
    const TString GZT_FILE_NAME = "./test.gzt.bin";

    void AssertNotFound(const NGeosearch::TGazetteerFinder& finder, const char* request);
    void AssertFound(const NGeosearch::TGazetteerFinder& finder, const char* request, size_t howMany = 0);

    void AssertSubphraseNotFound(const NGeosearch::TGazetteerFinder& finder, const char* request);
    void AssertSubphraseFound(const NGeosearch::TGazetteerFinder& finder, const char* request, size_t howMany = 0);
} // namespace

Y_UNIT_TEST_SUITE(TGeosearchGazetteerLibraryTest) {
    Y_UNIT_TEST(IgnoreWordOrder) {
        NGeosearch::TGazetteerBuilder builder;
        builder.Add("Сбербанк России, платежный терминал", 100);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "сбербанк россии платежный терминал");
            AssertFound(finder, "Сбербанк России: платежный терминал");
            AssertFound(finder, "Платежный Сбербанк. России терминал.");
            AssertFound(finder, "сбербанк; терминал; россии; платежный;");
            AssertFound(finder, "платежный терминал Сбербанка России");
            AssertFound(finder, "Сбербанк платежного терминала Россией.");

            AssertNotFound(finder, "Платежный терминал Сбербанка");
            AssertNotFound(finder, "Сбербанк России");
            AssertNotFound(finder, "платежный терминал");
            AssertNotFound(finder, "Сбербанк России: платежный терминал, банкомат");

            AssertNotFound(finder, "article1");
            AssertNotFound(finder, "ArticleData");
            AssertNotFound(finder, "settings");

            UNIT_ASSERT_EXCEPTION(finder.SearchSubphrases("сбербанк"), yexception);
        }

        // We ensure that finder object has been destructed, so we can safely delete the file.
        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

    Y_UNIT_TEST(PreserveWordOrder) {
        NGeosearch::TGazetteerBuilder builder(true, false);
        builder.Add("Больницы, аптеки", 100);
        builder.Add("Банкоматы", 200);
        builder.Add("Платежные терминалы", 300);
        builder.Add("Аптеки гомеопатические", 400);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "банкоматы");

            AssertNotFound(finder, "Аптеки, больницы");
            AssertFound(finder, "Больницы, аптеки");

            AssertSubphraseNotFound(finder, "Терминал платежный");
            AssertSubphraseFound(finder, "Платежный терминал");

            AssertSubphraseFound(finder, "Гомеопатический банкомат", 1);
            AssertSubphraseFound(finder, "Гомеопатический платежный терминал", 1);
            AssertSubphraseNotFound(finder, "Платёжный гомеопатический терминал");
            AssertSubphraseFound(finder, "Больницы, банкоматы, аптеки", 1);
            AssertSubphraseFound(finder, "Больницы, аптеки, банкоматы", 2);
            AssertSubphraseFound(finder, "банкоматы", 1);
            AssertSubphraseNotFound(finder, "гомеопатические аптеки");
            AssertSubphraseFound(finder, "Больницы, аптеки гомеопатические", 2);
            AssertSubphraseFound(finder, "Аптеки, больницы, банкоматы Беларусбанка", 1);
            AssertSubphraseFound(finder, "Сбербанк России, банкомат", 1);
            AssertSubphraseFound(finder, "Банкомат, банкомата, банкомату, банкоматом, банкомате", 1);
        }

        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

    Y_UNIT_TEST(IgnoreConjAndPrep) {
        NGeosearch::TGazetteerBuilder builder;
        builder.Add("Рога и Копыта", 100);
        builder.Add("ЧОП Вследствие и К", 200);
        builder.Add("Аптека", 300);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "рога копыта и");
            AssertFound(finder, "рога, копыта");
            AssertFound(finder, "или рога, или копыта - и то, и то!");
            AssertFound(finder, "ЧОП Вследствие и К");
            AssertFound(finder, "чоп");
            AssertNotFound(finder, "ЧОП Следствие");
            AssertFound(finder, "аптека");
            AssertFound(finder, "вблизи, вдоль, возле, около, после, аптека");
            AssertNotFound(finder, "вблизи, вдоль, возле, около, после, аптека, оптика");

            UNIT_ASSERT_EXCEPTION(NGeosearch::TGazetteerFinder(GZT_FILE_NAME, false), yexception);
        }

        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

    Y_UNIT_TEST(PreserveConjAndPrep) {
        NGeosearch::TGazetteerBuilder builder(false);
        builder.Add("Рога и Копыта", 100);
        builder.Add("ЧОП Вследствие и К", 200);
        builder.Add("Аптека", 300);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "рога копыта и");
            AssertNotFound(finder, "рога, копыта");
            AssertNotFound(finder, "или рога, или копыта - и то, и то!");
            AssertFound(finder, "ЧОП Вследствие и К");
            AssertNotFound(finder, "чоп");
            AssertNotFound(finder, "ЧОП Следствие");
            AssertFound(finder, "аптека");
            AssertNotFound(finder, "вблизи, вдоль, возле, около, после, аптека");
            AssertNotFound(finder, "вблизи, вдоль, возле, около, после, аптека, оптика");

            UNIT_ASSERT_EXCEPTION(NGeosearch::TGazetteerFinder(GZT_FILE_NAME, true), yexception);
        }

        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

    Y_UNIT_TEST(MaxWordsInPermutation) {
        NGeosearch::TGazetteerBuilder builder(true /* ignoreConjAndPrep */, false /*ignoreWordOrder*/, 2 /*MaxWordsInPermutation*/);
        builder.Add("Спортивный комплекс", 100);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "спортивный комплекс");
            AssertFound(finder, "комплекс спортивный");
            AssertFound(finder, "комплекс, спортивный");

            AssertNotFound(finder, "комплекс, спортивный, галерея");
        }

        // We ensure that finder object has been destructed, so we can safely delete the file.
        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

    Y_UNIT_TEST(MaxWordsInPermutationMax) {
        NGeosearch::TGazetteerBuilder builder(true /* ignoreConjAndPrep */, false /*ignoreWordOrder*/, 2 /*MaxWordsInPermutation*/);
        builder.Add("Спортивный комплекс Буревестник", 100);
        builder.BuildFile(GZT_FILE_NAME);

        {
            NGeosearch::TGazetteerFinder finder(GZT_FILE_NAME);

            AssertFound(finder, "Спортивный комплекс Буревестник");

            AssertNotFound(finder, "комплекс спортивный");
            AssertNotFound(finder, "буревестник комплекс спортивный");
        }

        // We ensure that finder object has been destructed, so we can safely delete the file.
        TFsPath(GZT_FILE_NAME).DeleteIfExists();
    }

}

namespace {
    void AssertFound(const NGeosearch::TGazetteerFinder& finder, const char* request, size_t howMany) {
        const TVector<NGeosearch::TIDType>& result = finder.Search(request);
        if (result.empty()) {
            UNIT_FAIL("text '" << request << "'' was not found in gazetteer");
        }
        if (howMany != 0 && howMany != result.size()) {
            UNIT_FAIL("expected to find " << howMany << " articles, found " << result.size() << " with text '" << request << "'");
        }
    }

    void AssertNotFound(const NGeosearch::TGazetteerFinder& finder, const char* request) {
        if (!finder.Search(request).empty()) {
            UNIT_FAIL("text '" << request << "' was found in gazetteer, but it shouldn't");
        }
    }

    void AssertSubphraseFound(const NGeosearch::TGazetteerFinder& finder, const char* request, size_t howMany) {
        const TVector<NGeosearch::TIDType>& result = finder.SearchSubphrases(request);
        if (result.empty()) {
            UNIT_FAIL("subphrases of text '" << request << "' were not found in gazetteer");
        }
        if (howMany != 0 && howMany != result.size()) {
            UNIT_FAIL("expected to find " << howMany << " articles, found " << result.size() << " by subphrases of '" << request << "'");
        }
    }

    void AssertSubphraseNotFound(const NGeosearch::TGazetteerFinder& finder, const char* request) {
        if (!finder.SearchSubphrases(request).empty()) {
            UNIT_FAIL("subphrases of text '" << request << "' were found in gazetteer, but they shouldn't");
        }
    }
} // namespace
