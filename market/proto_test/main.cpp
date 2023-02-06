#include "proto_table_getter.h"
#include "table_types.h"

#include <market/bootcamp/deep_dive_2022/ahmad1337/proto_test/proto_lib/heavy.pb.h>
#include <market/bootcamp/deep_dive_2022/ahmad1337/proto_test/proto_lib/light.pb.h>
#include <market/bootcamp/deep_dive_2022/ahmad1337/proto_test/proto_lib/place_stat.pb.h>

#include <library/cpp/getopt/last_getopt.h>

int main(int argc, const char* argv[])
{
    NYT::Initialize(argc, argv);

    NLastGetopt::TOpts opts;

    TParams params;
    ETableTypes tableType;

    opts.AddHelpOption('h');

    opts.AddLongOption('c', "cluster")
        .Help("name of the cluster (default: hahn)")
        .DefaultValue("hahn")
        .StoreResult(&params.Cluster);

    opts.AddLongOption("table-path")
        .Help("full path to the table on cluster (e.g. //tmp/user/table)")
        .Required()
        .StoreResult(&params.TablePath);

    opts.AddLongOption("table-type")
        .Help("type of the table (HEAVY | LIGHT | STATS)")
        .Required()
        .StoreResult(&tableType);

    opts.AddLongOption("token-path")
        .Help("a path to token")
        .Required()
        .StoreResult(&params.TokenPath);

    opts.AddLongOption("out-path")
        .Help("a path to store the result")
        .Required()
        .StoreResult(&params.OutputPath);

    opts.Validate();

    // Parse arguments
    NLastGetopt::TOptsParseResult parsedResult(&opts, argc, argv);

    INFO_LOG << "Fetching from cluster: " << params.Cluster << Endl;
    INFO_LOG << "Using table: " << params.TablePath << " with type " << tableType << Endl;
    INFO_LOG << "Using path " << params.TokenPath << " as token source" << Endl;
    INFO_LOG << "Using path " << params.OutputPath << " for output" << Endl;

    switch (tableType) {
        case ETableTypes::HEAVY:
            SaveTable<THeavyTable, THeavyPlace>(params, [](THeavyTable& t) -> THeavyPlace* {
                return t.AddHeavyPlaces();
            });
            break;
        case ETableTypes::LIGHT:
            SaveTable<TLightTable, TLightPlace>(params, [](TLightTable& t) -> TLightPlace* {
                return t.AddLightPlaces();
            });
            break;
        case ETableTypes::STATS:
            SaveTable<TPlaceStatTable, TPlaceStat>(params, [](TPlaceStatTable& t) -> TPlaceStat* {
                return t.AddPlaces();
            });
            break;
        default:
            Cerr << "Unsupported table" << Endl;
            return 1;
    }
}
