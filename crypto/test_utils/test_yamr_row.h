#pragma once

#include <mapreduce/yt/interface/io.h>

namespace NCrypta::NYtTestUtils {
    struct TTestYaMRRow {
        TString Key;
        TString Subkey;
        TString Value;
        size_t Index;

        TTestYaMRRow(TString key, TString subkey, TString value, size_t index = 0);
        TTestYaMRRow(const NYT::TYaMRRow& row, size_t index = 0);

        bool operator==(const TTestYaMRRow& other);
        bool operator!=(const TTestYaMRRow& other);

        struct TGetter {
            static size_t GetIndex(const TTestYaMRRow& row);
            static NYT::TYaMRRow GetRow(const TTestYaMRRow& row);
        };
    };
}
