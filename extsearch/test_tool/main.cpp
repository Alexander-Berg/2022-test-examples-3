#include <mapreduce/yt/interface/client.h>

#include <library/cpp/getopt/last_getopt.h>

#include <util/datetime/base.h>


int main(int argc, const char** argv) {
    NYT::Initialize(argc, argv);

    TString cluster;
    TString inputTable;
    TString outputTable;
    ui64 rowOffset = 0;
    ui64 rowLimit = 0;
    ui32 sleepSeconds = 0;

    {
        NLastGetopt::TOpts opts;
        opts.SetFreeArgsNum(0);

        opts.AddLongOption("cluster").Required().RequiredArgument().StoreResult(&cluster);
        opts.AddLongOption("input-table").Required().RequiredArgument().StoreResult(&inputTable);
        opts.AddLongOption("output-table").Required().RequiredArgument().StoreResult(&outputTable);
        opts.AddLongOption("row-offset").Required().RequiredArgument().StoreResult(&rowOffset).DefaultValue(0);
        opts.AddLongOption("row-limit").Required().RequiredArgument().StoreResult(&rowLimit).DefaultValue(0);
        opts.AddLongOption("sleep-seconds").RequiredArgument().StoreResult(&sleepSeconds).DefaultValue(0);

        NLastGetopt::TOptsParseResult optsParseResult(&opts, argc, argv);
    }

    auto client = NYT::CreateClient(cluster);
    auto writer = client->CreateTableWriter<NYT::TNode>(NYT::TRichYPath(outputTable).Append(true));

    if (sleepSeconds > 0) {
        Sleep(TDuration::Seconds(sleepSeconds));
    }

    writer->AddRow(NYT::TNode()
        ("input_table", inputTable)
        ("row_offset", rowOffset)
        ("row_limit", rowLimit));

    writer->Finish();

    return 0;
}
