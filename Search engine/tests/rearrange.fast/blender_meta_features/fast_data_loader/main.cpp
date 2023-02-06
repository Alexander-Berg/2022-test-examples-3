#include <search/web/rearrange/blender_meta_features/fast_data/fast_data.h>

#include <library/cpp/getopt/last_getopt.h>
#include <util/folder/path.h>

using namespace NLastGetopt;

int main(int argc, char* argv[]) {
    TOpts opts = TOpts::Default();
    TString path;
    opts.AddCharOption('p', "path").Required().StoreResult(&path);
    TOptsParseResult parseResult(&opts, argc, argv);
    NBlenderMetaFeatures::TFastData fd;
    fd.Load(path);
    return 0;
}
