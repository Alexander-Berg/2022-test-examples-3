#include "Csv.h"

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/system/tempfile.h>
#include <util/stream/file.h>


class TCsvTest : public TTestBase {
    UNIT_TEST_SUITE(TCsvTest)
    UNIT_TEST(CsvTest);
    UNIT_TEST_SUITE_END();

    void CsvTest() {
        TTempFileHandle tmpFile;
        {
            TUnbufferedFileOutput f(tmpFile.Name());
            f.Write("1046101,Reiki - A Medical Dictionary\\, Bibliography\\, and Annotated Research Guide to Internet References,,,90881\n");
            f.Write("1046103,spe rulit,a,2,91004\n");
            f.Flush();
        }

        {
            TUnbufferedFileInput f(tmpFile.Name());
            TCsvReader reader(f);
            TVector<TString> fields;
            UNIT_ASSERT_EQUAL(5, reader.Read(fields));
            UNIT_ASSERT_EQUAL("1046101", fields[0]);
            UNIT_ASSERT_EQUAL("Reiki - A Medical Dictionary, Bibliography, and Annotated Research Guide to Internet References", fields[1]);
            UNIT_ASSERT_EQUAL(TString(), fields[2]);
            UNIT_ASSERT_EQUAL(TString(), fields[3]);
            UNIT_ASSERT_EQUAL("90881", fields[4]);

            UNIT_ASSERT_EQUAL(5, reader.Read(fields));
            UNIT_ASSERT_EQUAL("1046103", fields[0]);
            UNIT_ASSERT_EQUAL("spe rulit", fields[1]);
            UNIT_ASSERT_EQUAL("a", fields[2]);
            UNIT_ASSERT_EQUAL("2", fields[3]);
            UNIT_ASSERT_EQUAL("91004", fields[4]);

            UNIT_ASSERT_EQUAL(0, reader.Read(fields));
        }
    }
};

UNIT_TEST_SUITE_REGISTRATION(TCsvTest);
