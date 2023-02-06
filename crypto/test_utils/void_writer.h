#pragma once

#include <mapreduce/yt/interface/io.h>

namespace NCrypta {
    class TVoidWriter : public NYT::IYaMRWriterImpl {
    public:
        TVoidWriter(size_t streamCount);

        size_t GetTableCount() const override;

        void AddRow(const NYT::TYaMRRow &, size_t) override;

        void AddRow(NYT::TYaMRRow &&, size_t) override;

        void FinishTable(size_t) override;

    private:
        const size_t StreamCount = 0;
    };
}
