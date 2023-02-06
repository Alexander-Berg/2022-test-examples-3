#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/protobuf/yql/descriptor.h>
#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/util/ypath_join.h>
#include <extsearch/video/robot/crawling/player_testing/protos/job.pb.h>


int main(int argc, const char* argv[]) {
    TString ytProxy;
    TString dstTable;
    NLastGetopt::TOpts opts;
    opts.AddLongOption('s', "--server")
        .RequiredArgument("YT proxy")
        .Optional()
        .DefaultValue("arnold")
        .StoreResult(&ytProxy);
    opts.AddLongOption('d', "--dst-table")
        .RequiredArgument("YT path")
        .Optional()
        .DefaultValue("//home/videoindex/deletes/snail/jobs.prev")
        .StoreResult(&dstTable);
    NLastGetopt::TOptsParseResult optsParseResult(&opts, argc, argv);
    auto client = NYT::CreateClient(ytProxy);
    const auto attrPath = NYT::JoinYPaths(dstTable,  "@_yql_proto_field_data");
    const auto attrValue = GenerateProtobufTypeConfig<NSnail::TJobResult>();
    client->Set(attrPath, attrValue);
    return 0;
}
