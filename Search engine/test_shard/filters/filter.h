#pragma once

#include <search/tools/test_shard/common/attribute_tree.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/generic/ptr.h>
#include <util/generic/hash_set.h>

namespace NTestShard {

class IQueryFilter {
public:
    virtual ~IQueryFilter() = default;

    virtual bool IsPassed(const TAttrRefTree& query) = 0;

    /* non static because it has to be virtual */
    inline virtual TString Name() const {
        return "";
    }
};

class TRequestFilter {
public:
    TRequestFilter(const TVector<TString>& requesedFilters);
    bool IsPassed(const TAttrRefTree& request) const;

private:
    template<typename T>
    void RegisterFilter();

private:
    TVector<THolder<IQueryFilter>> Filters_;
    THashSet<TString> EnabledFilters_;
};

}
