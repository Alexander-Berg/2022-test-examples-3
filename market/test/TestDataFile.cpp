#include "DataFile.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/stream/file.h>
#include <util/system/fs.h>


const char DATA1[] = "hello world";

TEST(TestDataFile, GetDataLenght)
{
    const TString filename = "datafile";

    {
        TFileOutput f(filename);
        f << DATA1;
    }

    TDataFile mm(filename);
    char* p = (char*)mm.getData();
    size_t lenght = mm.getLength();

    ASSERT_TRUE(strncmp(p, DATA1, lenght) == 0);
    NFs::Remove(filename);
}
