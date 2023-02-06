#include "test_query.h"
#include "init_vectors.h"

#include <kernel/click_sim/query.h>
#include <extsearch/images/robot/userdata/library/opt/opt.h>

int TestQuery(int argc, const char **argv) {
    NImages::NUserData::TOptions options;
    TString query;
    NImages::NUserData::TStartupArgsParser()
            .AddRequired("query", "Query ti test", "<string>", &query)
            .Parse(argc, argv);
    std::pair<TString, TString> normalizedQuery = NClickSim::NormalizeQuery(query);
    printf("normalized query = %s\n", normalizedQuery.first.data());
    printf("normalized sorted query = %s\n", normalizedQuery.second.data());
    const TString saasKey = NClickSim::Query2SaasKey(normalizedQuery.second);
    printf("query key = %s\n", saasKey.data());
    return 0;
}
