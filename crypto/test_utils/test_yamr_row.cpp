#include "test_yamr_row.h"

using namespace NCrypta::NYtTestUtils;

TTestYaMRRow::TTestYaMRRow(TString key, TString subkey, TString value, size_t index)
    : Key(std::move(key))
    , Subkey(std::move(subkey))
    , Value(std::move(value))
    , Index(index) {
}

TTestYaMRRow::TTestYaMRRow(const NYT::TYaMRRow& row, size_t index)
    : Key(row.Key)
    , Subkey(row.SubKey)
    , Value(row.Value)
    , Index(index) {
}

bool TTestYaMRRow::operator==(const TTestYaMRRow& other) {
    return Key == other.Key && Subkey == other.Subkey && Value == other.Value && Index == other.Index;
}

bool TTestYaMRRow::operator!=(const TTestYaMRRow& other) {
    return !(*this == other);
}

size_t TTestYaMRRow::TGetter::GetIndex(const TTestYaMRRow& row) {
    return row.Index;
}

NYT::TYaMRRow TTestYaMRRow::TGetter::GetRow(const TTestYaMRRow& row) {
    return {row.Key, row.Subkey, row.Value};
};
