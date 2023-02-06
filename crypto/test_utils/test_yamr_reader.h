#pragma once

#include "test_yamr_row.h"

namespace NCrypta::NYtTestUtils {
    template<typename TRows, typename TGetter>
    class TTestYamrReader : public NYT::IYaMRReaderImpl {
    public:
        explicit TTestYamrReader(const TRows& rows)
            : Rows(&rows)
            , Iter(Rows->cbegin()) {
            UpdateRowView();
        }

        const NYT::TYaMRRow& GetRow() const {
            Y_ENSURE(IsValid());
            return RowView;
        }

        bool IsValid() const {
            return Iter != Rows->cend();
        }

        void Next() {
            Y_ENSURE(IsValid());
            ++Iter;
            UpdateRowView();
        }

        ui32 GetTableIndex() const {
            Y_ENSURE(IsValid());
            return TGetter::GetIndex(*Iter);
        };

        ui32 GetRangeIndex() const {
            Y_ENSURE(IsValid());
            return 0;
        };

        ui64 GetRowIndex() const {
            Y_ENSURE(IsValid());
            return Iter - Rows->cbegin();
        };

        void NextKey() {
            // ???
        };

        void Reset(const TRows& rows) {
            Rows = &rows;
            Iter = Rows->cbegin();
            UpdateRowView();
        }

    private:
        void UpdateRowView() {
            RowView = IsValid() ? TGetter::GetRow(*Iter) : NYT::TYaMRRow();
        }

        const TRows* Rows;
        typename TRows::const_iterator Iter;
        NYT::TYaMRRow RowView;
    };
}