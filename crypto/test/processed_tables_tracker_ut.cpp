#include <crypta/lib/native/yt/processed_tables_tracker/processed_tables_tracker.h>

#include <crypta/lib/native/yt/utils/helpers.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mapreduce/yt/interface/client.h>

#include <util/system/env.h>
#include <util/stream/file.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(ProcessedTablesTracker) {
    Y_UNIT_TEST(Basic)
    {
        TString ytProxy = GetEnv("YT_PROXY");

        auto client = NYT::CreateClient(ytProxy);

        const auto createOptions = NYT::TCreateOptions().Recursive(true);

        TProcessedTablesTracker::TConfig config;
        config.SourceDir = "//tmp/log";
        config.StateTable = "//tmp/.processed";

        TProcessedTablesTracker processedTablesTracker(config);

        static const TString TABLE1 = "//tmp/log/2019-04-05T11:00:00";
        static const TString TABLE2 = "//tmp/log/2019-04-05T11:05:00";
        static const TString TABLE3 = "//tmp/log/2019-04-05T11:10:00";

        client->Create(TABLE1, NYT::NT_TABLE, createOptions);
        client->Create(TABLE2, NYT::NT_TABLE, createOptions);

        UNIT_ASSERT_EQUAL(TVector<TString>({TABLE2}), processedTablesTracker.GetUnprocessedTables(client, 1));

        auto unprocessedTables = processedTablesTracker.GetUnprocessedTables(client);
        UNIT_ASSERT_EQUAL(TVector<TString>({TABLE2, TABLE1}), unprocessedTables);

        processedTablesTracker.AddProcessedTables(client, unprocessedTables);

        UNIT_ASSERT_EQUAL(TVector<TString>({}), processedTablesTracker.GetUnprocessedTables(client));

        client->Create(TABLE3, NYT::NT_TABLE, createOptions);
        UNIT_ASSERT_EQUAL(TVector<TString>({TABLE3}), processedTablesTracker.GetUnprocessedTables(client));
    }
};
