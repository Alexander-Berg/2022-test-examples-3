#pragma once

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/init.h>

namespace NTestId {

    class TMapper : public NYT::IMapper<NYT::TTableReader<NYT::TNode>, NYT::TTableWriter<NYT::TNode>> {
    public:
        TMapper() = default;
        TMapper(const TVector<TString>& testIds) : TestIds_(testIds) {}

        void Do(NYT::TTableReader<NYT::TNode>* reader, NYT::TTableWriter<NYT::TNode>* writer) override;

        Y_SAVELOAD_JOB(TestIds_);

    private:
        TVector<TString> TestIds_;
    };

} // NTestId
