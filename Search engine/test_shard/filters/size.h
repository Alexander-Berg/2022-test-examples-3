#pragma once

#include "filter.h"

namespace NTestShard {

class TSizeFilter : public IQueryFilter {
public:
    bool IsPassed(const TAttrRefTree& query) override;

    inline TString Name() const override {
        return "Default";
    }

private:
    static constexpr size_t MaxSize = 400;
};

}
