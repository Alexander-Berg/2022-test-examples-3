#pragma once

#include "attribute_queries.h"
#include "options.h"
#include "save_requests.h"

#include <search/tools/test_shard/common/attribute_tree.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>

namespace NProto {

class TQuery;
class TQueryVector;

}

namespace NTestShard {

class TRequestsBuilder {
public:
    struct TWeightedType {
        TAttrSchemeTree Attributes;
        float Weight;
    };

public:
    TRequestsBuilder(const TOptions& opts);

    void AddType(const TWeightedType& type);
    void ClearTypes();
    NProto::TQueryVector Generate();

private:
    struct TQueryType {
        TAttrSchemeTree Attributes;
        ui32 Count;
    };

    TVector<TQueryType> Types_;
    ui32 RequestsCount_;
    TAttributesQueriesBuilder Builder_;
};

class TRequestsGenerator {
public:
    TRequestsGenerator(TOptions& opts);

    NProto::TQueryVector Generate(const TOptions& opts);
    void Mutate(NProto::TQueryVector& queries);

private:
    TRequestsBuilder Builder_;
    TRequestsFetcher Fetcher_;
};

NProto::TQueryVector GenerateRequests(TOptions& opts);

int PrintRequests(TOptions& opts);

}
