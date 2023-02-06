#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>

#include <util/string/builder.h>
#include <util/string/split.h>
#include <util/system/compiler.h>

int main(int argc, char* argv[]) {
    Y_UNUSED(argc);
    TVector<TStringBuf> processNameParts;
    Split(argv[0], "_", processNameParts);
    const int processIndex = FromString<int>(processNameParts.back());

    const NJson::TJsonValue input = NJson::ReadJsonTree(&Cin, true);
    const TString& prevState = input["state"].GetString();
    int intState = 0;
    if (prevState) {
        intState = FromString<int>(prevState);
    }
    intState += 1;

    NJson::TJsonValue abs_tags;
    abs_tags.InsertValue("lab1", "val1");
    abs_tags.InsertValue("lab2", "val2");

    NJson::TJsonValue abs_metric;
    abs_metric.InsertValue("name", TStringBuilder() << "abs_metric." << processIndex);
    abs_metric.InsertValue("value", intState);
    abs_metric.InsertValue("ts", intState * 5'000'000);
    abs_metric.InsertValue("is_absolute", true);
    abs_metric.InsertValue("is_low_resolution", false);
    abs_metric.InsertValue("aggregation", "max");
    abs_metric.InsertValue("reset", intState == 3);
    abs_metric.InsertValue("tags", abs_tags);
    

    NJson::TJsonValue diff_metric;
    diff_metric.InsertValue("name", TStringBuilder() << "diff_metric." << processIndex);
    diff_metric.InsertValue("value", intState);
    diff_metric.InsertValue("ts", intState * 5'000'000);
    diff_metric.InsertValue("is_absolute", false);
    diff_metric.InsertValue("is_low_resolution", false);
    diff_metric.InsertValue("aggregation", "min");
    diff_metric.InsertValue("reset", intState == 3);

    NJson::TJsonValue metrics;
    metrics.AppendValue(abs_metric);
    metrics.AppendValue(diff_metric);

    NJson::TJsonValue result;
    result.InsertValue("metrics", metrics);
    result.InsertValue("state", ToString(intState));
    WriteJson(&Cout, &result);
    return 0;
}
